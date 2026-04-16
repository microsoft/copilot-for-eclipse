// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ChatProgressListener;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.ui.chat.ActionBar;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.tools.JavaDebuggerToolAdapter;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handles debug events and automatically sends chat messages when breakpoints are hit
 * or conditional breakpoint errors occur.
 */
public class DebugEventAutoResponseHandler implements IDebugEventSetListener, ChatProgressListener {
  private static final long THROTTLE_INTERVAL_MS = 2000; // Minimum 2 seconds between messages

  private final AgentToolService agentToolService;
  private volatile PendingBreakpointMessage pendingMessage;
  private volatile boolean turnInProgress;
  private volatile long lastMessageSentTime = 0;

  /**
   * Stores information about a pending breakpoint hit message.
   */
  private static class PendingBreakpointMessage {
    final String fileName;
    final int lineNumber;
    final String methodName;
    final String message;

    PendingBreakpointMessage(String fileName, int lineNumber, String methodName, String message) {
      this.fileName = fileName;
      this.lineNumber = lineNumber;
      this.methodName = methodName;
      this.message = message;
    }
  }

  /**
   * Creates a new debug event auto-response handler.
   *
   * @param agentToolService the agent tool service to check tool availability
   */
  public DebugEventAutoResponseHandler(AgentToolService agentToolService) {
    this.agentToolService = agentToolService;
    this.pendingMessage = null;
    this.turnInProgress = false;

    // Register as a chat progress listener to detect turn completion
    var chatEventsManager = CopilotCore.getPlugin().getChatEventsManager();
    if (chatEventsManager != null) {
      chatEventsManager.addChatProgressListener(this);
    }
  }

  @Override
  public void handleDebugEvents(DebugEvent[] events) {
    // Check if java_debugger tool is still available before processing events
    if (agentToolService.getTool(JavaDebuggerToolAdapter.TOOL_NAME) == null) {
      return;
    }

    for (DebugEvent event : events) {
      if (event.getKind() == DebugEvent.SUSPEND && event.getDetail() == DebugEvent.BREAKPOINT) {
        handleBreakpointHit(event);
      }
    }
  }

  /**
   * Handles a breakpoint hit event.
   *
   * @param event the debug event
   */
  private void handleBreakpointHit(DebugEvent event) {
    Object source = event.getSource();
    if (!(source instanceof IThread)) {
      return;
    }

    IThread thread = (IThread) source;
    try {
      IStackFrame topFrame = thread.getTopStackFrame();
      if (topFrame == null) {
        return;
      }

      // Get breakpoint location information
      String fileName = getFileName(topFrame);
      int lineNumber = topFrame.getLineNumber();
      String methodName = topFrame.getName();

      // Check if a breakpoint still exists at this location
      // If the agent removed it during the current turn, ignore this event
      if (!breakpointExistsAt(fileName, lineNumber)) {
        return;
      }

      // Format minimal message and store with breakpoint details for verification
      String message = String.format("Breakpoint hit at %s:%d in %s", fileName, lineNumber, methodName);
      PendingBreakpointMessage breakpointMessage = new PendingBreakpointMessage(
          fileName, lineNumber, methodName, message);

      // Send message to chat
      sendMessageToChat(breakpointMessage);

    } catch (DebugException ex) {
      // Check if this might be a conditional breakpoint error
      handlePotentialConditionalBreakpointError(thread, ex);
    }
  }

  /**
   * Checks if a breakpoint exists at the given file and line number.
   *
   * @param fileName the file name
   * @param lineNumber the line number
   * @return true if a breakpoint exists at this location
   */
  private boolean breakpointExistsAt(String fileName, int lineNumber) {
    try {
      IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints();
      for (IBreakpoint bp : breakpoints) {
        if (bp instanceof IJavaLineBreakpoint javaBreakpoint) {
          if (matchesBreakpoint(javaBreakpoint, fileName, lineNumber)) {
            // Also check that the marker still exists
            IMarker marker = javaBreakpoint.getMarker();
            return marker != null && marker.exists();
          }
        }
      }
    } catch (CoreException ex) {
      // If we can't determine, assume it exists to avoid missing legitimate breakpoints
      CopilotCore.LOGGER.error("Error checking breakpoint existence", ex);
      return true;
    }
    return false;
  }

  /**
   * Handles potential conditional breakpoint compilation errors.
   *
   * @param thread the suspended thread
   * @param exception the exception that occurred
   */
  private void handlePotentialConditionalBreakpointError(IThread thread, Exception exception) {
    try {
      IStackFrame topFrame = thread.getTopStackFrame();
      if (topFrame == null) {
        return;
      }

      String fileName = getFileName(topFrame);
      int lineNumber = topFrame.getLineNumber();

      // Find matching conditional breakpoint
      IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints();
      for (IBreakpoint bp : breakpoints) {
        if (bp instanceof IJavaLineBreakpoint) {
          IJavaLineBreakpoint javaBreakpoint = (IJavaLineBreakpoint) bp;
          if (matchesBreakpoint(javaBreakpoint, fileName, lineNumber)) {
            if (javaBreakpoint.isConditionEnabled()) {
              String condition = javaBreakpoint.getCondition();
              String message = String.format(
                  "Conditional breakpoint compilation error at %s:%d. Condition: '%s'. Please fix.",
                  fileName, lineNumber, condition);
              PendingBreakpointMessage breakpointMessage =
                  new PendingBreakpointMessage(fileName, lineNumber, null, message);
              sendMessageToChat(breakpointMessage);
              return;
            }
          }
        }
      }
    } catch (CoreException ex) {
      CopilotCore.LOGGER.error("Error handling potential conditional breakpoint error", ex);
    }
  }

  /**
   * Checks if a breakpoint matches the given file and line number.
   *
   * @param breakpoint the breakpoint to check
   * @param fileName the file name
   * @param lineNumber the line number
   * @return true if the breakpoint matches
   * @throws CoreException if an error occurs
   */
  private boolean matchesBreakpoint(IJavaLineBreakpoint breakpoint, String fileName, int lineNumber)
      throws CoreException {
    IMarker marker = breakpoint.getMarker();
    if (marker == null) {
      return false;
    }

    IResource resource = marker.getResource();
    if (resource == null) {
      return false;
    }

    String breakpointFileName = resource.getName();
    int breakpointLine = breakpoint.getLineNumber();

    return fileName.equals(breakpointFileName) && lineNumber == breakpointLine;
  }

  /**
   * Extracts the file name from a stack frame.
   *
   * @param frame the stack frame
   * @return the file name, or "unknown" if not available
   */
  private String getFileName(IStackFrame frame) {
    try {
      if (frame instanceof IJavaStackFrame javaFrame) {
        String sourceName = javaFrame.getSourceName();
        if (sourceName != null) {
          // Extract just the file name from the full path
          int lastSlash = Math.max(sourceName.lastIndexOf('/'), sourceName.lastIndexOf('\\'));
          if (lastSlash >= 0) {
            return sourceName.substring(lastSlash + 1);
          }
          return sourceName;
        }
      }
    } catch (CoreException ex) {
      // Ignore and return default
    }
    return "unknown";
  }

  /**
   * Sends a message to the chat by simulating user input.
   * This approach uses the same flow as clicking the send button,
   * ensuring all UI state management is handled correctly.
   *
   * @param breakpointMessage the breakpoint message with verification details
   */
  private void sendMessageToChat(PendingBreakpointMessage breakpointMessage) {
    // Double-check tool availability before sending
    if (agentToolService.getTool(JavaDebuggerToolAdapter.TOOL_NAME) == null) {
      return;
    }

    if (turnInProgress) {
      // If a turn is in progress, store only the latest message
      // Previous events may have already been handled by the agent during the current turn
      pendingMessage = breakpointMessage;
    } else {
      // No turn in progress - store the message and process immediately
      pendingMessage = breakpointMessage;
      processNextMessage();
    }
  }

  /**
   * Processes the pending message if no turn is in progress.
   * Simulates user input by setting the text in the action bar and clicking send.
   */
  private void processNextMessage() {
    // Only process if no turn is currently in progress
    if (turnInProgress) {
      return;
    }

    PendingBreakpointMessage breakpointMessage = pendingMessage;
    if (breakpointMessage == null) {
      return;
    }

    // Verify that the debug state still matches before sending
    // (events are queued, so state may have changed)
    if (!verifyBreakpointState(breakpointMessage)) {
      // State has changed - discard this message
      pendingMessage = null;
      return;
    }

    // Enforce throttling: check if enough time has passed since the last message
    long currentTime = System.currentTimeMillis();
    long timeSinceLastMessage = currentTime - lastMessageSentTime;
    if (timeSinceLastMessage < THROTTLE_INTERVAL_MS) {
      // Too soon - keep the pending message but don't send it yet
      return;
    }

    // Clear the pending message and update last sent time
    String message = breakpointMessage.message;
    pendingMessage = null;
    lastMessageSentTime = currentTime;

    // Simulate user input on the UI thread
    Display.getDefault().asyncExec(() -> {
      ChatView chatView = UiUtils.getView(Constants.CHAT_VIEW_ID, ChatView.class);
      if (chatView == null) {
        return;
      }

      ActionBar actionBar = chatView.getActionBar();
      if (actionBar == null) {
        return;
      }

      // Simulate user typing the message and clicking send
      actionBar.setInputTextViewerContent(message);
      actionBar.handleSendMessage();
    });
  }

  /**
   * Verifies that the current debug state matches the captured breakpoint event.
   * Since events are queued, the debug session may have moved on by the time
   * we're ready to send the message.
   *
   * @param breakpointMessage the captured breakpoint message details
   * @return true if the current state matches and message should be sent
   */
  private boolean verifyBreakpointState(PendingBreakpointMessage breakpointMessage) {
    try {
      // Check that the breakpoint still exists at the expected location
      if (!breakpointExistsAt(breakpointMessage.fileName, breakpointMessage.lineNumber)) {
        return false;
      }

      // Verify that there's a suspended thread at the expected location
      IThread[] threads = DebugPlugin.getDefault().getLaunchManager()
          .getDebugTargets()[0].getThreads();
      for (IThread thread : threads) {
        if (thread.isSuspended()) {
          IStackFrame topFrame = thread.getTopStackFrame();
          if (topFrame != null) {
            String currentFileName = getFileName(topFrame);
            int currentLineNumber = topFrame.getLineNumber();

            // Verify the current suspended location matches the captured event
            if (currentFileName.equals(breakpointMessage.fileName)
                && currentLineNumber == breakpointMessage.lineNumber) {
              return true;
            }
          }
        }
      }
    } catch (DebugException ex) {
      CopilotCore.LOGGER.error("Error verifying breakpoint state", ex);
      // If we can't verify, don't send the message
      return false;
    }

    return false;
  }

  /**
   * Called when a turn is completed.
   */
  private void onTurnCompleted() {
    turnInProgress = false;
    processNextMessage();
  }

  @Override
  public void onChatProgress(ChatProgressValue progress) {
    if (progress == null) {
      return;
    }

    if (progress.getKind() == WorkDoneProgressKind.begin) {
      // Turn has begun - mark as in progress
      turnInProgress = true;
    } else if (progress.getKind() == WorkDoneProgressKind.end) {
      // Turn has ended - process next queued message
      onTurnCompleted();
    }
  }
}

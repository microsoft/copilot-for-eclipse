// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.terminal.tm;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalServiceOutputStreamMonitorListener;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tm.terminal.view.ui.activator.UIPlugin;
import org.eclipse.tm.terminal.view.ui.interfaces.IPreferenceKeys;
import org.eclipse.tm.terminal.view.ui.interfaces.IUIConstants;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Component;

import com.microsoft.copilot.eclipse.terminal.api.IRunInTerminalTool;
import com.microsoft.copilot.eclipse.terminal.api.ShellIntegrationScripts;
import com.microsoft.copilot.eclipse.terminal.api.TerminalCommandProcessor;
import com.microsoft.copilot.eclipse.terminal.api.TerminalCommandProcessor.CompletionCheckResult;
import com.microsoft.copilot.eclipse.terminal.api.TerminalCommandProcessor.CompletionCheckState;

/**
 * terminal tool implementation for older Eclipse versions.
 */
@Component(service = IRunInTerminalTool.class, immediate = true)
public class RunInTerminalTool implements IRunInTerminalTool {
  // Shared constants and static fields
  private static final Object lock = new Object();
  private static final Map<String, StringBuilder> backgroundCommandOutputs = new HashMap<>();
  private static final String BACKGROUND_TERMINAL_PREFIX = "Copilot-";
  private static final String POWERSHELL_SCRIPT_ENV = "COPILOT_POWERSHELL_INTEGRATION_SCRIPT";
  private static final char INTERRUPT_CHARACTER = '\u0003';
  private static final String COMMAND_CANCELLED_MESSAGE = "Terminal command cancelled.";
  private static final String COMMAND_INTERRUPTED_MESSAGE = "Terminal command interrupted by a new command.";

  // Non-background terminal field
  private ITerminalViewControl persistentTerminalViewControl;

  // Terminal UI-related fields
  private ITerminalControl terminalControl;
  private CTabFolder tabFolder;
  private CTabItem copilotTabItem;
  private Image terminalIcon;
  private ImageDescriptor terminalIconDescriptor;

  // Output and command state
  private StringBuilder sb;
  private CompletableFuture<String> resultFuture;
  private volatile String activeCommand;
  private volatile boolean useMarker;
  private volatile boolean skipNextCompletionAfterInterrupt;

  /**
   * Constructor for RunInTerminalTool.
   */
  public RunInTerminalTool() {
    this.sb = new StringBuilder();
  }

  @Override
  public CompletableFuture<String> executeCommand(String command, boolean isBackground, String workingDirectory) {
    if (StringUtils.isBlank(command)) {
      return CompletableFuture.completedFuture("The command is null or empty.");
    }

    if (!isBackground) {
      // A new foreground command immediately installs a new future after Ctrl+C, so the interrupted command's
      // next prompt-completion marker must be skipped if it arrives after the new command starts listening.
      interruptCurrentCommand(COMMAND_INTERRUPTED_MESSAGE, true);
    }

    resultFuture = new CompletableFuture<>();
    CompletableFuture<String> commandFuture = resultFuture;
    useMarker = hasShellIntegrationMarker();
    activeCommand = isBackground ? null : command;

    if (!useMarker) {
      // Retain only the last line (prompt) in the output buffer
      if (!sb.isEmpty()) {
        int lastLineStart = sb.lastIndexOf(StringUtils.LF);
        if (lastLineStart > 0) {
          sb.delete(0, lastLineStart);
        }
      }
    } else {
      // For marker-based detection, clear the buffer
      sb.setLength(0);
    }

    String executionId = UUID.randomUUID().toString();
    final String finalCommand = TerminalCommandProcessor.formatForExecution(command, useBracketedPaste());

    synchronized (lock) {
      if (!isBackground && this.persistentTerminalViewControl != null) {
        revealTerminal();
        sendCommand(this.persistentTerminalViewControl, finalCommand);
        return commandFuture;
      }

      ITerminalService service = TerminalServiceFactory.getService();
      if (service == null) {
        activeCommand = null;
        return CompletableFuture.completedFuture("Failed to open terminal console due to terminal service is null.");
      }

      service.openConsole(prepareTerminalProperties(isBackground, executionId, workingDirectory), status -> {
        if (status.isOK()) {
          if (commandFuture.isDone()) {
            return;
          }
          ITerminalViewControl terminalViewControl = finalizeTerminalSetup(executionId, isBackground);
          if (terminalViewControl == null) {
            activeCommand = null;
            commandFuture.complete("Terminal view control cannot be setup for RunInTerminalTool.");
            return;
          }

          if (!isBackground) {
            this.persistentTerminalViewControl = terminalViewControl;
            revealTerminal();
          }
          sendCommand(terminalViewControl, finalCommand);
        } else {
          activeCommand = null;
          commandFuture.complete("Failed to open terminal console: " + status.getException());
        }
      });
    }

    if (isBackground) {
      return CompletableFuture.completedFuture("Command is running in terminal with ID=" + executionId);
    }

    return commandFuture;
  }

  @Override
  public Map<String, Object> prepareTerminalProperties(boolean runInBackground, String executionId,
      String workingDirectory) {
    Map<String, Object> properties = new HashMap<>();

    properties.put(ITerminalsConnectorConstants.PROP_ENCODING, "UTF-8");
    properties.put(ITerminalsConnectorConstants.PROP_TITLE_DISABLE_ANSI_TITLE, true);
    if (StringUtils.isNotBlank(workingDirectory)) {
      properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, workingDirectory);
    }

    if (Platform.getOS().equals(Platform.OS_WIN32)) {
      properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, "powershell.exe");
      String scriptPath = ShellIntegrationScripts.getPowerShellScriptPath();
      if (scriptPath != null) {
        String[] environment = new String[] { POWERSHELL_SCRIPT_ENV + "=" + scriptPath };
        String args = "-NoExit -ExecutionPolicy Bypass -Command \". $env:" + POWERSHELL_SCRIPT_ENV + "\"";
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ENVIRONMENT, environment);
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_MERGE_ENVIRONMENT, true);
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, args);
      }
    } else if (Platform.getOS().equals(Platform.OS_LINUX)) {
      properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, "/bin/bash");
      String scriptPath = ShellIntegrationScripts.getBashScriptPath();
      if (scriptPath != null) {
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, "--init-file \"" + scriptPath + "\" -i");
      }
    } else {
      // macOS or other Unix-like: keep existing behavior, only set args if empty
      String args = UIPlugin.getScopedPreferences()
          .getString(IPreferenceKeys.PREF_LOCAL_TERMINAL_DEFAULT_SHELL_UNIX_ARGS);
      if (StringUtils.isBlank(args)) {
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, "-l");
      }
    }

    properties.put(ITerminalsConnectorConstants.PROP_FORCE_NEW, true);
    properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
        "org.eclipse.tm.terminal.connector.local.launcher.local");

    if (runInBackground) {
      properties.put(ITerminalsConnectorConstants.PROP_TITLE, buildBackgroundTerminalTitle(executionId));
      properties.put(ITerminalsConnectorConstants.PROP_STDOUT_LISTENERS,
          new ITerminalServiceOutputStreamMonitorListener[] { buildOutputStreamMonitorListener(true, executionId) });
    } else {
      properties.put(ITerminalsConnectorConstants.PROP_TITLE, "Copilot");
      properties.put(ITerminalsConnectorConstants.PROP_STDOUT_LISTENERS,
          new ITerminalServiceOutputStreamMonitorListener[] { buildOutputStreamMonitorListener(false, null) });
    }

    return properties;
  }

  @Override
  public StringBuilder getBackgroundCommandOutput(String executionId) {
    StringBuilder output = backgroundCommandOutputs.get(executionId);
    return output;
  }

  @Override
  public void cancelCurrentCommand() {
    // User cancel completes the current future without starting a replacement command. Do not reserve a skip here,
    // otherwise a later command could incorrectly skip its own completion if the interrupted prompt was already idle.
    interruptCurrentCommand(COMMAND_CANCELLED_MESSAGE, false);
  }

  private void interruptCurrentCommand(String completionMessage, boolean skipInterruptedCompletion) {
    ITerminalViewControl terminalViewControl = null;
    CompletableFuture<String> commandFuture = null;
    synchronized (lock) {
      if (!hasRunningForegroundCommand()) {
        if (!skipInterruptedCompletion) {
          skipNextCompletionAfterInterrupt = false;
        }
        return;
      }
      activeCommand = null;
      skipNextCompletionAfterInterrupt = skipInterruptedCompletion;
      terminalViewControl = persistentTerminalViewControl;
      commandFuture = resultFuture;
    }

    if (terminalViewControl != null) {
      sendInterrupt(terminalViewControl);
    }
    if (commandFuture != null && !commandFuture.isDone()) {
      commandFuture.complete(completionMessage);
    }
  }

  private boolean hasRunningForegroundCommand() {
    return StringUtils.isNotBlank(activeCommand) && resultFuture != null && !resultFuture.isDone();
  }

  private void sendInterrupt(ITerminalViewControl terminalViewControl) {
    Display display = terminalViewControl.getControl().getDisplay();
    display.syncExec(() -> {
      if (!terminalViewControl.isDisposed()) {
        terminalViewControl.sendKey(INTERRUPT_CHARACTER);
      }
    });
  }

  private void sendCommand(ITerminalViewControl terminalViewControl, String command) {
    terminalViewControl.pasteString(command);
  }

  private boolean hasShellIntegrationMarker() {
    if (Platform.getOS().equals(Platform.OS_WIN32)) {
      return ShellIntegrationScripts.getPowerShellScriptPath() != null;
    }
    if (Platform.getOS().equals(Platform.OS_LINUX)) {
      return ShellIntegrationScripts.getBashScriptPath() != null;
    }
    return false;
  }

  private boolean useBracketedPaste() {
    // macOS terminal multiline handling differs from PowerShell/Bash integration, so keep its existing plain input.
    return Platform.getOS().equals(Platform.OS_WIN32) || Platform.getOS().equals(Platform.OS_LINUX);
  }

  private ITerminalViewControl finalizeTerminalSetup(String executionId, boolean isBackground) {
    String title = isBackground ? buildBackgroundTerminalTitle(executionId) : "Copilot";
    synchronized (lock) {
      terminalControl = getTerminalControl(title, isBackground);
      if (terminalControl != null && terminalControl instanceof ITerminalViewControl iterminalviewcontrol) {
        return iterminalviewcontrol;
      }
    }
    return null;
  }

  private ITerminalControl getTerminalControl(String terminalTitle, boolean isBackground) {
    AtomicReference<ITerminalControl> ref = new AtomicReference<>();

    Display.getDefault().syncExec(() -> {
      try {
        IWorkbenchPage page = getActivePage();
        if (page != null) {
          IViewPart view = page.showView(IUIConstants.ID, null, IWorkbenchPage.VIEW_VISIBLE);
          if (view != null) {
            tabFolder = view.getAdapter(CTabFolder.class);
            if (tabFolder != null) {
              for (CTabItem item : tabFolder.getItems()) {
                if (terminalTitle.equals(item.getText())) {
                  if (terminalIconDescriptor != null) {
                    if (terminalIcon == null || terminalIcon.isDisposed()) {
                      terminalIcon = terminalIconDescriptor.createImage();
                    }
                    item.setImage(terminalIcon);
                  }
                  item.addDisposeListener(
                      buildDisposeListener(terminalTitle.replace(BACKGROUND_TERMINAL_PREFIX, ""), isBackground));
                  if (!isBackground) {
                    // Foreground terminal command will reuse the tab item, so keep a reference to the tab item
                    copilotTabItem = item;
                  }
                  ref.set((ITerminalControl) item.getData());
                  break;
                }
              }
            }
          }
        }
      } catch (PartInitException e) {
        // Skip exception
      }
    });

    return ref.get();
  }

  private ITerminalServiceOutputStreamMonitorListener buildOutputStreamMonitorListener(boolean isBackground,
      String executionId) {
    StringBuilder output = isBackground ? new StringBuilder() : sb;
    if (isBackground) {
      backgroundCommandOutputs.put(executionId, output);
    }

    return (byteBuffer, bytesRead) -> {
      String content = new String(byteBuffer, 0, bytesRead, StandardCharsets.UTF_8);
      // Remove ANSI escape sequences while preserving only real line breaks from the terminal output.
      content = content.replaceAll("\u001B\\[(\\?)?[\\d;]*[a-zA-Z]", "");

      // Handle Windows terminal title sequences - using Platform instead of
      // PlatformUtils
      if (Platform.getOS().equals(Platform.OS_WIN32)) {
        // Remove terminal title sequences in Windows
        // It sometimes appears at the last line, which will also destroy the validation
        // of the last prompt line.
        content = content.replaceAll("\u001B\\][0-9];.*?(\u0007|\u001B\\\\)", "");
      }

      output.append(content);

      // Detect completion based on platform strategy
      if (!isBackground && resultFuture != null && !resultFuture.isDone()) {
        CompletionCheckResult completionResult;
        do {
          completionResult = useMarker
              ? TerminalCommandProcessor.tryCompleteWithMarker(output, activeCommand, skipNextCompletionAfterInterrupt)
              : TerminalCommandProcessor.tryCompleteWithPrompt(output, skipNextCompletionAfterInterrupt);
          handleCompletionResult(completionResult);
        } while (completionResult.state() == CompletionCheckState.SKIPPED
            && resultFuture != null && !resultFuture.isDone());
      }
    };
  }

  private void handleCompletionResult(CompletionCheckResult completionResult) {
    if (completionResult.state() == CompletionCheckState.INCOMPLETE) {
      return;
    }
    if (completionResult.state() == CompletionCheckState.SKIPPED) {
      skipNextCompletionAfterInterrupt = false;
      return;
    }
    activeCommand = null;
    if (resultFuture != null && !resultFuture.isDone()) {
      resultFuture.complete(completionResult.output());
    }
  }

  private DisposeListener buildDisposeListener(String executionId, boolean isBackground) {
    return e -> {
      if (isBackground) {
        backgroundCommandOutputs.remove(executionId);
      } else {
        persistentTerminalViewControl = null;
      }

      if (backgroundCommandOutputs.isEmpty() && persistentTerminalViewControl == null) {
        terminalControl = null;
        if (terminalIcon != null && !terminalIcon.isDisposed()) {
          terminalIcon.dispose();
          terminalIcon = null;
        }
      }
    };
  }

  private void revealTerminal() {
    if (tabFolder != null && copilotTabItem != null) {
      Display.getDefault().syncExec(() -> {
        try {
          IWorkbenchPage page = getActivePage();
          if (page != null) {
            IViewPart view = page.showView(IUIConstants.ID, null, IWorkbenchPage.VIEW_VISIBLE);
            if (tabFolder.isDisposed() && view != null) {
              tabFolder = view.getAdapter(CTabFolder.class);
            }
          }
          if (tabFolder != null && !tabFolder.isDisposed()) {
            tabFolder.setSelection(copilotTabItem);
          }
        } catch (PartInitException e) {
          // Skip exception
        }
      });
    }
  }

  private String buildBackgroundTerminalTitle(String executionId) {
    return BACKGROUND_TERMINAL_PREFIX + executionId;
  }

  /**
   * Get active workbench page without UiUtils dependency.
   */
  private IWorkbenchPage getActivePage() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    if (window != null) {
      return window.getActivePage();
    }
    return null;
  }

  @Override
  public void setTerminalIconDescriptor(ImageDescriptor iconDescriptor) {
    this.terminalIconDescriptor = iconDescriptor;
  }
}

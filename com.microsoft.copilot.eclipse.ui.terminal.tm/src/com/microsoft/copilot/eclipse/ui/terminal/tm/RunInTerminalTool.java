package com.microsoft.copilot.eclipse.ui.terminal.tm;

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

/**
 * terminal tool implementation for older Eclipse versions.
 */
@Component(service = IRunInTerminalTool.class, immediate = true)
public class RunInTerminalTool implements IRunInTerminalTool {
  // Shared constants and static fields
  private static final Object lock = new Object();
  private static final Map<String, StringBuilder> backgroundCommandOutputs = new HashMap<>();
  private static final String BACKGROUND_TERMINAL_PREFIX = "Copilot-";

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

  /**
   * Constructor for RunInTerminalTool.
   */
  public RunInTerminalTool() {
    this.sb = new StringBuilder();
  }

  @Override
  public CompletableFuture<String> executeCommand(String command, boolean isBackground) {
    if (StringUtils.isBlank(command)) {
      return CompletableFuture.completedFuture("The command is null or empty.");
    }

    resultFuture = new CompletableFuture<>();

    // Retain only the last line (prompt) in the output buffer
    if (!sb.isEmpty()) {
      int lastLineStart = sb.lastIndexOf(StringUtils.LF);
      if (lastLineStart > 0) {
        sb.delete(0, lastLineStart);
      }
    }

    String executionId = UUID.randomUUID().toString();
    final String finalCommand = command + System.lineSeparator();

    synchronized (lock) {
      if (!isBackground && this.persistentTerminalViewControl != null) {
        bringTerminalViewAndCopilotConsoleToFront();
        this.persistentTerminalViewControl.pasteString(finalCommand);
        return this.resultFuture;
      }

      ITerminalService service = TerminalServiceFactory.getService();
      if (service == null) {
        return CompletableFuture.completedFuture("Failed to open terminal console due to terminal service is null.");
      }

      service.openConsole(prepareTerminalProperties(isBackground, executionId), status -> {
        if (status.isOK()) {
          ITerminalViewControl terminalViewControl = finalizeTerminalSetup(executionId, isBackground);
          if (terminalViewControl == null) {
            resultFuture.complete("Terminal view control cannot be setup for RunInTerminalTool.");
            return;
          }

          if (!isBackground) {
            this.persistentTerminalViewControl = terminalViewControl;
            bringTerminalViewAndCopilotConsoleToFront();
          }
          terminalViewControl.pasteString(finalCommand);
        } else {
          resultFuture.complete("Failed to open terminal console: " + status.getException());
        }
      });
    }

    if (isBackground) {
      return CompletableFuture.completedFuture("Command is running in terminal with ID=" + executionId);
    }

    return resultFuture;
  }

  @Override
  public Map<String, Object> prepareTerminalProperties(boolean runInBackground, String executionId) {
    Map<String, Object> properties = new HashMap<>();

    properties.put(ITerminalsConnectorConstants.PROP_ENCODING, "UTF-8");
    properties.put(ITerminalsConnectorConstants.PROP_TITLE_DISABLE_ANSI_TITLE, true);

    // Only set target terminal for Windows - using Platform directly instead of PlatformUtils
    if (Platform.getOS().equals(Platform.OS_WIN32)) {
      properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, "cmd.exe");
    } else {
      // Only set the process args if not already set by user preferences
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
          IViewPart view = page.showView(IUIConstants.ID);
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
      String content = new String(byteBuffer, 0, bytesRead);
      // Remove ANSI escape sequences
      // Sometimes it also removes the linebreaks. But we need the last prompt line to be a separate line later. So we
      // add line separator back to the content.
      content = content.replaceAll("\u001B\\[(\\?)?[\\d;]*[a-zA-Z]", StringUtils.LF);

      // Handle Windows terminal title sequences - using Platform instead of PlatformUtils
      if (Platform.getOS().equals(Platform.OS_WIN32)) {
        // Remove terminal title sequences in Windows
        // It sometimes appears at the last line, which will also destroy the validation of the last prompt line.
        content = content.replaceAll("\u001B\\][0-9];.*?(\u0007|\u001B\\\\)", "");
      }

      output.append(content);
      String terminalOutput = output.toString().trim();
      int lastNewLineIndex = terminalOutput.lastIndexOf(StringUtils.LF);
      if (lastNewLineIndex > 0) {
        String lastLine = terminalOutput.substring(lastNewLineIndex).trim();

        // Check if last line is a prompt line
        // Mac always has single '%' as last line, that's not what we want.
        if (StringUtils.isNotBlank(lastLine) && lastLine.length() != 1) {
          char lastChar = lastLine.charAt(lastLine.length() - 1);
          boolean isPromptChar = lastChar == '>' || lastChar == '#' || lastChar == '$' || lastChar == '%';

          if (isPromptChar) {
            // Extract result text between prompts
            String contentWithoutLastPrompt = terminalOutput.substring(0, lastNewLineIndex);
            int promptStartIndex = contentWithoutLastPrompt.indexOf(lastLine);
            // If the prompt line is not found, set start index to 0. Sometimes it starts with the commandResult.
            if (promptStartIndex == -1) {
              promptStartIndex = 0;
            } else {
              promptStartIndex += lastLine.length();
            }

            if (!contentWithoutLastPrompt.isBlank()) {
              String commandResult = contentWithoutLastPrompt.substring(promptStartIndex).trim();
              if (resultFuture != null && !resultFuture.isDone()) {
                resultFuture.complete(commandResult);
              }
            }
          }
        }
      }
    };
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

  private void bringTerminalViewAndCopilotConsoleToFront() {
    if (tabFolder != null && copilotTabItem != null) {
      Display.getDefault().syncExec(() -> {
        try {
          IWorkbenchPage page = getActivePage();
          if (page != null) {
            IViewPart view = page.showView(IUIConstants.ID);
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

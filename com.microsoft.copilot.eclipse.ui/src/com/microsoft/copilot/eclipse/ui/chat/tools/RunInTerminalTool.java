package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalServiceOutputStreamMonitorListener;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Tool for running a command in eclipse internal terminal.
 */
public class RunInTerminalTool extends BaseTool {
  private ITerminalControl terminalControl;
  private ITerminalViewControl terminalViewControl;
  private CTabFolder tabFolder;
  private CTabItem copilotTabItem;
  private Image terminalIcon;
  private StringBuilder sb;
  private CompletableFuture<LanguageModelToolResult[]> resultFuture;
  private int signalCount = 0;
  private static final Object lock = new Object();

  private static final String TOOL_NAME = "run_in_terminal";
  private static final String EXECUTION_END_SIGNAL = "Execution finished. Return to Copilot chat to continue.";

  /**
   * Constructor for RunInTerminalTool.
   */
  public RunInTerminalTool() {
    this.name = TOOL_NAME;
    this.sb = new StringBuilder();
  }

  @Override
  public boolean needConfirmation() {
    return true;
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
    resultFuture = new CompletableFuture<>();
    String command = (String) input.get("command");
    if (StringUtils.isBlank(command)) {
      LanguageModelToolResult toolResult = new LanguageModelToolResult();
      toolResult.addContent("The tool cannot be invoked due to the command is null or empty.");
      resultFuture.complete(new LanguageModelToolResult[] { toolResult });
    } else {
      executeCommand(command, false);
    }
    return resultFuture;
  }

  @Override
  public String getConfirmedMessage() {
    return "The tool is about to run the following command in the terminal.\nDo you want to continue?";
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    // Create a new instance of LanguageModelToolInformation
    LanguageModelToolInformation toolInfo = new LanguageModelToolInformation();

    // Set the name and description of the tool
    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription("""
        Run a shell command in a terminal. State is persistent across tool calls.\n- Use this tool instead of
        printing a shell codeblock and asking the user to run it.
        - If the command is a long-running background process, you MUST pass isBackground=true.
        Background terminals will return a terminal ID which you can use to check the output
        of a background process with copilot_getTerminalOutput.
        - If a command may use a pager, you must something to disable it.
        For example, you can use `git --no-pager`.
        Otherwise you should add something like ` | cat`. Examples: git, less, man, etc.
        """);

    // Define the input schema for the tool
    InputSchema inputSchema = new InputSchema();
    inputSchema.setType("object");

    // Define the properties of the input schema
    Map<String, InputSchemaPropertyValue> properties = new HashMap<>();
    properties.put("command", new InputSchemaPropertyValue("string", "The command to run in the terminal"));
    properties.put("explaination", new InputSchemaPropertyValue("string", """
        A one-sentence description of what the command does.
        This will be shown to the user before the command is run."""));
    properties.put("isBackground", new InputSchemaPropertyValue("boolean", """
        Whether the command starts a background process.
        If true, the command will run in the background and you will not see the output.
        If false, the tool call will block on the command finishing, and then you will get the output.
        Examples of backgrond processes: building in watch mode, starting a server.
        You can check the output of a backgrond process later on by using copilot_getTerminalOutput.
        """));

    // Set the properties and required fields for the input schema
    inputSchema.setProperties(properties);
    inputSchema.setRequired(Arrays.asList("command", "explaination", "isBackground"));

    // Attach the input schema to the tool information
    toolInfo.setInputSchema(inputSchema);

    return toolInfo;
  }

  private void executeCommand(String command, boolean isBackground) {
    if (StringUtils.isBlank(command)) {
      return;
    }

    // TODO: Add background process support
    resultFuture = new CompletableFuture<>();
    signalCount = 0;
    String endSignalCommand = PlatformUtils.isWindows() ? "& echo " + EXECUTION_END_SIGNAL
        : "; echo " + EXECUTION_END_SIGNAL;
    String finalCommands = command + endSignalCommand + System.lineSeparator();
    synchronized (lock) {
      if (this.terminalViewControl == null) {
        final Map<String, Object> properties = prepareTerminalProperties();

        ITerminalService service = TerminalServiceFactory.getService();
        if (service != null) {
          service.openConsole(properties, status -> {
            if (status.isOK()) {
              // Continue initialization only after console is opened
              finalizeTerminalSetup();
              if (terminalViewControl == null) {
                CopilotCore.LOGGER.error("Failed to open terminal console",
                    new IllegalStateException("Terminal view control cannot be setup for RunInTerminalTool."));
                return;
              } else {
                bringCopilotTerminalToFront();
                terminalViewControl.pasteString(finalCommands);
              }
            } else {
              CopilotCore.LOGGER.error("Failed to open terminal console", status.getException());
            }
          });
        }
      } else {
        bringCopilotTerminalToFront();
        this.terminalViewControl.pasteString(finalCommands);
      }
    }
  }

  private Map<String, Object> prepareTerminalProperties() {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ITerminalsConnectorConstants.PROP_TITLE, Messages.agent_tool_terminal_copilotTerminalTitle);
    properties.put(ITerminalsConnectorConstants.PROP_ENCODING, "UTF-8");
    properties.put(ITerminalsConnectorConstants.PROP_TITLE_DISABLE_ANSI_TITLE, true);

    // Only set target terminal for Windows
    if (PlatformUtils.isWindows()) {
      properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, "cmd.exe");
    }

    properties.put(ITerminalsConnectorConstants.PROP_FORCE_NEW, false);
    properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
        "org.eclipse.tm.terminal.connector.local.launcher.local");
    properties.put(ITerminalsConnectorConstants.PROP_STDOUT_LISTENERS,
        new ITerminalServiceOutputStreamMonitorListener[] { buildOutputStreamMonitorListener() });
    return properties;
  }

  private void finalizeTerminalSetup() {
    synchronized (lock) {
      terminalControl = getTerminalControl();
      if (terminalControl != null && terminalControl instanceof ITerminalViewControl iterminalviewcontrol) {
        this.terminalViewControl = iterminalviewcontrol;
      }
    }
  }

  private ITerminalControl getTerminalControl() {
    AtomicReference<ITerminalControl> ref = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      try {
        IWorkbenchPage page = UiUtils.getActivePage();
        if (page != null) {
          IViewPart view = page.showView("org.eclipse.tm.terminal.view.ui.TerminalsView");
          if (view != null) {
            tabFolder = view.getAdapter(CTabFolder.class);
            if (tabFolder != null) {
              for (CTabItem item : tabFolder.getItems()) {
                if (Messages.agent_tool_terminal_copilotTerminalTitle.equals(item.getText())) {
                  copilotTabItem = item;
                  terminalIcon = UiUtils.buildImageFromPngPath("/icons/github_copilot_signed_in.png");
                  copilotTabItem.setImage(terminalIcon);
                  copilotTabItem.addDisposeListener(buildDisposeListener());
                  ref.set((ITerminalControl) copilotTabItem.getData());
                  break;
                }
              }
            }
          }
        }
      } catch (PartInitException e) {
        CopilotCore.LOGGER.error("Failed to show terminal view", e);
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Error accessing terminal view", e);
      }
    });

    return ref.get();
  }

  private ITerminalServiceOutputStreamMonitorListener buildOutputStreamMonitorListener() {
    // signalCount will be used here to count the number of appearances of the end signal, because it will appear more
    // than once. The first appearance is the command itself, the second is the end signal of the execution.
    return (byteBuffer, bytesRead) -> {
      String content = new String(byteBuffer, 0, bytesRead);
      // Remove ANSI escape sequences
      content = content.replaceAll("\u001B\\[(\\?)?[\\d;]*[a-zA-Z]", "");

      sb.append(content);
      int indexOfEndSignal = sb.lastIndexOf(EXECUTION_END_SIGNAL);

      while (indexOfEndSignal >= 0) {
        // TODO: Test the platform specific check for the end signal
        if (PlatformUtils.isWindows() && signalCount == 0 || !PlatformUtils.isWindows() && signalCount <= 1) {
          sb.delete(0, indexOfEndSignal + EXECUTION_END_SIGNAL.length());
        } else {
          String result = sb.substring(0, indexOfEndSignal);
          sb.setLength(0);
          resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(result.trim()) });
        }
        indexOfEndSignal = sb.lastIndexOf(EXECUTION_END_SIGNAL);
        signalCount++;
      }
    };
  }

  private DisposeListener buildDisposeListener() {
    return e -> {
      terminalControl = null;
      terminalViewControl = null;
      if (terminalIcon != null && !terminalIcon.isDisposed()) {
        terminalIcon.dispose();
        terminalIcon = null;
      }
    };
  }

  private void bringCopilotTerminalToFront() {
    if (tabFolder != null && copilotTabItem != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        tabFolder.setSelection(copilotTabItem);
      }, tabFolder);
    }
  }
}

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  // Shared constants and static fields
  private static final Object lock = new Object();
  private static final String TOOL_NAME = "run_in_terminal";

  // Background terminal constants and static fields
  private static final Map<String, StringBuilder> backgroundCommandOutputs = new HashMap<>();
  private static final String BACKGROUND_TERMINAL_PREFIX = Messages.agent_tool_terminal_copilotTerminalTitle + "-";

  // Non-background terminal field
  private ITerminalViewControl persistentTerminalViewControl;

  // Terminal UI-related fields
  private ITerminalControl terminalControl;
  private CTabFolder tabFolder;
  private CTabItem copilotTabItem;
  private Image terminalIcon;

  // Output and command state
  private StringBuilder sb;

  // Shared future for the tool invoke async result
  private CompletableFuture<LanguageModelToolResult[]> resultFuture;

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
      boolean isBackground = false;
      Object isBackgroundObj = input.get("isBackground");

      if (isBackgroundObj instanceof Boolean) {
        isBackground = (Boolean) isBackgroundObj;
      } else if (isBackgroundObj instanceof String) {
        isBackground = Boolean.parseBoolean((String) isBackgroundObj);
      }
      executeCommand(command, isBackground);
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
        Run a shell command in a terminal. State is persistent across tool calls.
        - Use this tool instead of printing a shell codeblock and asking the user to run it.
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
    properties.put("explanation", new InputSchemaPropertyValue("string", """
        A one-sentence description of what the command does.
        This will be shown to the user before the command is run."""));
    properties.put("isBackground", new InputSchemaPropertyValue("boolean", """
        Whether the command starts a background process.
        If true, the command will run in the background and you will not see the output.
        If false, the tool call will block on the command finishing, and then you will get the output.
        Examples of background processes: building in watch mode, starting a server.
        You can check the output of a background process later on by using copilot_getTerminalOutput.
        """));

    // Set the properties and required fields for the input schema
    inputSchema.setProperties(properties);
    inputSchema.setRequired(List.of("command", "explanation", "isBackground"));

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
        bringCopilotTerminalToFront();
        this.persistentTerminalViewControl.pasteString(finalCommand);
        return;
      }

      ITerminalService service = TerminalServiceFactory.getService();
      if (service == null) {
        resultFuture.complete(new LanguageModelToolResult[] {
            new LanguageModelToolResult("Failed to open terminal console due to terminal service is null.") });
        return;
      }

      service.openConsole(prepareTerminalProperties(isBackground, executionId), status -> {
        if (status.isOK()) {
          ITerminalViewControl terminalViewControl = finalizeTerminalSetup(executionId, isBackground);
          if (terminalViewControl == null) {
            CopilotCore.LOGGER.error("Failed to open terminal console",
                new IllegalStateException("Terminal view control cannot be setup for RunInTerminalTool."));
            resultFuture.complete(new LanguageModelToolResult[] {
                new LanguageModelToolResult("Terminal view control cannot be setup for RunInTerminalTool.") });
            return;
          }

          if (!isBackground) {
            this.persistentTerminalViewControl = terminalViewControl;
            bringCopilotTerminalToFront();
          }
          terminalViewControl.pasteString(finalCommand);
        } else {
          CopilotCore.LOGGER.error("Failed to open terminal console", status.getException());
          resultFuture.complete(new LanguageModelToolResult[] {
              new LanguageModelToolResult("Failed to open terminal console" + status.getException()) });
        }
      });
    }

    if (isBackground) {
      resultFuture.complete(new LanguageModelToolResult[] {
          new LanguageModelToolResult("Command is running in terminal with ID=" + executionId) });
    }
  }

  private Map<String, Object> prepareTerminalProperties(boolean runInBackground, String executionId) {
    Map<String, Object> properties = new HashMap<>();

    properties.put(ITerminalsConnectorConstants.PROP_ENCODING, "UTF-8");
    properties.put(ITerminalsConnectorConstants.PROP_TITLE_DISABLE_ANSI_TITLE, true);

    // Only set target terminal for Windows
    if (PlatformUtils.isWindows()) {
      properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, "cmd.exe");
    }

    properties.put(ITerminalsConnectorConstants.PROP_FORCE_NEW, true);
    properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
        "org.eclipse.tm.terminal.connector.local.launcher.local");

    if (runInBackground) {
      properties.put(ITerminalsConnectorConstants.PROP_TITLE, buildBackgroundTerminalTitle(executionId));
      properties.put(ITerminalsConnectorConstants.PROP_STDOUT_LISTENERS,
          new ITerminalServiceOutputStreamMonitorListener[] { buildOutputStreamMonitorListener(true, executionId) });
    } else {
      properties.put(ITerminalsConnectorConstants.PROP_TITLE, Messages.agent_tool_terminal_copilotTerminalTitle);
      properties.put(ITerminalsConnectorConstants.PROP_STDOUT_LISTENERS,
          new ITerminalServiceOutputStreamMonitorListener[] { buildOutputStreamMonitorListener(false, null) });
    }

    return properties;
  }

  private ITerminalViewControl finalizeTerminalSetup(String executionId, boolean isBackground) {
    String title = isBackground ? buildBackgroundTerminalTitle(executionId)
        : Messages.agent_tool_terminal_copilotTerminalTitle;
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
    SwtUtils.invokeOnDisplayThread(() -> {
      try {
        IWorkbenchPage page = UiUtils.getActivePage();
        if (page != null) {
          IViewPart view = page.showView("org.eclipse.tm.terminal.view.ui.TerminalsView");
          if (view != null) {
            tabFolder = view.getAdapter(CTabFolder.class);
            if (tabFolder != null) {
              for (CTabItem item : tabFolder.getItems()) {
                if (terminalTitle.equals(item.getText())) {
                  terminalIcon = UiUtils.buildImageFromPngPath("/icons/github_copilot_signed_in.png");
                  item.setImage(terminalIcon);
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
        CopilotCore.LOGGER.error("Failed to show terminal view", e);
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Error accessing terminal view", e);
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
      content = content.replaceAll("\u001B\\[(\\?)?[\\d;]*[a-zA-Z]", "");
      output.append(content);
      String terminalOutput = output.toString();
      int lastNewLineIndex = terminalOutput.lastIndexOf(StringUtils.LF);
      if (lastNewLineIndex > 0) {
        String lastLine = terminalOutput.substring(lastNewLineIndex).trim();

        // Check if last line is a prompt line
        if (StringUtils.isNotBlank(lastLine)) {
          char lastChar = lastLine.charAt(lastLine.length() - 1);
          boolean isPromptChar = lastChar == '>' || lastChar == '#' || lastChar == '$' || lastChar == '%';

          if (isPromptChar) {
            // Extract result text between prompts
            String contentWithoutLastPrompt = terminalOutput.substring(0, lastNewLineIndex);
            int promptStartIndex = contentWithoutLastPrompt.indexOf(lastLine);

            if (promptStartIndex >= 0) {
              String commandResult = contentWithoutLastPrompt.substring(promptStartIndex + lastLine.length()).trim();
              resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(commandResult) });
            }
          }
        }
      }
    };
  }

  private String buildBackgroundTerminalTitle(String executionId) {
    return BACKGROUND_TERMINAL_PREFIX + executionId;
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

  private void bringCopilotTerminalToFront() {
    if (tabFolder != null && copilotTabItem != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        tabFolder.setSelection(copilotTabItem);
      }, tabFolder);
    }
  }

  /**
   * Tool for getting the output of a terminal command previously started with run_in_terminal.
   */
  public static class GetTerminalOutputTool extends BaseTool {
    private static final String TOOL_NAME = "get_terminal_output";

    /**
     * Constructor for GetTerminalOutputTool.
     */
    public GetTerminalOutputTool() {
      this.name = TOOL_NAME;
    }

    @Override
    public LanguageModelToolInformation getToolInformation() {
      // Create a new instance of LanguageModelToolInformation
      LanguageModelToolInformation toolInfo = new LanguageModelToolInformation();

      // Set the name and description of the tool
      toolInfo.setName(TOOL_NAME);
      toolInfo.setDescription("Get the output of a terminal command previous started with run_in_terminal.");

      // Define the input schema for the tool
      InputSchema inputSchema = new InputSchema();
      inputSchema.setType("object");

      // Define the properties of the input schema
      Map<String, InputSchemaPropertyValue> properties = new HashMap<>();
      properties.put("id", new InputSchemaPropertyValue("string", "The ID of the terminal command output to check."));

      // Set the properties and required fields for the input schema
      inputSchema.setProperties(properties);
      inputSchema.setRequired(List.of("id"));

      // Attach the input schema to the tool information
      toolInfo.setInputSchema(inputSchema);

      return toolInfo;
    }

    @Override
    public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
      String id = (String) input.get("id");
      LanguageModelToolResult toolResult = new LanguageModelToolResult();
      CompletableFuture<LanguageModelToolResult[]> resultFuture = new CompletableFuture<>();
      if (StringUtils.isBlank(id)) {
        toolResult.addContent("The tool cannot be invoked due to the ID is null or empty.");
      } else {
        StringBuilder output = backgroundCommandOutputs.get(id);
        if (output == null) {
          toolResult.addContent("Invalid terminal ID " + id);
        } else {
          toolResult.addContent(output.toString());
        }
      }
      resultFuture.complete(new LanguageModelToolResult[] { toolResult });
      return resultFuture;
    }

  }
}

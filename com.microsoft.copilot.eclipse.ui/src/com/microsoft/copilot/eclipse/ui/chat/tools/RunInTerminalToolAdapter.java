package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ConfirmationMessages;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.terminal.api.IRunInTerminalTool;
import com.microsoft.copilot.eclipse.terminal.api.TerminalServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Adapter that bridges the UI tool interface with SPI-based terminal implementations.
 */
public class RunInTerminalToolAdapter extends BaseTool {
  private static final String TOOL_NAME = "run_in_terminal";

  /**
   * Constructor for the RunInTerminalToolAdapter.
   */
  public RunInTerminalToolAdapter() {
    this.name = TOOL_NAME;
  }

  private String buildToolDescription() {
    if (PlatformUtils.isWindows()) {
      return """
          Shell: powershell.exe

          This tool allows you to execute PowerShell commands in a persistent terminal session, \
          preserving environment variables, working directory, and other context across multiple commands.
          Use this tool instead of printing a shell codeblock and asking the user to run it.

          Command Execution:
          - Use semicolons ; to chain commands on one line.
          - Never create a sub-shell (e.g., powershell -c "command") unless explicitly asked
          - Prefer pipelines | for data flow
          - Must use absolute paths to avoid navigation issues
          - If a command may use a pager, disable it with command flags (e.g., `git --no-pager`)

          Background Processes:
          - For long-running tasks (e.g., servers), set isBackground=true
          - Returns a terminal ID for checking output later with get_terminal_output
          """;
    }
    return """
        This tool allows you to execute shell commands in a persistent terminal session, \
        preserving environment variables, working directory, and other context across multiple commands.
        Use this tool instead of printing a shell codeblock and asking the user to run it.

        Command Execution:
        - Use && to chain commands on one line
        - Never create a sub-shell (e.g., bash -c "command") unless explicitly asked
        - Prefer pipelines | for data flow
        - Must use absolute paths to avoid navigation issues
        - If a command may use a pager, disable it (e.g., `git --no-pager` or add `| cat`)

        Background Processes:
        - For long-running tasks (e.g., servers), set isBackground=true
        - Returns a terminal ID for checking output later with get_terminal_output
        """;
  }

  @Override
  public boolean needConfirmation() {
    return true;
  }

  @Override
  public ConfirmationMessages getConfirmationMessages() {
    return new ConfirmationMessages("Run command in terminal",
        "The tool is about to run the following command in the terminal.");
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    // Create a new instance of LanguageModelToolInformation
    LanguageModelToolInformation toolInfo = new LanguageModelToolInformation();

    // Set the name and description of the tool
    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription(buildToolDescription());

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

    if (needConfirmation()) {
      toolInfo.setConfirmationMessages(getConfirmationMessages());
    }

    return toolInfo;
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
    // Get terminal service from TerminalServiceManager instead of AgentToolService
    TerminalServiceManager terminalManager = TerminalServiceManager.getInstance();
    IRunInTerminalTool impl = terminalManager != null ? terminalManager.getCurrentService() : null;
    if (impl == null) {
      LanguageModelToolResult errorResult = new LanguageModelToolResult();
      errorResult
          .addContent("No terminal implementation available. Terminal service not yet loaded or failed to load.");
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { errorResult });
    }

    String command = (String) input.get("command");
    if (StringUtils.isBlank(command)) {
      LanguageModelToolResult errorResult = new LanguageModelToolResult();
      errorResult.addContent("The tool cannot be invoked due to the command is null or empty.");
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { errorResult });
    }

    boolean isBackground = false;
    Object isBackgroundObj = input.get("isBackground");
    if (isBackgroundObj instanceof Boolean) {
      isBackground = (Boolean) isBackgroundObj;
    } else if (isBackgroundObj instanceof String) {
      isBackground = Boolean.parseBoolean((String) isBackgroundObj);
    }

    impl.setTerminalIconDescriptor(UiUtils.buildImageDescriptorFromPngPath("/icons/github_copilot.png"));

    return impl.executeCommand(command, isBackground)
        .thenApply(result -> new LanguageModelToolResult[] { new LanguageModelToolResult(result) })
        .exceptionally(throwable -> new LanguageModelToolResult[] {
            new LanguageModelToolResult("Terminal execution failed: " + throwable.getMessage()) });
  }

  /**
   * Tool to retrieve the output of a terminal command that was previously started with run_in_terminal.
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
      LanguageModelToolInformation toolInfo = super.getToolInformation();

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

      // Get terminal service from TerminalServiceManager
      TerminalServiceManager terminalManager = TerminalServiceManager.getInstance();
      IRunInTerminalTool impl = terminalManager != null ? terminalManager.getCurrentService() : null;
      if (impl == null) {
        toolResult
            .addContent("No terminal implementation available. Terminal service not yet loaded or failed to load.");
      } else if (StringUtils.isBlank(id)) {
        toolResult.addContent("The tool cannot be invoked due to the ID is null or empty.");
      } else {
        StringBuilder output = impl.getBackgroundCommandOutput(id);
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

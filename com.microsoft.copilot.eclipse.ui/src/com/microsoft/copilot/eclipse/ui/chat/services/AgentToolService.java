package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ChatEventsManager;
import com.microsoft.copilot.eclipse.core.chat.ToolInvocationListener;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.RegisterToolsParams;
import com.microsoft.copilot.eclipse.ui.chat.BaseTurnWidget;
import com.microsoft.copilot.eclipse.ui.chat.ChatContentViewer;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.tools.BaseTool;
import com.microsoft.copilot.eclipse.ui.chat.tools.EditFileTool;
import com.microsoft.copilot.eclipse.ui.chat.tools.GetErrorsTool;
import com.microsoft.copilot.eclipse.ui.chat.tools.RunInTerminalTool;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Service to manage and access tools.
 */
public class AgentToolService implements ToolInvocationListener {
  private ConcurrentHashMap<String, BaseTool> tools;
  private ChatView boundChatView;

  protected CopilotLanguageServerConnection lsConnection;

  /**
   * Constructor for AgentToolService.
   */
  public AgentToolService(CopilotLanguageServerConnection lsConnection) {
    this.tools = new ConcurrentHashMap<>();
    this.lsConnection = lsConnection;
    registerDefaultTools();
  }

  /**
   * Register default tools.
   */
  private void registerDefaultTools() {
    registerTool(new RunInTerminalTool());
    registerTool(new EditFileTool());
    registerTool(new GetErrorsTool());
    // TODO: Register additional default tools here

    // Register the tools to the language server
    registerToolWithServer();

    ChatEventsManager chatEventsManager = CopilotCore.getPlugin().getChatEventsManager();
    if (chatEventsManager != null) {
      chatEventsManager.addAgentToolListener(this);
    }
  }

  /**
   * Register tools to the language server.
   *
   * @param registerToolsParams The parameters for tool registration
   * @return true if registration was successful, false otherwise
   */
  private void registerToolWithServer() {
    RegisterToolsParams registerToolsParams = new RegisterToolsParams();
    for (BaseTool tool : getAllTools()) {
      registerToolsParams.addTool(tool.getToolInformation());
    }

    lsConnection.registerTools(registerToolsParams).thenAccept(registrationResult -> {
      if (!Objects.equals("OK", registrationResult)) {
        CopilotCore.LOGGER
            .error(new IllegalStateException("Tool registration failed with result: " + registrationResult));
      }
    }).exceptionally(e -> {
      CopilotCore.LOGGER.error("Error registering tools with the server", e);
      return null;
    });
  }

  /**
   * Register a tool.
   *
   * @param tool The tool to register
   */
  public void registerTool(BaseTool tool) {
    tools.put(tool.getToolName(), tool);
  }

  /**
   * Unregister a tool.
   *
   * @param toolName The name of the tool to unregister
   * @return The removed tool, or null if not found
   */
  public BaseTool unregisterTool(String toolName) {
    return tools.remove(toolName);
  }

  /**
   * Get a tool by name.
   *
   * @param toolName The name of the tool
   * @return The tool, or null if not found
   */
  private BaseTool getTool(String toolName) {
    return tools.get(toolName);
  }

  /**
   * Get all registered tools.
   *
   * @return An unmodifiable collection of all tools
   */
  public Collection<BaseTool> getAllTools() {
    return Collections.unmodifiableCollection(tools.values());
  }

  /**
   * Bind the chat view to the auth status.
   */
  public void bindChatView(ChatView chatView) {
    if (chatView == null) {
      return;
    }

    // Unbind any previously bound chat view
    unbindChatView();
    this.boundChatView = chatView;
  }

  /**
   * Unbind the currently bound chat view if any.
   */
  public void unbindChatView() {
    boundChatView = null;
  }

  /**
   * Invoke a tool by its name.
   *
   * @param toolName The name of the tool to invoke
   * @return The result of the tool invocation, or null if the tool was not found
   */
  public CompletableFuture<LanguageModelToolResult[]> invokeTool(String toolName, @Nullable Map<String, Object> input,
      String turnId, @Nullable ChatView chatView) {
    BaseTool tool = getTool(toolName);
    LanguageModelToolResult result = new LanguageModelToolResult();
    if (tool == null) {
      result.addContent("Tool invocation failed due to the tool not found: " + toolName);
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
    }

    BaseTurnWidget turnWidget = boundChatView.getChatContentViewer().getTurnWidget(turnId);
    AtomicReference<CompletableFuture<Boolean>> ref = new AtomicReference<>();
    if (tool.needConfirmation()) {
      SwtUtils.invokeOnDisplayThread(() -> {
        ref.set(turnWidget.requestToolExecutionConfirmation(tool.getConfirmedMessage()));
      });
    } else {
      ref.set(CompletableFuture.completedFuture(true));
    }

    return ref.get().thenCompose(confirmed -> {
      if (Boolean.TRUE.equals(confirmed)) {
        try {
          return tool.invoke(input, chatView);
        } catch (Exception e) {
          CopilotCore.LOGGER.error("Error invoking the tool " + toolName + " due to exception: ", e);
          result.addContent("Tool invocation failed due to exception: " + e.getMessage());
          return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
        }
      } else {
        CopilotCore.LOGGER.info("Tool invocation cancelled by user.");
        result.addContent("Tool invocation cancelled by user.");
        return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
      }
    });
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> onToolInvocation(InvokeClientToolParams params) {
    if (boundChatView == null || !Objects.equals(params.getConversationId(), boundChatView.getConversationId())) {
      return null;
    }

    ChatContentViewer chatContentViewer = boundChatView.getChatContentViewer();
    if (chatContentViewer == null || chatContentViewer.getTurnWidget(params.getTurnId()) == null) {
      return null;
    }
    return invokeTool(params.getName(), (Map<String, Object>) params.getInput(), params.getTurnId(), boundChatView);
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    this.tools.clear();
    unbindChatView();
  }
}
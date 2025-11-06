package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.RegisterToolsParams;
import com.microsoft.copilot.eclipse.terminal.api.IRunInTerminalTool;
import com.microsoft.copilot.eclipse.terminal.api.TerminalServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.BaseTurnWidget;
import com.microsoft.copilot.eclipse.ui.chat.ChatContentViewer;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.tools.BaseTool;
import com.microsoft.copilot.eclipse.ui.chat.tools.CreateFileTool;
import com.microsoft.copilot.eclipse.ui.chat.tools.EditFileTool;
import com.microsoft.copilot.eclipse.ui.chat.tools.GetErrorsTool;
import com.microsoft.copilot.eclipse.ui.chat.tools.RunInTerminalToolAdapter;
import com.microsoft.copilot.eclipse.ui.chat.tools.RunInTerminalToolAdapter.GetTerminalOutputTool;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Service to manage and access tools.
 */
public class AgentToolService implements ToolInvocationListener, TerminalServiceManager.TerminalServiceListener {
  private ConcurrentHashMap<String, BaseTool> tools;
  private ChatView boundChatView;

  protected CopilotLanguageServerConnection lsConnection;
  private volatile boolean terminalToolsRegistered = false;
  private List<LanguageModelToolInformation> cachedBuiltInTools;

  /**
   * Constructor for AgentToolService.
   */
  public AgentToolService(CopilotLanguageServerConnection lsConnection) {
    this.tools = new ConcurrentHashMap<>();
    this.lsConnection = lsConnection;
    TerminalServiceManager terminalManager = TerminalServiceManager.getInstance();
    if (terminalManager != null) {
      terminalManager.addListener(this);
    }
    registerDefaultTools();
  }

  @Override
  public void onServiceAvailable(IRunInTerminalTool service) {
    if (!terminalToolsRegistered) {
      synchronized (this) {
        if (!terminalToolsRegistered) {
          registerTerminalTools();
          registerToolWithServer();
          terminalToolsRegistered = true;
        }
      }
    }
  }

  /**
   * Register default tools.
   */
  private void registerDefaultTools() {
    // File operations
    registerTool(new CreateFileTool());
    registerTool(new EditFileTool());

    // Diagnostic tools
    registerTool(new GetErrorsTool());

    // Register the tools to the language server and cache the result
    registerToolWithServer().thenAccept(toolList -> {
      cachedBuiltInTools = toolList;
    });

    ChatEventsManager chatEventsManager = CopilotCore.getPlugin().getChatEventsManager();
    if (chatEventsManager != null) {
      chatEventsManager.registerAgentToolListener(this);
    }
  }

  /**
   * Register tools to the language server and get the list of registered tools.
   * This is called with different tool sets depending on the chat mode.
   *
   * @return A CompletableFuture containing the list of registered tools from the language server
   */
  private CompletableFuture<List<LanguageModelToolInformation>> registerToolWithServer() {
    RegisterToolsParams registerToolsParams = new RegisterToolsParams();
    for (BaseTool tool : getAllTools()) {
      registerToolsParams.addTool(tool.getToolInformation());
    }

    return lsConnection.registerTools(registerToolsParams);
  }

  private void registerTerminalTools() {
    registerTool(new RunInTerminalToolAdapter());
    registerTool(new GetTerminalOutputTool());
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
  public BaseTool getTool(String toolName) {
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
   * Get the cached built-in tools information.
   * This method is intended for MCP preference service and always returns the cached result.
   *
   * @return An unmodifiable list of built-in tool information, or empty list if not yet initialized
   */
  public List<LanguageModelToolInformation> getBuiltInTools() {
    return cachedBuiltInTools != null ? Collections.unmodifiableList(cachedBuiltInTools) : Collections.emptyList();
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
      @Nullable ChatView chatView) {
    BaseTool tool = getTool(toolName);
    LanguageModelToolResult result = new LanguageModelToolResult();
    if (tool == null) {
      result.addContent("Tool invocation failed due to the tool not found: " + toolName);
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
    }

    return tool.invoke(input, chatView);
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> onToolInvocation(InvokeClientToolParams params) {
    if (!validToolConfirmInvokeParams(params.getConversationId(), params.getTurnId())) {
      return null;
    }
    return invokeTool(params.getName(), (Map<String, Object>) params.getInput(), boundChatView);
  }

  @Override
  public CompletableFuture<LanguageModelToolConfirmationResult> onToolConfirmation(
      InvokeClientToolConfirmationParams params) {
    if (!validToolConfirmInvokeParams(params.getConversationId(), params.getTurnId())) {
      return null;
    }

    BaseTurnWidget turnWidget = boundChatView.getChatContentViewer().getTurnWidget(params.getTurnId());
    if (turnWidget == null) {
      return null;
    }
    AtomicReference<CompletableFuture<LanguageModelToolConfirmationResult>> ref = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      ref.set(turnWidget.requestToolExecutionConfirmation(params.getTitle(), params.getMessage(), params.getInput()));
      boundChatView.getChatContentViewer().refreshScrollerLayout();
    });

    return ref.get();
  }

  private boolean validToolConfirmInvokeParams(String conversationId, String turnId) {
    if (boundChatView == null || !Objects.equals(conversationId, boundChatView.getConversationId())) {
      return false;
    }

    ChatContentViewer chatContentViewer = boundChatView.getChatContentViewer();
    if (chatContentViewer == null || chatContentViewer.getTurnWidget(turnId) == null) {
      return false;
    }
    return true;
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    // Remove listener from terminal service manager
    TerminalServiceManager terminalManager = TerminalServiceManager.getInstance();
    if (terminalManager != null) {
      terminalManager.removeListener(this);
    }

    this.tools.clear();
    unbindChatView();
  }
}
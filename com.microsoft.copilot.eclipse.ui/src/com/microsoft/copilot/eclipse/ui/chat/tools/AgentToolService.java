package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.Nullable;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Service to manage and access tools.
 */
public class AgentToolService {
  private ConcurrentHashMap<String, BaseTool> tools;

  /**
   * Private constructor for singleton pattern.
   */
  public AgentToolService() {
    tools = new ConcurrentHashMap<>();
    registerDefaultTools();
  }

  /**
   * Register default tools.
   */
  private void registerDefaultTools() {
    // TODO: Register additional default tools here
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
   * Invoke a tool by its name.
   *
   * @param toolName The name of the tool to invoke
   * @return The result of the tool invocation, or null if the tool was not found
   */
  public String invokeTool(String toolName, @Nullable InputSchema inputSchema, @Nullable ChatView chatView) {
    BaseTool tool = getTool(toolName);
    if (tool != null) {
      try {
        return tool.invoke(inputSchema, chatView).get();
      } catch (InterruptedException | ExecutionException e) {
        String errorMessage = "Failed to invoke tool: " + toolName + ", " + e.getMessage();
        CopilotCore.LOGGER.error(errorMessage, e);
        return errorMessage;
      }
    }
    return "";
  }
}
package com.microsoft.copilot.eclipse.core.chat;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;

/**
 * Represents a built-in chat mode from the conversation/modes API.
 *
 * <p>Built-in modes include:
 * <ul>
 * <li><b>Ask</b>: General-purpose conversational mode for coding assistance.
 *     Provides standard Q&A interaction without tool configuration.</li>
 * <li><b>Agent</b>: Advanced mode with MCP tool integration for complex tasks.
 *     Allows Copilot to use configured tools to perform actions like file operations,
 *     running commands, and integration with external services.</li>
 * <li><b>Plan</b>: Planning and task breakdown mode for complex development tasks.
 *     Helps break down large tasks into actionable steps with execution plans.
 *     Uses Agent UI but without tool configuration.</li>
 * </ul>
 */
public class BuiltInChatMode extends BaseChatMode {

  // Built-in mode names
  public static final String ASK_MODE_NAME = "Ask";
  public static final String AGENT_MODE_NAME = "Agent";
  public static final String PLAN_MODE_NAME = "Plan";
  public static final String DEBUGGER_MODE_NAME = "Debugger";

  /**
   * Constructor that creates a BuiltInChatMode from a ConversationMode.
   *
   * @param mode the ConversationMode from the LSP API
   */
  public BuiltInChatMode(ConversationMode mode) {
    super(mode.getId(), mode.getName(), mode.getDescription(),
        mode.getCustomTools(), mode.getModel(), mode.getHandOffs());
  }

  /**
   * Determines whether this mode allows tool configuration.
   * Only the Agent mode allows tool configuration.
   *
   * @return true if this is the Agent mode, false otherwise
   */
  @Override
  public boolean allowsToolConfiguration() {
    return AGENT_MODE_NAME.equalsIgnoreCase(this.displayName);
  }

  /**
   * Determines whether this is a built-in mode.
   *
   * @return always returns true for BuiltInChatMode
   */
  @Override
  public boolean isBuiltIn() {
    return true;
  }
}
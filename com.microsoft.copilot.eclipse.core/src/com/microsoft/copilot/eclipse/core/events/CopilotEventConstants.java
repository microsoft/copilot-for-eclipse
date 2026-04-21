// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.events;

/**
 * Constants for Copilot event topics.
 */
public class CopilotEventConstants {

  /**
   * Base topic for all Copilot events.
   */
  private static final String TOPIC_BASE = "com/microsoft/copilot/eclipse/";

  /**
   * Topic for chat events.
   */
  private static final String TOPIC_CHAT = TOPIC_BASE + "CHAT/";

  private static final String TOPIC_POLICY = TOPIC_BASE + "POLICY/";

  /**
   * Topic for auth events.
   */
  private static final String TOPIC_AUTH = TOPIC_BASE + "AUTH/";

  /**
   * Topic for MCP events.
   */
  private static final String TOPIC_MCP = TOPIC_BASE + "MCP/";

  /**
   * Topic for Next Edit Suggestion (NES) events.
   */
  private static final String TOPIC_NES = TOPIC_BASE + "NES/";

  /**
   * Event when new conversation is started.
   */
  public static final String TOPIC_CHAT_NEW_CONVERSATION = TOPIC_CHAT + "NEW_CONVERSATION";

  /**
   * Event when a chat message is cancelled.
   */
  public static final String TOPIC_CHAT_MESSAGE_CANCELLED = TOPIC_CHAT + "MESSAGE_CANCELLED";

  /**
   * Event when auth status changed.
   */
  public static final String TOPIC_AUTH_STATUS_CHANGED = TOPIC_AUTH + "STATUS_CHANGED";

  /**
   * Event when MCP tools changed.
   */
  public static final String ON_DID_CHANGE_MCP_TOOLS = TOPIC_CHAT + "ON_DID_CHANGE_MCP_TOOLS";

  /**
   * Event when the chat message to Copilot should be sent.
   */
  public static final String TOPIC_CHAT_ON_SEND = TOPIC_CHAT + "ON_SEND";

  /**
   * Event when a message is sent from the action bar.
   */
  public static final String TOPIC_CHAT_MESSAGE_SEND = TOPIC_CHAT + "MESSAGE_SEND";

  /**
   * Event when MCP runtime log is received.
   */
  public static final String TOPIC_CHAT_MCP_RUNTIME_LOG = TOPIC_CHAT + "MCP_RUNTIME_LOG";

  /**
   * Event when the chat feature flag are updated.
   */
  public static final String TOPIC_CHAT_DID_CHANGE_FEATURE_FLAGS = TOPIC_CHAT + "DID_CHANGE_FEATURE_FLAGS";

  /**
   * Event when the mcp contribution point group policy flag are updated.
   */
  public static final String TOPIC_DID_CHANGE_MCP_CONTRIBUTION_POINT_POLICY = TOPIC_POLICY
      + "MCP_CONTRIBUTION_POINT_ENABLED";

  /**
   * Event when the sub-agent policy flag is updated.
   */
  public static final String TOPIC_DID_CHANGE_SUB_AGENT_POLICY = TOPIC_POLICY + "SUB_AGENT_ENABLED";

  /**
   * Event when the custom agent policy flag is updated.
   */
  public static final String TOPIC_DID_CHANGE_CUSTOM_AGENT_POLICY = TOPIC_POLICY + "CUSTOM_AGENT_ENABLED";

  /**
   * Event when the chat mode is changed.
   */
  public static final String TOPIC_CHAT_MODE_CHANGED = TOPIC_CHAT + "MODE_CHANGED";

  /**
   * Event when the auto breakpoint response toggle is changed.
   */
  public static final String TOPIC_CHAT_AUTO_BREAKPOINT_TOGGLE = TOPIC_CHAT + "AUTO_BREAKPOINT_TOGGLE";

  /**
   * Event when the chat history visibility is toggled to hide chat history.
   */
  public static final String TOPIC_CHAT_HIDE_CHAT_HISTORY = TOPIC_CHAT + "HIDE_CHAT_HISTORY";

  /**
   * Event when the chat history visibility is toggled to show chat history.
   */
  public static final String TOPIC_CHAT_SHOW_CHAT_HISTORY = TOPIC_CHAT + "SHOW_CHAT_HISTORY";

  /**
   * Event when the back button is clicked in chat history view.
   */
  public static final String TOPIC_CHAT_HISTORY_BACK_CLICKED = TOPIC_CHAT + "BACK_TO_CHAT_CLICKED";

  /**
   * Event when a conversation is selected in chat history view.
   */
  public static final String TOPIC_CHAT_HISTORY_CONVERSATION_SELECTED = TOPIC_CHAT + "HISTORY_CONVERSATION_SELECTED";

  /**
   * Event when a conversation title is updated.
   */
  public static final String TOPIC_CHAT_CONVERSATION_TITLE_UPDATED = TOPIC_CHAT + "CONVERSATION_TITLE_UPDATED";

  /**
   * Event when BYOK models are updated.
   */
  public static final String TOPIC_CHAT_BYOK_MODELS_UPDATED = TOPIC_CHAT + "BYOK_MODELS_UPDATED";

  /**
   * Event when switching to a custom mode with a specific model.
   */
  public static final String TOPIC_CHAT_CUSTOM_MODE_MODEL_CHANGED = TOPIC_CHAT + "CUSTOM_MODE_MODEL_CHANGED";

  /**
   * Event when a coding agent message is received.
   */
  public static final String TOPIC_CHAT_CODING_AGENT_MESSAGE = TOPIC_CHAT + "CODING_AGENT_MESSAGE";

  /**
   * Event when MCP server state changes (install, uninstall, etc.).
   */
  public static final String TOPIC_MCP_SERVER_STATE_CHANGE = TOPIC_MCP + "SERVER_STATE_CHANGE";

  /**
   * Event when NES suggestion is accepted.
   */
  public static final String TOPIC_NES_ACCEPT_SUGGESTION = TOPIC_NES + "ACCEPT_SUGGESTION";

  /**
   * Event when NES suggestion is rejected.
   */
  public static final String TOPIC_NES_REJECT_SUGGESTION = TOPIC_NES + "REJECT_SUGGESTION";

  /**
   * Event when a rate limit warning is received from the language server.
   */
  public static final String TOPIC_RATE_LIMIT_WARNING = TOPIC_CHAT + "RATE_LIMIT_WARNING";
}

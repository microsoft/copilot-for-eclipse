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

  /**
   * Topic for auth events.
   */
  private static final String TOPIC_AUTH = TOPIC_BASE + "AUTH/";

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
   * Event when MCP runtime log is received.
   */
  public static final String TOPIC_CHAT_MCP_RUNTIME_LOG = TOPIC_CHAT + "MCP_RUNTIME_LOG";

  /**
   * Event when the chat feature flag are updated.
   */
  public static final String TOPIC_CHAT_DID_CHANGE_FEATURE_FLAGS = TOPIC_CHAT + "DID_CHANGE_FEATURE_FLAGS";

  /**
   * Event when the chat mode is changed.
   */
  public static final String TOPIC_CHAT_MODE_CHANGED = TOPIC_CHAT + "MODE_CHANGED";

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
   * Event when BYOK models are updated.
   */
  public static final String TOPIC_CHAT_BYOK_MODELS_UPDATED = TOPIC_CHAT + "BYOK_MODELS_UPDATED";
}
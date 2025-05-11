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
}
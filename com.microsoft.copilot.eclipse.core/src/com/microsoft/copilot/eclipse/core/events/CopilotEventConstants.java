package com.microsoft.copilot.eclipse.core.events;

/**
 * Constants for Copilot event topics.
 */
public class CopilotEventConstants {

  /**
   * Base topic for all Copilot events.
   */
  public static final String TOPIC_BASE = "com/microsoft/copilot/eclipse/";

  /**
   * Topic for chat events.
   */
  public static final String TOPIC_CHAT = TOPIC_BASE + "CHAT/";

  /**
   * Event when a chat message is cancelled.
   */
  public static final String TOPIC_CHAT_MESSAGE_CANCELLED = TOPIC_CHAT + "MESSAGE_CANCELLED";
}
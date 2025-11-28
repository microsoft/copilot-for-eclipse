package com.microsoft.copilot.eclipse.ui.jobs.events;

/**
 * Constants for Jobs View events.
 */
public final class JobsViewEvents {

  /** Base topic for all Jobs View events. */
  public static final String TOPIC_BASE = "com/microsoft/copilot/eclipse/ui/jobs";

  /** Event topic for refresh request. */
  public static final String TOPIC_REFRESH = TOPIC_BASE + "/REFRESH";

  /** Event topic for collapse all request. */
  public static final String TOPIC_COLLAPSE_ALL = TOPIC_BASE + "/COLLAPSE_ALL";

  /** Event topic for expand all request. */
  public static final String TOPIC_EXPAND_ALL = TOPIC_BASE + "/EXPAND_ALL";

  private JobsViewEvents() {
    // Prevent instantiation
  }
}

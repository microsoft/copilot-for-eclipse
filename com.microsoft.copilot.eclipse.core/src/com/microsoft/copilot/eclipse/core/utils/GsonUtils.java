package com.microsoft.copilot.eclipse.core.utils;

import com.google.gson.Gson;

/**
 * Utility class providing shared Gson instances for JSON serialization/deserialization.
 *
 * <p>Gson instances are thread-safe and reusable. Using shared instances avoids unnecessary object allocations and
 * improves performance.
 */
public final class GsonUtils {

  /** Default Gson instance for simple serialization. */
  private static final Gson DEFAULT = new Gson();

  private GsonUtils() {
    // Utility class, not instantiable
  }

  /**
   * Returns the default Gson instance.
   *
   * @return the default Gson instance
   */
  public static Gson getDefault() {
    return DEFAULT;
  }
}

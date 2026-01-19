package com.microsoft.copilot.eclipse.core.utils;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Utility class for checking Java Development Tools (JDT) bundle availability.
 */
public final class JdtUtils {

  private JdtUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Check if all required JDT debug bundles are available.
   *
   * @return true if all required JDT debug bundles (debug.core, jdt.core, jdt.debug) are present
   */
  public static boolean isJdtDebugAvailable() {
    Bundle debugCore = Platform.getBundle("org.eclipse.debug.core");
    Bundle jdtCore = Platform.getBundle("org.eclipse.jdt.core");
    Bundle jdtDebug = Platform.getBundle("org.eclipse.jdt.debug");
    return debugCore != null && jdtCore != null && jdtDebug != null;
  }

  /**
   * Check if the debug core bundle is available.
   *
   * @return true if org.eclipse.debug.core bundle is present
   */
  public static boolean isDebugCoreAvailable() {
    Bundle debugCore = Platform.getBundle("org.eclipse.debug.core");
    return debugCore != null;
  }
}

package com.microsoft.copilot.eclipse.core.utils;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * Utility class for platform related operations.
 */
public class PlatformUtils {

  public static final String EC_PLATFORM_BUNDLE_NAME = "org.eclipse.platform";

  private PlatformUtils() {
  }

  /**
   * Get the version of the Eclipse platform.
   */
  public static String getEclipseVersion() {
    Bundle bundle = Platform.getBundle(EC_PLATFORM_BUNDLE_NAME);
    if (bundle == null) {
      return "unknown";
    }
    return bundle.getVersion().toString();
  }

  /**
   * Escapes spaces in a URL string.
   */
  public static String escapeSpaceInUrl(String urlString) {
    char[] chars = urlString.toCharArray();
    StringBuffer sb = new StringBuffer(chars.length);
    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == ' ') {
        sb.append("%20");
      } else {
        sb.append(chars[i]);
      }
    }
    return sb.toString();
  }

  public static boolean isMac() {
    return Platform.getOS().equals(Platform.OS_MACOSX);
  }

  public static boolean isLinux() {
    return Platform.getOS().equals(Platform.OS_LINUX);
  }

  public static boolean isWindows() {
    return Platform.getOS().equals(Platform.OS_WIN32);
  }

  public static boolean isIntel64() {
    return Platform.getOSArch().equals(Platform.ARCH_X86_64);
  }

  public static boolean isArm64() {
    return Platform.getOSArch().equals(Platform.ARCH_AARCH64);
  }

}

package com.microsoft.copilot.eclipse.ui.testers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Property tester to compare the current Eclipse platform version against a specified version. Only compares major and
 * minor version parts, ignoring micro and qualifier. The platform version is cached on first access to avoid repeated
 * bundle lookups.
 */
public class PlatformVersionTester extends PropertyTester {

  /**
   * Property name to test platform version comparisons.
   */
  private static final String PROP = "platformVersion";

  /**
   * Cached platform version to avoid repeated bundle lookups. Initialized lazily using the initialization-on-demand
   * holder idiom for thread safety.
   */
  private Version cachedPlatformVersion;
  private final Object lock = new Object();

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if (!PROP.equals(property)) {
      return false;
    }
    // args[0] expected in form: "le:4.36" | "lt:4.36" | "ge:4.35" | "gt:4.35" | "eq:4.36"
    String spec = (args != null && args.length > 0 && args[0] instanceof String) ? (String) args[0] : null;
    if (spec == null || !spec.contains(":")) {
      return false;
    }
    String[] parts = spec.split(":", 2);
    String op = parts[0].trim().toLowerCase();
    String targetStr = normalizeVersion(parts[1].trim());

    Version current = getMajorMinorVersion(getCachedPlatformVersion());
    Version target = getMajorMinorVersion(toVersion(targetStr));
    if (current == null || target == null) {
      return false;
    }

    int cmp = current.compareTo(target);
    return switch (op) {
      case "lt" -> cmp < 0;
      case "le", "lte" -> cmp <= 0;
      case "gt" -> cmp > 0;
      case "ge", "gte" -> cmp >= 0;
      case "eq", "==" -> cmp == 0;
      case "ne", "!=" -> cmp != 0;
      default -> false;
    };
  }

  /**
   * Get the cached platform version, initializing it on first access. Uses double-checked locking pattern for
   * thread-safe lazy initialization. This method ensures that getPlatformVersion() is called only once per JVM
   * lifetime.
   *
   * @return the cached platform version, or null if unavailable
   */
  private Version getCachedPlatformVersion() {
    if (cachedPlatformVersion == null) {
      synchronized (lock) {
        cachedPlatformVersion = getPlatformVersion();
      }
    }
    return cachedPlatformVersion;
  }

  /**
   * Retrieves the platform version from the org.eclipse.platform bundle. This method is called only once and the result
   * is cached.
   *
   * @return the platform version, or null if the bundle is not available
   */
  private Version getPlatformVersion() {
    // org.eclipse.platform bundle version aligns with the Eclipse platform version (e.g., 4.36.0)
    Bundle b = Platform.getBundle("org.eclipse.platform");
    return (b != null) ? b.getVersion() : null;
  }

  /**
   * Extract only the major and minor parts of a version, setting micro to 0 and qualifier to empty. This ensures we
   * only compare significant version differences.
   *
   * @param version the original version
   * @return a new Version with only major.minor.0 format, or null if input is null
   */
  private Version getMajorMinorVersion(Version version) {
    if (version == null) {
      return null;
    }
    return new Version(version.getMajor(), version.getMinor(), 0);
  }

  // Accept "4.36" -> "4.36.0" and strip micro/qualifier parts for major.minor only comparison
  private String normalizeVersion(String v) {
    if (v == null || v.isEmpty()) {
      return v;
    }
    String[] segs = v.split("\\.");

    // Extract only major and minor parts
    if (segs.length >= 2) {
      // Return major.minor.0 format
      return segs[0] + "." + segs[1] + ".0";
    } else if (segs.length == 1) {
      // Single number like "4" -> "4.0.0"
      return v + ".0.0";
    }

    return v;
  }

  private Version toVersion(String v) {
    try {
      return Version.parseVersion(v);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Invalid version format: " + v, e);
    }
    return null;
  }
}

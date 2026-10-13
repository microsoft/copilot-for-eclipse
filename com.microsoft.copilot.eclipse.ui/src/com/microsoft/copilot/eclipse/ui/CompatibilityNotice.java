// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.copilot.eclipse.core.Constants;

/**
 * Determines whether the current environment needs the 0.23 compatibility announcement.
 */
public final class CompatibilityNotice {

  private static final Version MINIMUM_ECLIPSE_VERSION = new Version(4, 38, 0);
  private static final int MINIMUM_JAVA_FEATURE = 21;

  private final IEclipsePreferences preferences;

  /**
   * Creates an announcement using configuration-scoped dismissal preferences.
   *
   * @param preferences the preferences shared by workspaces in this Eclipse configuration
   */
  public CompatibilityNotice(IEclipsePreferences preferences) {
    this.preferences = preferences;
  }

  /**
   * Checks whether the current environment will be unsupported in version 0.23.0.
   *
   * @param platformVersion the version of the org.eclipse.platform bundle, or null when unavailable
   * @param javaFeature the Java feature version used to run Eclipse, not the project JDK
   * @return whether the announcement should be displayed
   */
  public boolean shouldShow(Version platformVersion, int javaFeature) {
    return !preferences.getBoolean(Constants.SUPPRESS_COMPATIBILITY_NOTICE_0_23, false)
        && (javaFeature < MINIMUM_JAVA_FEATURE
            || platformVersion != null && platformVersion.compareTo(MINIMUM_ECLIPSE_VERSION) < 0);
  }

  /**
   * Persists dismissal of this announcement. Call outside the UI thread because preferences are flushed to disk.
   *
   * @throws BackingStoreException if the dismissal cannot be persisted
   */
  public void dismiss() throws BackingStoreException {
    preferences.putBoolean(Constants.SUPPRESS_COMPATIBILITY_NOTICE_0_23, true);
    preferences.flush();
  }
}

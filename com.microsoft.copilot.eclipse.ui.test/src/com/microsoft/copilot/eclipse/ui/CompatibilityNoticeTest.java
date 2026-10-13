// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.copilot.eclipse.core.Constants;

class CompatibilityNoticeTest {

  private IEclipsePreferences preferences;

  @BeforeEach
  void setUp() {
    preferences = ConfigurationScope.INSTANCE.getNode("copilot.compatibility.test." + UUID.randomUUID());
  }

  @AfterEach
  void tearDown() throws BackingStoreException {
    var parent = preferences.parent();
    preferences.removeNode();
    parent.flush();
  }

  @Test
  void testShouldShow_eclipse437WithJava21_returnsTrue() {
    CompatibilityNotice notice = new CompatibilityNotice(preferences);

    assertTrue(notice.shouldShow(new Version(4, 37, 99, "release"), 21));
  }

  @Test
  void testShouldShow_eclipse438WithJava20_returnsTrue() {
    CompatibilityNotice notice = new CompatibilityNotice(preferences);

    assertTrue(notice.shouldShow(new Version(4, 38, 0), 20));
  }

  @Test
  void testDismiss_nextStartupWithOldEnvironment_doesNotShow() throws BackingStoreException {
    CompatibilityNotice notice = new CompatibilityNotice(preferences);
    notice.dismiss();

    CompatibilityNotice nextStartup = new CompatibilityNotice(
        ConfigurationScope.INSTANCE.getNode(preferences.name()));

    assertFalse(nextStartup.shouldShow(new Version(4, 37, 0), 17));
  }

  @Test
  void testShouldShow_unknownPlatformWithJava17_returnsTrue() {
    CompatibilityNotice notice = new CompatibilityNotice(preferences);

    assertTrue(notice.shouldShow(null, 17));
  }

  @ParameterizedTest
  @CsvSource({"4.38.0, 21", "4.38.1.release, 21", "4.40.0, 25", "5.0.0, 21"})
  void testShouldShow_supportedEnvironment_returnsFalse(String platformVersion, int javaFeature) {
    CompatibilityNotice notice = new CompatibilityNotice(preferences);

    assertFalse(notice.shouldShow(Version.parseVersion(platformVersion), javaFeature));
  }

  @Test
  void testShouldShow_unknownPlatformWithJava21_doesNotAssumeUnsupportedPlatform() {
    CompatibilityNotice notice = new CompatibilityNotice(preferences);

    assertFalse(notice.shouldShow(null, 21));
  }

  @Test
  void testShouldShow_nextStartupWithoutDismissal_returnsTrue() {
    new CompatibilityNotice(preferences).shouldShow(new Version(4, 37, 0), 21);

    CompatibilityNotice nextStartup = new CompatibilityNotice(preferences);

    assertTrue(nextStartup.shouldShow(new Version(4, 37, 0), 21));
  }

  @Test
  void testShouldShow_otherNoticesDisabled_returnsTrue() {
    preferences.putBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, false);
    preferences.putBoolean(Constants.SUPPRESS_TERMINAL_DEPENDENCY_DIALOG, true);
    CompatibilityNotice notice = new CompatibilityNotice(preferences);

    assertTrue(notice.shouldShow(new Version(4, 37, 0), 21));
  }
}

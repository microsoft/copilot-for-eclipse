package com.microsoft.copilot.eclipse.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.Constants;

/**
 * Test cases for CopilotPreferenceInitializer.
 */
class CopilotPreferenceInitializerTest {

  private CopilotPreferenceInitializer initializer;
  private IEclipsePreferences configPrefs;

  @BeforeEach
  void setUp() {
    initializer = new CopilotPreferenceInitializer();
    configPrefs = ConfigurationScope.INSTANCE.getNode(Constants.PLUGIN_ID);

    // Clean up any existing preference to ensure clean test state
    configPrefs.remove(Constants.AUTO_SHOW_WHAT_IS_NEW);
  }

  @Test
  void testInitializeDefaultPreferences_WhenAutoShowWhatsNewNotSet_ShouldSetToTrue() {
    initializer.initializeDefaultPreferences();

    boolean autoShowWhatsNew = configPrefs.getBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, false);
    assertTrue(autoShowWhatsNew);
  }

  @Test
  void testInitializeDefaultPreferences_WhenAutoShowWhatsNewSetToFalse_ShouldRemainFalse() {
    configPrefs.putBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, false);

    initializer.initializeDefaultPreferences();

    boolean autoShowWhatsNew = configPrefs.getBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, true);
    assertFalse(autoShowWhatsNew);
  }
}
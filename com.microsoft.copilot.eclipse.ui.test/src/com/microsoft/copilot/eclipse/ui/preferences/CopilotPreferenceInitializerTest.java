package com.microsoft.copilot.eclipse.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

/**
 * Test cases for CopilotPreferenceInitializer.
 */
class CopilotPreferenceInitializerTest {

  private CopilotPreferenceInitializer initializer;
  private IEclipsePreferences configPrefs;

  @BeforeEach
  void setUp() {
    initializer = new CopilotPreferenceInitializer();
    configPrefs = ConfigurationScope.INSTANCE.getNode(CopilotUi.getPlugin().getBundle().getSymbolicName());

    // Clean up any existing preference to ensure clean test state
    configPrefs.remove(Constants.AUTO_SHOW_WHAT_IS_NEW);
  }

  @Test
  void testInitializeDefaultPreferences_WhenAutoShowWhatsNewSetToFalse_ShouldRemainFalse() {
    configPrefs.putBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, false);

    initializer.initializeDefaultPreferences();

    boolean autoShowWhatsNew = configPrefs.getBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, true);
    assertFalse(autoShowWhatsNew);
  }
}
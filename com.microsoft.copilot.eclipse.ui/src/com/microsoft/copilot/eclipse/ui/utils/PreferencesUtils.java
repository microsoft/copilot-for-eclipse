package com.microsoft.copilot.eclipse.ui.utils;

import com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage;
import com.microsoft.copilot.eclipse.ui.preferences.ChatPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CompletionsPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CopilotPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CustomInstructionPreferencePage;
import com.microsoft.copilot.eclipse.ui.preferences.CustomModesPreferencePage;
import com.microsoft.copilot.eclipse.ui.preferences.GeneralPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;

/**
 * Utility class for managing user preferences in the Eclipse Copilot plugin.
 */
public class PreferencesUtils {

  private PreferencesUtils() {
    // Private constructor to prevent instantiation
  }

  public static String[] getAllPreferenceIds() {
    return new String[] { CopilotPreferencesPage.ID, GeneralPreferencesPage.ID, ChatPreferencesPage.ID,
        CompletionsPreferencesPage.ID, CustomInstructionPreferencePage.ID, CustomModesPreferencePage.ID,
        McpPreferencePage.ID, ByokPreferencePage.ID };
  }

}

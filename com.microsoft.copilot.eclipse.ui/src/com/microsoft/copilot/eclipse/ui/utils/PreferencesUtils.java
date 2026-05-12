// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
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

  /**
   * Returns whether the skills feature is enabled. Skills require both the user preference
   * {@link Constants#ENABLE_SKILLS} to be set and the client preview feature flag to be enabled.
   *
   * @return {@code true} if skills are enabled, {@code false} otherwise
   */
  public static boolean isSkillsEnabled() {
    CopilotCore plugin = CopilotCore.getPlugin();
    FeatureFlags flags = plugin != null ? plugin.getFeatureFlags() : null;
    return CopilotUi.getPlugin().getPreferenceStore().getBoolean(Constants.ENABLE_SKILLS)
        && flags != null && flags.isClientPreviewFeatureEnabled();
  }

}

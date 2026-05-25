// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.CustomInstructionsChatLoadScope;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.preferences.AutoApprovePreferencePage;
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
        McpPreferencePage.ID, ByokPreferencePage.ID, AutoApprovePreferencePage.ID };
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

  /**
   * Returns the current value for the scope used for loading custom instructions in the chat.
   *
   * @param preferenceStore the preference store to read from
   * @return the current setting from {@link InstanceScope}
   */
  public static CustomInstructionsChatLoadScope getCustomInstructionsChatLoadScope(IPreferenceStore preferenceStore) {
    return getCustomInstructionsChatLoadScopeValue(preferenceStore, false);
  }

  /**
   * Returns the default value for the scope used for loading custom instructions in the chat.
   *
   * @param preferenceStore the preference store to read from
   * @return the current setting from {@link DefaultScope}
   */
  public static CustomInstructionsChatLoadScope getCustomInstructionsChatLoadScopeDefault(
      IPreferenceStore preferenceStore) {
    return getCustomInstructionsChatLoadScopeValue(preferenceStore, true);
  }

  private static CustomInstructionsChatLoadScope getCustomInstructionsChatLoadScopeValue(
      IPreferenceStore preferenceStore, boolean readDefault) {

    String value = readDefault ? preferenceStore.getDefaultString(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE)
        : preferenceStore.getString(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE);

    try {
      return CustomInstructionsChatLoadScope.fromValue(value);
    } catch (IllegalArgumentException e) {
      CopilotCore.LOGGER.error("Failed to load custom instructions scope. Falling back to default value.", e);

      if (!readDefault) {
        // If the stored value is invalid, use the default value instead
        preferenceStore.setValue(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE,
            CustomInstructionsChatLoadScope.DEFAULT_VALUE.getValue());
      }
      return CustomInstructionsChatLoadScope.DEFAULT_VALUE;
    }
  }

}

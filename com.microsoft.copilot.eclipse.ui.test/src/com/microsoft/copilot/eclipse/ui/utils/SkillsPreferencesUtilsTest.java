// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

class SkillsPreferencesUtilsTest {

  @Test
  void testIsSkillsEnabled_previewDisabledAndPreferenceEnabled_returnsTrue() {
    IPreferenceStore preferenceStore = mock(IPreferenceStore.class);
    CopilotUi uiPlugin = mock(CopilotUi.class);
    // Keep preview disabled to prevent it from becoming a Skills prerequisite again.
    CopilotCore corePlugin = mock(CopilotCore.class);
    FeatureFlags featureFlags = mock(FeatureFlags.class);
    when(uiPlugin.getPreferenceStore()).thenReturn(preferenceStore);
    when(preferenceStore.getBoolean(Constants.ENABLE_SKILLS)).thenReturn(true);
    when(corePlugin.getFeatureFlags()).thenReturn(featureFlags);
    when(featureFlags.isClientPreviewFeatureEnabled()).thenReturn(false);

    try (MockedStatic<CopilotUi> copilotUi = Mockito.mockStatic(CopilotUi.class);
        MockedStatic<CopilotCore> copilotCore = Mockito.mockStatic(CopilotCore.class)) {
      copilotUi.when(CopilotUi::getPlugin).thenReturn(uiPlugin);
      copilotCore.when(CopilotCore::getPlugin).thenReturn(corePlugin);

      assertTrue(PreferencesUtils.isSkillsEnabled());
    }
  }

  @Test
  void testIsSkillsEnabled_preferenceDisabled_returnsFalse() {
    IPreferenceStore preferenceStore = mock(IPreferenceStore.class);
    CopilotUi uiPlugin = mock(CopilotUi.class);
    when(uiPlugin.getPreferenceStore()).thenReturn(preferenceStore);
    when(preferenceStore.getBoolean(Constants.ENABLE_SKILLS)).thenReturn(false);

    try (MockedStatic<CopilotUi> copilotUi = Mockito.mockStatic(CopilotUi.class)) {
      copilotUi.when(CopilotUi::getPlugin).thenReturn(uiPlugin);

      assertFalse(PreferencesUtils.isSkillsEnabled());
    }
  }
}

package com.microsoft.copilot.eclipse.ui.prerferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

/**
 * A class to initialize the default preferences for the plugin.
 */
public class CopilotPreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore pref = CopilotUi.getPlugin().getPreferenceStore();
    pref.setDefault(Constants.AUTO_SHOW_COMPLETION, true);
  }
}

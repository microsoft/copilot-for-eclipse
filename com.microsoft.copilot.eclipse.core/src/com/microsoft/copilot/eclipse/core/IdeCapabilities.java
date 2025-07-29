package com.microsoft.copilot.eclipse.core;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Class to check the IDE capabilities.
 */
public class IdeCapabilities {
  private Boolean agentModeEnabled = null;

  public Boolean isAgentModeEnabled() {
    return agentModeEnabled;
  }

  public void setAgentModeEnabled(Boolean agentModeEnabled) {
    this.agentModeEnabled = agentModeEnabled;
  }

  /**
   * Checks if code mining capability is available based on Eclipse version.
   *
   * @return true if Eclipse version is 2024.12(4.34) or newer, false otherwise
   */
  public static boolean canUseCodeMining() {
    Bundle platformBundle = Platform.getBundle("org.eclipse.platform");
    if (platformBundle == null) {
      // In test environments, the bundle might not be available
      return false;
    }
    Version currentVersion = platformBundle.getVersion();
    Version requiredVersion = new Version(4, 34, 0);
    return currentVersion.compareTo(requiredVersion) >= 0;
  }

  /**
   * Checks if the workspace context is enabled.
   *
   * @return true if the workspace context is enabled, false otherwise.
   */
  public static boolean isWorkspaceContextEnabled() {
    // Directly access the instance scope of Eclipse preferences, which are preferences that are specific to the
    // current workspace. So the code won't need to involve any component from the UI plugin.
    // The file name for the preferences is "com.microsoft.copilot.eclipse.ui.prefs"
    IEclipsePreferences uiPrefs = InstanceScope.INSTANCE.getNode("com.microsoft.copilot.eclipse.ui");
    if (uiPrefs != null) {
      return uiPrefs.getBoolean(Constants.WORKSPACE_CONTEXT_ENABLED, false);
    }

    return false;
  }
}

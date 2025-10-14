package com.microsoft.copilot.eclipse.core;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Class to manage feature flags for the Copilot plugin. This class allows enabling or disabling features.
 */
public class FeatureFlags {
  private boolean agentModeEnabled = true;

  private boolean mcpEnabled = true;

  private boolean byokEnabled = true;

  private boolean mcpContributionPointEnabled = false;

  public boolean isAgentModeEnabled() {
    return agentModeEnabled;
  }

  public void setAgentModeEnabled(boolean agentModeEnabled) {
    this.agentModeEnabled = agentModeEnabled;
  }

  public boolean isMcpEnabled() {
    return mcpEnabled;
  }

  public void setMcpEnabled(boolean mcpEnabled) {
    this.mcpEnabled = mcpEnabled;
  }

  public boolean isByokEnabled() {
    return byokEnabled;
  }

  public void setByokEnabled(boolean byokEnabled) {
    this.byokEnabled = byokEnabled;
  }

  public boolean isMcpContributionPointEnabled() {
    return mcpContributionPointEnabled;
  }

  public void setMcpContributionPointEnabled(boolean mcpContributionPointEnabled) {
    this.mcpContributionPointEnabled = mcpContributionPointEnabled;
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

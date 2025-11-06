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

  private boolean clientPreviewFeatureEnabled = true;

  private boolean mcpContributionPointEnabled = false;

  private boolean subAgentPolicyEnabled = true;

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

  public boolean isSubAgentPolicyEnabled() {
    return subAgentPolicyEnabled;
  }

  public void setSubAgentPolicyEnabled(boolean subAgentPolicyEnabled) {
    this.subAgentPolicyEnabled = subAgentPolicyEnabled;
  }

  public boolean isClientPreviewFeatureEnabled() {
    return clientPreviewFeatureEnabled;
  }

  public void setClientPreviewFeatureEnabled(boolean clientPreviewFeatureEnabled) {
    this.clientPreviewFeatureEnabled = clientPreviewFeatureEnabled;
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

  /**
   * Checks if the sub-agent is enabled.
   * Sub-agent is enabled only if both the user preference is enabled AND the organization policy allows it.
   *
   * @return true if the sub-agent is enabled, false otherwise.
   */
  public static boolean isSubAgentEnabled() {
    // Check if policy allows sub-agent (defaults to true, so safe to check during initialization)
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    if (flags != null && !flags.isSubAgentPolicyEnabled()) {
      return false;
    }

    // Check user preference
    IEclipsePreferences uiPrefs = InstanceScope.INSTANCE.getNode("com.microsoft.copilot.eclipse.ui");
    if (uiPrefs != null) {
      return uiPrefs.getBoolean(Constants.SUB_AGENT_ENABLED, false);
    }

    return false;
  }
}

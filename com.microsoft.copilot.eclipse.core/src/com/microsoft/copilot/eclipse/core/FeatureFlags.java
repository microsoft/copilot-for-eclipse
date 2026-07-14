// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

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

  private boolean customAgentPolicyEnabled = true;

  private boolean autoApprovalTokenEnabled = true;

  private boolean autoApprovalPolicyEnabled = true;

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

  public boolean isCustomAgentPolicyEnabled() {
    return customAgentPolicyEnabled;
  }

  public void setCustomAgentPolicyEnabled(boolean customAgentPolicyEnabled) {
    this.customAgentPolicyEnabled = customAgentPolicyEnabled;
  }

  /**
   * Returns true if the auto-approval feature is available.
   * Requires both the server token ({@code agent_mode_auto_approval}) and
   * the organization policy ({@code agentMode.autoApproval.enabled}) to permit it.
   *
   * @return true if auto-approval is permitted
   */
  public boolean isAutoApprovalEnabled() {
    return autoApprovalTokenEnabled && autoApprovalPolicyEnabled;
  }

  public void setAutoApprovalTokenEnabled(boolean autoApprovalTokenEnabled) {
    this.autoApprovalTokenEnabled = autoApprovalTokenEnabled;
  }

  public void setAutoApprovalPolicyEnabled(boolean autoApprovalPolicyEnabled) {
    this.autoApprovalPolicyEnabled = autoApprovalPolicyEnabled;
  }

  public boolean isClientPreviewFeatureEnabled() {
    return clientPreviewFeatureEnabled;
  }

  /**
   * Sets whether the client preview feature is enabled.
   *
   * @param clientPreviewFeatureEnabled true to enable the client preview feature, false to disable it
   */
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
   * Checks if the custom agent is enabled.
   * Custom agent is enabled only if the organization policy allows it.
   *
   * @return true if the custom agent is enabled, false otherwise.
   */
  public static boolean isCustomAgentEnabled() {
    // Check if client preview feature is enabled
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    if (flags != null && !flags.isClientPreviewFeatureEnabled()) {
      return false;
    }

    // Check if policy allows custom agent (defaults to true, so safe to check during initialization)
    if (flags != null && !flags.isCustomAgentPolicyEnabled()) {
      return false;
    }

    return true;
  }
}

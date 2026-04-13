// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

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

  private boolean customAgentPolicyEnabled = true;

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

  /**
   * Sets whether the sub-agent policy is enabled.
   * When the policy is disabled, it also disables the user preference for sub-agent.
   *
   * @param subAgentPolicyEnabled true to enable the sub-agent policy, false to disable it
   */
  public void setSubAgentPolicyEnabled(boolean subAgentPolicyEnabled) {
    this.subAgentPolicyEnabled = subAgentPolicyEnabled;

    // When policy disables subagent, also disable the user preference
    if (!subAgentPolicyEnabled) {
      disableSubAgentPreference();
    }
  }

  public boolean isCustomAgentPolicyEnabled() {
    return customAgentPolicyEnabled;
  }

  public void setCustomAgentPolicyEnabled(boolean customAgentPolicyEnabled) {
    this.customAgentPolicyEnabled = customAgentPolicyEnabled;
  }

  public boolean isClientPreviewFeatureEnabled() {
    return clientPreviewFeatureEnabled;
  }

  /**
   * Sets whether the client preview feature is enabled.
   * When the feature is disabled, it also disables the user preference for sub-agent.
   *
   * @param clientPreviewFeatureEnabled true to enable the client preview feature, false to disable it
   */
  public void setClientPreviewFeatureEnabled(boolean clientPreviewFeatureEnabled) {
    this.clientPreviewFeatureEnabled = clientPreviewFeatureEnabled;

    // When client preview feature is disabled, also disable the user preference for subagent
    if (!clientPreviewFeatureEnabled) {
      disableSubAgentPreference();
    }
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
   * Disables the user preference for sub-agent.
   * This method accesses the UI plugin's preference store to set the sub-agent preference to false.
   *
   * <p>Note: The sub-agent preference is used to initialize capabilities which happens before policies
   * are sent to us. Therefore, we need to flush the preference immediately when policy changes occur
   * to ensure the next capability initialization reflects the updated policy state.
   */
  private void disableSubAgentPreference() {
    // The preference is stored in the UI plugin's preference store, which uses InstanceScope internally
    IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("com.microsoft.copilot.eclipse.ui");
    if (prefs != null) {
      // Only update and flush if the current setting is true
      boolean currentValue = prefs.getBoolean(Constants.SUB_AGENT_ENABLED, false);
      if (currentValue) {
        prefs.putBoolean(Constants.SUB_AGENT_ENABLED, false);
        try {
          prefs.flush();
        } catch (BackingStoreException e) {
          CopilotCore.LOGGER.error("Failed to save subagent preference when disabled by policy", e);
        }
      }
    }
  }

  /**
   * Checks if the sub-agent is enabled.
   * Sub-agent is enabled only if both the user preference is enabled AND the organization policy allows it.
   *
   * @return true if the sub-agent is enabled, false otherwise.
   */
  public static boolean isSubAgentEnabled() {
    IEclipsePreferences uiPrefs = InstanceScope.INSTANCE.getNode("com.microsoft.copilot.eclipse.ui");
    if (uiPrefs != null) {
      return uiPrefs.getBoolean(Constants.SUB_AGENT_ENABLED, false);
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

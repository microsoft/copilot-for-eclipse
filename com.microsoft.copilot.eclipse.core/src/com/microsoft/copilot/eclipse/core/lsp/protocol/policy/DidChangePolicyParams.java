// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.policy;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for the 'policy/didChange' notification.
 */
public class DidChangePolicyParams {

  @SerializedName("mcp.contributionPoint.enabled")
  private boolean mcpContributionPointEnabled;

  @SerializedName("customAgent.enabled")
  private boolean customAgentEnabled = true;

  @SerializedName("agentMode.autoApproval.enabled")
  private boolean autoApprovalPolicyEnabled = true;

  public boolean isMcpContributionPointEnabled() {
    return mcpContributionPointEnabled;
  }

  public void setMcpContributionPointEnabled(boolean mcpContributionPointEnabled) {
    this.mcpContributionPointEnabled = mcpContributionPointEnabled;
  }

  public boolean isCustomAgentEnabled() {
    return customAgentEnabled;
  }

  public void setCustomAgentEnabled(boolean customAgentEnabled) {
    this.customAgentEnabled = customAgentEnabled;
  }

  public boolean isAutoApprovalPolicyEnabled() {
    return autoApprovalPolicyEnabled;
  }

  public void setAutoApprovalPolicyEnabled(boolean autoApprovalPolicyEnabled) {
    this.autoApprovalPolicyEnabled = autoApprovalPolicyEnabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mcpContributionPointEnabled, customAgentEnabled,
        autoApprovalPolicyEnabled);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DidChangePolicyParams other = (DidChangePolicyParams) obj;
    return mcpContributionPointEnabled == other.mcpContributionPointEnabled
        && customAgentEnabled == other.customAgentEnabled
        && autoApprovalPolicyEnabled == other.autoApprovalPolicyEnabled;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("mcpContributionPointEnabled", mcpContributionPointEnabled);
    builder.append("customAgentEnabled", customAgentEnabled);
    builder.append("autoApprovalPolicyEnabled", autoApprovalPolicyEnabled);
    return builder.toString();
  }
}

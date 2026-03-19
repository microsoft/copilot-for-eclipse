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

  @SerializedName("subAgent.enabled")
  private boolean subAgentEnabled = true;

  @SerializedName("customAgent.enabled")
  private boolean customAgentEnabled = true;

  public boolean isMcpContributionPointEnabled() {
    return mcpContributionPointEnabled;
  }

  public void setMcpContributionPointEnabled(boolean mcpContributionPointEnabled) {
    this.mcpContributionPointEnabled = mcpContributionPointEnabled;
  }

  public boolean isSubAgentEnabled() {
    return subAgentEnabled;
  }

  public void setSubAgentEnabled(boolean subAgentEnabled) {
    this.subAgentEnabled = subAgentEnabled;
  }

  public boolean isCustomAgentEnabled() {
    return customAgentEnabled;
  }

  public void setCustomAgentEnabled(boolean customAgentEnabled) {
    this.customAgentEnabled = customAgentEnabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mcpContributionPointEnabled, subAgentEnabled, customAgentEnabled);
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
        && subAgentEnabled == other.subAgentEnabled
        && customAgentEnabled == other.customAgentEnabled;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("mcpContributionPointEnabled", mcpContributionPointEnabled);
    builder.append("subAgentEnabled", subAgentEnabled);
    builder.append("customAgentEnabled", customAgentEnabled);
    return builder.toString();
  }
}

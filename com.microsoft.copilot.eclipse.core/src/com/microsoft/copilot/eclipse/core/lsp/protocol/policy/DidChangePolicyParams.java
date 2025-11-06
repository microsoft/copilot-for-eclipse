package com.microsoft.copilot.eclipse.core.lsp.protocol.policy;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Parameters for the 'policy/didChange' notification.
 */
public class DidChangePolicyParams {

  @SerializedName("mcp.contributionPoint.enabled")
  private boolean mcpContributionPointEnabled;

  @SerializedName("subAgent.enabled")
  private boolean subAgentEnabled = true;

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

  @Override
  public int hashCode() {
    return Objects.hash(mcpContributionPointEnabled, subAgentEnabled);
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
        && subAgentEnabled == other.subAgentEnabled;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("mcpContributionPointEnabled", mcpContributionPointEnabled);
    builder.add("subAgentEnabled", subAgentEnabled);
    return builder.toString();
  }
}

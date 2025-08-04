package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Parameters for the "copilot/didChangeFeatureFlags" notification.
 * This class represents changes to feature flags.
 */
public class DidChangeFeatureFlagsParams {
  /**
   * The token containing feature flags and some other configurations.
   * e.g.:
   * token={tid=xxx, ol=xxx,xxx, exp=1753241024, sku=copilot_enterprise_seat_multi_quota, 
   * proxy-ep=proxy.enterprise.githubcopilot.com, st=dotcom, ssc=1, chat=1, cit=1, malfil=1, 
   * editor_preview_features=1, mcp=1, ccr=1, rt=1, 8kp=1, ip=167.220.255.64, asn=AS3598}
   */
  @SerializedName("token")
  private Map<String, String> featureFlags;

  /**
   * Unused for now.
   * Leave them as Objects to allow for future flexibility.
   */
  private Object envelope;
  private Object activeExps;

  public Map<String, String> getFeatureFlags() {
    return featureFlags;
  }

  public void setFeatureFlags(Map<String, String> featureFlags) {
    this.featureFlags = featureFlags;
  }

  /**
   * Checks if the MCP is enabled.
   */
  public boolean isMcpEnabled() {
    boolean disabled = featureFlags != null && "0".equals(featureFlags.get("mcp"));
    return !disabled;
  }

  /**
   * Checks if the agent mode is enabled.
   */
  public boolean isAgentModeEnabled() {
    // Agent mode is by default enabled.
    // Disabled only when the feature flag "agent_mode" is set to "0".
    boolean disabled = featureFlags != null && "0".equals(featureFlags.get("agent_mode"));
    return !disabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(activeExps, featureFlags, envelope);
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
    DidChangeFeatureFlagsParams other = (DidChangeFeatureFlagsParams) obj;
    return Objects.equals(activeExps, other.activeExps) && Objects.equals(featureFlags, other.featureFlags)
        && Objects.equals(envelope, other.envelope);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("configurations", featureFlags);
    builder.add("envelope", envelope);
    builder.add("activeExps", activeExps);
    return builder.toString();
  }
}

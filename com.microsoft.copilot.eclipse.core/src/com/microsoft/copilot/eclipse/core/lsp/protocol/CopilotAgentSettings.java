package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Settings for the Copilot agent.
 */
public class CopilotAgentSettings {

  @SerializedName("maxToolCallingLoop")
  private int agentMaxRequests;

  public int getAgentMaxRequests() {
    return agentMaxRequests;
  }

  public void setAgentMaxRequests(int agentMaxRequests) {
    this.agentMaxRequests = agentMaxRequests;
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentMaxRequests);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CopilotAgentSettings)) {
      return false;
    }
    CopilotAgentSettings other = (CopilotAgentSettings) obj;
    return agentMaxRequests == other.agentMaxRequests;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("agentMaxRequests", agentMaxRequests);
    return builder.toString();
  }
}

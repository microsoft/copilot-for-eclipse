// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

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
  private boolean enableSkills;

  private String transcriptDirectory;

  public int getAgentMaxRequests() {
    return agentMaxRequests;
  }

  public void setAgentMaxRequests(int agentMaxRequests) {
    this.agentMaxRequests = agentMaxRequests;
  }

  public boolean isEnableSkills() {
    return enableSkills;
  }

  /**
   * Sets whether skills are enabled.
   *
   * @param enableSkills whether skills should be enabled
   * @return this settings instance, for chaining
   */
  public CopilotAgentSettings setEnableSkills(boolean enableSkills) {
    this.enableSkills = enableSkills;
    return this;
  }

  public String getTranscriptDirectory() {
    return transcriptDirectory;
  }

  public void setTranscriptDirectory(String transcriptDirectory) {
    this.transcriptDirectory = transcriptDirectory;
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentMaxRequests, enableSkills, transcriptDirectory);
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
    CopilotAgentSettings other = (CopilotAgentSettings) obj;
    return agentMaxRequests == other.agentMaxRequests && enableSkills == other.enableSkills
        && Objects.equals(transcriptDirectory, other.transcriptDirectory);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("agentMaxRequests", agentMaxRequests);
    builder.append("enableSkills", enableSkills);
    builder.append("transcriptDirectory", transcriptDirectory);
    return builder.toString();
  }

}

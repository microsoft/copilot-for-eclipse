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

  // Control if the editor handles all confirmation requests from CLS.
  @SerializedName("editorHandlesAllConfirmation")
  private boolean editorHandlesAllConfirmation;

  public int getAgentMaxRequests() {
    return agentMaxRequests;
  }

  public void setAgentMaxRequests(int agentMaxRequests) {
    this.agentMaxRequests = agentMaxRequests;
  }

  public boolean isEditorHandlesAllConfirmation() {
    return editorHandlesAllConfirmation;
  }

  public void setEditorHandlesAllConfirmation(boolean editorHandlesAllConfirmation) {
    this.editorHandlesAllConfirmation = editorHandlesAllConfirmation;
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentMaxRequests, editorHandlesAllConfirmation);
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
    return agentMaxRequests == other.agentMaxRequests
        && editorHandlesAllConfirmation == other.editorHandlesAllConfirmation;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("agentMaxRequests", agentMaxRequests);
    builder.append("editorHandlesAllConfirmation", editorHandlesAllConfirmation);
    return builder.toString();
  }
}

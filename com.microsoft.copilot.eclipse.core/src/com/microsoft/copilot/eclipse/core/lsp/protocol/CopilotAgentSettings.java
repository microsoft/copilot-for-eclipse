// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Settings for the Copilot agent.
 */
public class CopilotAgentSettings {

  @SerializedName("maxToolCallingLoop")
  private int agentMaxRequests;

  @SerializedName("autoApproveUnmatchedTerminal")
  private boolean autoApproveUnmatchedTerminal;

  // Tells CLS to always send confirmation requests to the editor
  @SerializedName("editorHandlesAllConfirmation")
  private boolean editorHandlesAllConfirmation = true;

  private ToolsSettings tools;

  /** Nested tools settings matching CLS agent.tools structure. */
  public static class ToolsSettings {
    private TerminalSettings terminal;

    /** Gets terminal settings, creating if needed. */
    public TerminalSettings getTerminal() {
      if (terminal == null) {
        terminal = new TerminalSettings();
      }
      return terminal;
    }
  }

  /** Terminal auto-approve rules: command/pattern → allow(true)/deny(false). */
  public static class TerminalSettings {
    private Map<String, Boolean> autoApprove;

    public Map<String, Boolean> getAutoApprove() {
      return autoApprove;
    }

    public void setAutoApprove(Map<String, Boolean> autoApprove) {
      this.autoApprove = autoApprove;
    }
  }

  public int getAgentMaxRequests() {
    return agentMaxRequests;
  }

  public void setAgentMaxRequests(int agentMaxRequests) {
    this.agentMaxRequests = agentMaxRequests;
  }

  public boolean isAutoApproveUnmatchedTerminal() {
    return autoApproveUnmatchedTerminal;
  }

  public void setAutoApproveUnmatchedTerminal(boolean autoApproveUnmatchedTerminal) {
    this.autoApproveUnmatchedTerminal = autoApproveUnmatchedTerminal;
  }

  /** Gets tools settings, creating if needed. */
  public ToolsSettings getTools() {
    if (tools == null) {
      tools = new ToolsSettings();
    }
    return tools;
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentMaxRequests,
        autoApproveUnmatchedTerminal, tools);
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
        && autoApproveUnmatchedTerminal == other.autoApproveUnmatchedTerminal
        && Objects.equals(tools, other.tools);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("agentMaxRequests", agentMaxRequests);
    builder.append("autoApproveUnmatchedTerminal", autoApproveUnmatchedTerminal);
    builder.append("tools", tools);
    return builder.toString();
  }
}

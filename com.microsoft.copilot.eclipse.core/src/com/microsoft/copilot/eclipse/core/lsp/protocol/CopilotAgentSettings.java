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
  private boolean enableSkills;
  private boolean autoCompress;

  private String transcriptDirectory;

  @SerializedName("autoApproveUnmatchedTerminal")
  private boolean autoApproveUnmatchedTerminal;

  @SerializedName("autoApproveUnmatchedFileOp")
  private boolean autoApproveUnmatchedFileOp;

  // Tells CLS to always send confirmation requests to the editor
  @SerializedName("editorHandlesAllConfirmation")
  private boolean editorHandlesAllConfirmation = true;

  private ToolsSettings tools;

  /** Nested tools settings matching CLS agent.tools structure. */
  public static class ToolsSettings {
    private TerminalSettings terminal;
    private EditSettings edit;

    /** Gets terminal settings, creating if needed. */
    public TerminalSettings getTerminal() {
      if (terminal == null) {
        terminal = new TerminalSettings();
      }
      return terminal;
    }

    /** Gets edit settings, creating if needed. */
    public EditSettings getEdit() {
      if (edit == null) {
        edit = new EditSettings();
      }
      return edit;
    }

    @Override
    public int hashCode() {
      return Objects.hash(terminal, edit);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      ToolsSettings other = (ToolsSettings) obj;
      return Objects.equals(terminal, other.terminal) && Objects.equals(edit, other.edit);
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
          .append("terminal", terminal)
          .append("edit", edit)
          .toString();
    }
  }

  /** Terminal auto-approve rules: command/pattern -> allow(true)/deny(false). */
  public static class TerminalSettings {
    private Map<String, Boolean> autoApprove;

    public Map<String, Boolean> getAutoApprove() {
      return autoApprove;
    }

    public void setAutoApprove(Map<String, Boolean> autoApprove) {
      this.autoApprove = autoApprove;
    }

    @Override
    public int hashCode() {
      return Objects.hash(autoApprove);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      return Objects.equals(autoApprove, ((TerminalSettings) obj).autoApprove);
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
          .append("autoApprove", autoApprove)
          .toString();
    }
  }

  /** Edit (file operation) auto-approve rules: pattern → allow(true)/deny(false). */
  public static class EditSettings {
    private Map<String, Boolean> autoApprove;

    public Map<String, Boolean> getAutoApprove() {
      return autoApprove;
    }

    public void setAutoApprove(Map<String, Boolean> autoApprove) {
      this.autoApprove = autoApprove;
    }

    @Override
    public int hashCode() {
      return Objects.hash(autoApprove);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      return Objects.equals(autoApprove, ((EditSettings) obj).autoApprove);
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this)
          .append("autoApprove", autoApprove)
          .toString();
    }
  }

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

  public boolean isAutoCompress() {
    return autoCompress;
  }

  public void setAutoCompress(boolean autoCompress) {
    this.autoCompress = autoCompress;
  }

  public void setTranscriptDirectory(String transcriptDirectory) {
    this.transcriptDirectory = transcriptDirectory;
  }

  public boolean isEditorHandlesAllConfirmation() {
    return editorHandlesAllConfirmation;
  }

  public boolean isAutoApproveUnmatchedTerminal() {
    return autoApproveUnmatchedTerminal;
  }

  public void setAutoApproveUnmatchedTerminal(boolean autoApproveUnmatchedTerminal) {
    this.autoApproveUnmatchedTerminal = autoApproveUnmatchedTerminal;
  }

  public boolean isAutoApproveUnmatchedFileOp() {
    return autoApproveUnmatchedFileOp;
  }

  public void setAutoApproveUnmatchedFileOp(boolean autoApproveUnmatchedFileOp) {
    this.autoApproveUnmatchedFileOp = autoApproveUnmatchedFileOp;
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
    return Objects.hash(agentMaxRequests, enableSkills, autoCompress, transcriptDirectory,
        editorHandlesAllConfirmation, autoApproveUnmatchedTerminal, autoApproveUnmatchedFileOp, tools);
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
        && autoCompress == other.autoCompress
        && Objects.equals(transcriptDirectory, other.transcriptDirectory)
        && editorHandlesAllConfirmation == other.editorHandlesAllConfirmation
        && autoApproveUnmatchedTerminal == other.autoApproveUnmatchedTerminal
        && autoApproveUnmatchedFileOp == other.autoApproveUnmatchedFileOp
        && Objects.equals(tools, other.tools);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("agentMaxRequests", agentMaxRequests);
    builder.append("enableSkills", enableSkills);
    builder.append("autoCompress", autoCompress);
    builder.append("transcriptDirectory", transcriptDirectory);
    builder.append("editorHandlesAllConfirmation", editorHandlesAllConfirmation);
    builder.append("autoApproveUnmatchedTerminal", autoApproveUnmatchedTerminal);
    builder.append("autoApproveUnmatchedFileOp", autoApproveUnmatchedFileOp);
    builder.append("tools", tools);
    return builder.toString();
  }

}

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Parameters for creating a conversation.
 */
public class ConversationTurnParams {
  @NonNull
  String workDoneToken;
  @NonNull
  String conversationId;
  @NonNull
  String message;
  ArrayList<FileReferenceParams> references;

  Boolean computeSuggestions;
  String workspaceFolder;
  String[] ignoredSkills;
  String model;

  /**
   * Creates a new ConversationTurnParams.
   */
  public ConversationTurnParams(String workDoneToken, String conversationId, String message) {
    this.workDoneToken = workDoneToken;
    this.conversationId = conversationId;
    this.message = message;
    this.references = new ArrayList<>();
  }

  /**
   * Adds file references to the conversation.
   */
  public void addFileRefs(List<IFile> files) {
    for (IFile file : files) {
      this.references.add(new FileReferenceParams(file));
    }
  }

  public String getWorkDoneToken() {
    return workDoneToken;
  }

  public String getConversationId() {
    return conversationId;
  }

  public String getMessage() {
    return message;
  }

  public Boolean getComputeSuggestions() {
    return computeSuggestions;
  }

  public String getWorkspaceFolder() {
    return workspaceFolder;
  }

  public String[] getIgnoredSkills() {
    return ignoredSkills;
  }

  public String getModel() {
    return model;
  }

  public void setWorkDoneToken(String workDoneToken) {
    this.workDoneToken = workDoneToken;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setComputeSuggestions(Boolean computeSuggestions) {
    this.computeSuggestions = computeSuggestions;
  }

  public void setWorkspaceFolder(String workspaceFolder) {
    this.workspaceFolder = workspaceFolder;
  }

  public void setIgnoredSkills(String[] ignoredSkills) {
    this.ignoredSkills = ignoredSkills;
  }

  public void setModel(String model) {
    this.model = model;
  }

  @Override
  public int hashCode() {
    return Objects.hash(workDoneToken, conversationId, message, computeSuggestions, workspaceFolder, ignoredSkills,
        model);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConversationTurnParams that = (ConversationTurnParams) o;
    return Objects.equals(workDoneToken, that.workDoneToken) && Objects.equals(conversationId, that.conversationId)
        && Objects.equals(message, that.message) && Objects.equals(computeSuggestions, that.computeSuggestions)
        && Objects.equals(workspaceFolder, that.workspaceFolder) && Arrays.equals(ignoredSkills, that.ignoredSkills)
        && Objects.equals(model, that.model);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("workDoneToken", workDoneToken);
    builder.add("conversationId", conversationId);
    builder.add("message", message);
    builder.add("computeSuggestions", computeSuggestions);
    builder.add("workspaceFolder", workspaceFolder);
    builder.add("ignoredSkills", Arrays.toString(ignoredSkills));
    builder.add("model", model);
    return builder.toString();
  }
}

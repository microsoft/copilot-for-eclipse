package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceFolder;
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
  List<FileReferenceParams> references;
  TextDocumentIdentifier textDocument;
  Boolean computeSuggestions;
  String workspaceFolder;
  List<WorkspaceFolder> workspaceFolders;
  String[] ignoredSkills;
  String model;

  String chatMode;

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

  public void setWorkDoneToken(String workDoneToken) {
    this.workDoneToken = workDoneToken;
  }

  public String getConversationId() {
    return conversationId;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<FileReferenceParams> getReferences() {
    return references;
  }

  public void setReferences(List<FileReferenceParams> references) {
    this.references = references;
  }

  public TextDocumentIdentifier getTextDocument() {
    return textDocument;
  }

  public void setTextDocument(TextDocumentIdentifier textDocument) {
    this.textDocument = textDocument;
  }

  public Boolean getComputeSuggestions() {
    return computeSuggestions;
  }

  public void setComputeSuggestions(Boolean computeSuggestions) {
    this.computeSuggestions = computeSuggestions;
  }

  public String getWorkspaceFolder() {
    return workspaceFolder;
  }

  public void setWorkspaceFolder(String workspaceFolder) {
    this.workspaceFolder = workspaceFolder;
  }

  public String[] getIgnoredSkills() {
    return ignoredSkills;
  }

  public void setIgnoredSkills(String[] ignoredSkills) {
    this.ignoredSkills = ignoredSkills;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getChatMode() {
    return chatMode;
  }

  public void setChatMode(String chatMode) {
    this.chatMode = chatMode;
  }

  public List<WorkspaceFolder> getWorkspaceFolders() {
    return workspaceFolders;
  }

  public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
    this.workspaceFolders = workspaceFolders;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(ignoredSkills);
    result = prime * result + Objects.hash(chatMode, computeSuggestions, conversationId, message, model, references,
        textDocument, workDoneToken, workspaceFolder, workspaceFolders);
    return result;
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
    ConversationTurnParams other = (ConversationTurnParams) obj;
    return Objects.equals(chatMode, other.chatMode) && Objects.equals(computeSuggestions, other.computeSuggestions)
        && Objects.equals(conversationId, other.conversationId) && Arrays.equals(ignoredSkills, other.ignoredSkills)
        && Objects.equals(message, other.message) && Objects.equals(model, other.model)
        && Objects.equals(references, other.references) && Objects.equals(textDocument, other.textDocument)
        && Objects.equals(workDoneToken, other.workDoneToken) && Objects.equals(workspaceFolder, other.workspaceFolder)
        && Objects.equals(workspaceFolders, other.workspaceFolders);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("workDoneToken", workDoneToken);
    builder.add("conversationId", conversationId);
    builder.add("message", message);
    builder.add("references", references);
    builder.add("textDocument", textDocument);
    builder.add("computeSuggestions", computeSuggestions);
    builder.add("workspaceFolder", workspaceFolder);
    builder.add("workspaceFolders", workspaceFolders);
    builder.add("ignoredSkills", Arrays.toString(ignoredSkills));
    builder.add("model", model);
    builder.add("chatMode", chatMode);
    return builder.toString();
  }
}

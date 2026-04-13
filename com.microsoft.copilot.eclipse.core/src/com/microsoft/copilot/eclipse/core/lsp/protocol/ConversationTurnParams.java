// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Parameters for creating a conversation.
 */
public class ConversationTurnParams {
  @NonNull
  private String workDoneToken;
  @NonNull
  private String conversationId;
  @NonNull
  private Either<String, List<ChatCompletionContentPart>> message; // String or ChatCompletionContentPart[]
  private List<ChatReference> references;
  private TextDocumentIdentifier textDocument;
  private Range selection;
  private boolean computeSuggestions;
  private String workspaceFolder;
  private List<WorkspaceFolder> workspaceFolders;
  private String[] ignoredSkills;
  private String model;
  private String modelProviderName;
  private String chatMode;
  private String customChatModeId;

  // TODO: remove needToolCallConfirmation when CLS fully supports it across all IDEs.
  private boolean needToolCallConfirmation;
  private String agentSlug;
  private List<TodoItem> todoList;

  /**
   * Creates a new ConversationTurnParams.
   */
  public ConversationTurnParams(String workDoneToken, String conversationId,
      Either<String, List<ChatCompletionContentPart>> message) {
    this.workDoneToken = workDoneToken;
    this.conversationId = conversationId;
    this.message = message;
    this.references = new ArrayList<>();
    this.computeSuggestions = true;
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

  public Either<String, List<ChatCompletionContentPart>> getMessage() {
    return message;
  }

  public void setMessage(Either<String, List<ChatCompletionContentPart>> message) {
    this.message = message;
  }

  public List<ChatReference> getReferences() {
    return references;
  }

  public void setReferences(List<ChatReference> references) {
    this.references = references;
  }

  public TextDocumentIdentifier getTextDocument() {
    return textDocument;
  }

  public void setTextDocument(TextDocumentIdentifier textDocument) {
    this.textDocument = textDocument;
  }

  public Range getSelection() {
    return selection;
  }

  public void setSelection(Range selection) {
    this.selection = selection;
  }

  public boolean isComputeSuggestions() {
    return computeSuggestions;
  }

  public void setComputeSuggestions(boolean computeSuggestions) {
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

  public String getCustomChatModeId() {
    return customChatModeId;
  }

  public void setCustomChatModeId(String customChatModeId) {
    this.customChatModeId = customChatModeId;
  }

  public List<WorkspaceFolder> getWorkspaceFolders() {
    return workspaceFolders;
  }

  public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
    this.workspaceFolders = workspaceFolders;
  }

  public boolean isNeedToolCallConfirmation() {
    return needToolCallConfirmation;
  }

  public void setNeedToolCallConfirmation(boolean needToolCallConfirmation) {
    this.needToolCallConfirmation = needToolCallConfirmation;
  }

  public String getModelProviderName() {
    return modelProviderName;
  }

  public void setModelProviderName(String modelProviderName) {
    this.modelProviderName = modelProviderName;
  }

  public String getAgentSlug() {
    return agentSlug;
  }

  public void setAgentSlug(String agentSlug) {
    this.agentSlug = agentSlug;
  }

  public List<TodoItem> getTodoList() {
    return todoList;
  }

  public void setTodoList(List<TodoItem> todoList) {
    this.todoList = todoList;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(ignoredSkills);
    result = prime * result
        + Objects.hash(agentSlug, chatMode, computeSuggestions, conversationId, customChatModeId, message, model,
            modelProviderName, needToolCallConfirmation, references, textDocument, todoList, workDoneToken,
            workspaceFolder, workspaceFolders);
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
    return Objects.equals(agentSlug, other.agentSlug) && Objects.equals(chatMode, other.chatMode)
        && computeSuggestions == other.computeSuggestions && Objects.equals(conversationId, other.conversationId)
        && Objects.equals(customChatModeId, other.customChatModeId)
        && Arrays.equals(ignoredSkills, other.ignoredSkills) && Objects.equals(message, other.message)
        && Objects.equals(model, other.model) && Objects.equals(modelProviderName, other.modelProviderName)
        && needToolCallConfirmation == other.needToolCallConfirmation && Objects.equals(references, other.references)
        && Objects.equals(textDocument, other.textDocument) && Objects.equals(todoList, other.todoList)
        && Objects.equals(workDoneToken, other.workDoneToken)
        && Objects.equals(workspaceFolder, other.workspaceFolder)
        && Objects.equals(workspaceFolders, other.workspaceFolders);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("workDoneToken", workDoneToken);
    builder.append("conversationId", conversationId);
    builder.append("message", message);
    builder.append("references", references);
    builder.append("textDocument", textDocument);
    builder.append("computeSuggestions", computeSuggestions);
    builder.append("workspaceFolder", workspaceFolder);
    builder.append("workspaceFolders", workspaceFolders);
    builder.append("ignoredSkills", Arrays.toString(ignoredSkills));
    builder.append("model", model);
    builder.append("chatMode", chatMode);
    builder.append("customChatModeId", customChatModeId);
    builder.append("needToolCallConfirmation", needToolCallConfirmation);
    builder.append("modelProviderName", modelProviderName);
    builder.append("agentSlug", agentSlug);
    builder.append("todoList", todoList);
    return builder.toString();
  }
}

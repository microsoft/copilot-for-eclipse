package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Parameters for creating a conversation.
 */
public class ConversationCreateParams {
  private String workDoneToken;
  private Turn[] turns;
  private ConversationCapabilities capabilities;
  private boolean computeSuggestions;
  private TextDocumentIdentifier textDocument;
  private List<ChatReference> references;
  private String source = "panel";
  private String workspaceFolder;
  private List<WorkspaceFolder> workspaceFolders;

  private String[] ignoredSkills;
  private String userLanguage;
  private String model;
  private String chatMode;

  // TODO: remove needToolCallConfirmation when CLS fully supports it across all IDEs.
  private boolean needToolCallConfirmation;

  /**
   * Creates a new ConversationCreateParams.
   */
  public ConversationCreateParams(Either<String, List<ChatCompletionContentPart>> prompt, String workDoneToken) {
    this.workDoneToken = workDoneToken;
    this.turns = new Turn[] { new Turn(prompt, null, null) };
    this.capabilities = new ConversationCapabilities();
    this.capabilities.setSkills(List.of(ConversationCapabilities.CURRENT_EDITOR_SKILL));
    this.computeSuggestions = true;
    this.references = new ArrayList<>();
    this.ignoredSkills = new String[0];
    this.userLanguage = "en";
  }

  public String getWorkDoneToken() {
    return workDoneToken;
  }

  public void setWorkDoneToken(String workDoneToken) {
    this.workDoneToken = workDoneToken;
  }

  public Turn[] getTurns() {
    return turns;
  }

  public void setTurns(Turn[] turns) {
    this.turns = turns;
  }

  public ConversationCapabilities getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(ConversationCapabilities capabilities) {
    this.capabilities = capabilities;
  }

  public boolean isComputeSuggestions() {
    return computeSuggestions;
  }

  public void setComputeSuggestions(boolean computeSuggestions) {
    this.computeSuggestions = computeSuggestions;
  }

  public TextDocumentIdentifier getTextDocument() {
    return textDocument;
  }

  public void setTextDocument(TextDocumentIdentifier textDocument) {
    this.textDocument = textDocument;
  }

  public List<ChatReference> getReferences() {
    return references;
  }

  public void setReferences(List<ChatReference> references) {
    this.references = references;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getWorkspaceFolder() {
    return workspaceFolder;
  }

  public void setWorkspaceFolder(String workspaceFolder) {
    this.workspaceFolder = workspaceFolder;
  }

  public List<WorkspaceFolder> getWorkspaceFolders() {
    return workspaceFolders;
  }

  public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
    this.workspaceFolders = workspaceFolders;
  }

  public String[] getIgnoredSkills() {
    return ignoredSkills;
  }

  public void setIgnoredSkills(String[] ignoredSkills) {
    this.ignoredSkills = ignoredSkills;
  }

  public String getUserLanguage() {
    return userLanguage;
  }

  public void setUserLanguage(String userLanguage) {
    this.userLanguage = userLanguage;
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

  public boolean isNeedToolCallConfirmation() {
    return needToolCallConfirmation;
  }

  public void setNeedToolCallConfirmation(boolean needToolCallConfirmation) {
    this.needToolCallConfirmation = needToolCallConfirmation;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(ignoredSkills);
    result = prime * result + Arrays.hashCode(turns);
    result = prime * result + Objects.hash(capabilities, chatMode, computeSuggestions, model, needToolCallConfirmation,
        references, source, textDocument, userLanguage, workDoneToken, workspaceFolder, workspaceFolders);
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
    ConversationCreateParams other = (ConversationCreateParams) obj;
    return Objects.equals(capabilities, other.capabilities) && Objects.equals(chatMode, other.chatMode)
        && computeSuggestions == other.computeSuggestions && Arrays.equals(ignoredSkills, other.ignoredSkills)
        && Objects.equals(model, other.model) && needToolCallConfirmation == other.needToolCallConfirmation
        && Objects.equals(references, other.references) && Objects.equals(source, other.source)
        && Objects.equals(textDocument, other.textDocument) && Arrays.equals(turns, other.turns)
        && Objects.equals(userLanguage, other.userLanguage) && Objects.equals(workDoneToken, other.workDoneToken)
        && Objects.equals(workspaceFolder, other.workspaceFolder)
        && Objects.equals(workspaceFolders, other.workspaceFolders);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("workDoneToken", workDoneToken);
    builder.add("turns", Arrays.toString(turns));
    builder.add("capabilities", capabilities);
    builder.add("computeSuggestions", computeSuggestions);
    builder.add("textDocument", textDocument);
    builder.add("references", references);
    builder.add("source", source);
    builder.add("workspaceFolder", workspaceFolder);
    builder.add("workspaceFolders", workspaceFolders);
    builder.add("ignoredSkills", Arrays.toString(ignoredSkills));
    builder.add("userLanguage", userLanguage);
    builder.add("model", model);
    builder.add("chatMode", chatMode);
    builder.add("needToolCallConfirmation", needToolCallConfirmation);
    return builder.toString();
  }
}

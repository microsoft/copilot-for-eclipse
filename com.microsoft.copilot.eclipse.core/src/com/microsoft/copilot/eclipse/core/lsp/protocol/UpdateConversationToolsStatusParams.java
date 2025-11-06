package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Parameters for updating conversation tools status.
 */
public class UpdateConversationToolsStatusParams {
  private String chatModeKind;
  private String customChatModeId;
  private List<WorkspaceFolder> workspaceFolders;
  private List<ConversationToolStatus> tools;

  /**
   * Creates a new UpdateConversationToolsStatusParams.
   */
  public UpdateConversationToolsStatusParams() {
  }

  public String getChatModeKind() {
    return chatModeKind;
  }

  public void setChatModeKind(String chatModeKind) {
    this.chatModeKind = chatModeKind;
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

  public List<ConversationToolStatus> getTools() {
    return tools;
  }

  public void setTools(List<ConversationToolStatus> tools) {
    this.tools = tools;
  }

  @Override
  public int hashCode() {
    return Objects.hash(chatModeKind, customChatModeId, workspaceFolders, tools);
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
    UpdateConversationToolsStatusParams other = (UpdateConversationToolsStatusParams) obj;
    return Objects.equals(chatModeKind, other.chatModeKind) && Objects.equals(customChatModeId, other.customChatModeId)
        && Objects.equals(workspaceFolders, other.workspaceFolders) && Objects.equals(tools, other.tools);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("chatModeKind", chatModeKind);
    builder.add("customChatModeId", customChatModeId);
    builder.add("workspaceFolders", workspaceFolders);
    builder.add("tools", tools);
    return builder.toString();
  }
}

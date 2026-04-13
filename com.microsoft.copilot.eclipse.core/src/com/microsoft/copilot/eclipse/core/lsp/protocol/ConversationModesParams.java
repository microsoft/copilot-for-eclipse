package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.WorkspaceFolder;

/**
 * Parameters for the conversation/modes request.
 */
public class ConversationModesParams {
  private List<WorkspaceFolder> workspaceFolders;

  /**
   * Creates a new ConversationModesParams with workspace folders.
   *
   * @param workspaceFolders the workspace folders
   */
  public ConversationModesParams(List<WorkspaceFolder> workspaceFolders) {
    this.workspaceFolders = workspaceFolders;
  }

  public List<WorkspaceFolder> getWorkspaceFolders() {
    return workspaceFolders;
  }

  public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
    this.workspaceFolders = workspaceFolders;
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceFolders);
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
    ConversationModesParams other = (ConversationModesParams) obj;
    return Objects.equals(workspaceFolders, other.workspaceFolders);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("workspaceFolders", workspaceFolders);
    return builder.toString();
  }
}

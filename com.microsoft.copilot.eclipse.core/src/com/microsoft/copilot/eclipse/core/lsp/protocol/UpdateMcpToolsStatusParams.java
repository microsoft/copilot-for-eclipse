package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.WorkspaceFolder;

import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsStatusCollection;

/**
 * Parameters for updating the status of the MCP tools.
 */
public class UpdateMcpToolsStatusParams {

  private List<McpServerToolsStatusCollection> servers;

  private String customChatModeId;

  private List<WorkspaceFolder> workspaceFolders;

  public List<McpServerToolsStatusCollection> getServers() {
    return servers;
  }

  public void setServers(List<McpServerToolsStatusCollection> servers) {
    this.servers = servers;
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

  @Override
  public int hashCode() {
    return Objects.hash(servers, customChatModeId, workspaceFolders);
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
    UpdateMcpToolsStatusParams other = (UpdateMcpToolsStatusParams) obj;
    return Objects.equals(servers, other.servers) && Objects.equals(customChatModeId, other.customChatModeId)
        && Objects.equals(workspaceFolders, other.workspaceFolders);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("servers", servers);
    builder.append("customChatModeId", customChatModeId);
    builder.append("workspaceFolders", workspaceFolders);
    return builder.toString();
  }
}
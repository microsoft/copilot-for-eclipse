// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Parameters for searching GitHub Pull Requests.
 */
public class SearchPrParams {

  /**
   * The GitHub search query string with template support for ${owner}, ${repository}, and ${user}.
   */
  @NonNull
  private String query;

  /**
   * Optional: The workspace folder for current project.
   */
  private String workspaceFolder;

  /**
   * Optional: Array of workspace folder URIs for multi-root workspaces The long-term goal is to deprecate the
   * {@link workspaceFolder} property in favor of this one.
   */
  private List<WorkspaceFolder> workspaceFolders;

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
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

  @Override
  public int hashCode() {
    return Objects.hash(query, workspaceFolder, workspaceFolders);
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
    SearchPrParams other = (SearchPrParams) obj;
    return Objects.equals(query, other.query) && Objects.equals(workspaceFolder, other.workspaceFolder)
        && Objects.equals(workspaceFolders, other.workspaceFolders);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("query", query);
    builder.append("workspaceFolder", workspaceFolder);
    builder.append("workspaceFolders", workspaceFolders);
    return builder.toString();
  }

}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;

import org.eclipse.lsp4j.WorkspaceFolder;

/**
 * Generic parameters for language-server requests scoped to a set of workspace folders.
 *
 * @param workspaceFolders the workspace folders to scan
 */
public record WorkspaceFoldersParams(List<WorkspaceFolder> workspaceFolders) {
  /** Compact constructor that defensively copies the folders, defaulting {@code null} to empty. */
  public WorkspaceFoldersParams {
    workspaceFolders = workspaceFolders != null ? List.copyOf(workspaceFolders) : List.of();
  }
}

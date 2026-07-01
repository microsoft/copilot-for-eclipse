// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.WorkspaceFolder;

/**
 * Generic parameters for requests that scan a set of workspace folders, such as the
 * {@code copilot/custom*} list requests.
 *
 * @param workspaceFolders the workspace folders to scan
 */
public record WorkspaceFoldersParams(List<WorkspaceFolder> workspaceFolders) {
  /** Compact constructor that defaults {@code null} workspace folders to an empty list. */
  public WorkspaceFoldersParams {
    workspaceFolders = workspaceFolders != null ? workspaceFolders : Collections.emptyList();
  }
}

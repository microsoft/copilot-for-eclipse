// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.WorkspaceFolder;

/**
 * Parameters for the {@code conversation/templates} request.
 *
 * @param workspaceFolders the workspace folders used to discover workspace-specific prompt files and skills
 */
public record ConversationTemplatesParams(List<WorkspaceFolder> workspaceFolders) {
  /** Compact constructor that defaults {@code null} workspace folders to an empty list. */
  public ConversationTemplatesParams {
    workspaceFolders = workspaceFolders != null ? workspaceFolders : Collections.emptyList();
  }
}

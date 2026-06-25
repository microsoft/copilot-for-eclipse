// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat.service;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4j.Range;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ReadFileResult;

/**
 * Interface for managing referenced files in the Copilot chat.
 */
public interface IReferencedFileService {
  /**
   * Get the current file being referenced in the Copilot chat.
   */
  IFile getCurrentFile();

  /**
   * Get the URI for the current editor when it is not backed by a workspace file.
   */
  @Nullable
  default String getCurrentEditorUri() {
    return null;
  }

  /**
   * Read the current editor contents for a URI returned by {@link #getCurrentEditorUri()}.
   */
  @Nullable
  default ReadFileResult readCurrentEditor(String uri) {
    return null;
  }

  /**
   * Get the referenced files that is attached by user.
   */
  List<IResource> getReferencedFiles();

  /**
   * Get the current selection range in the active editor, or null if there is no selection.
   * The selection represents the user-selected lines in the editor.
   *
   * @return the selection range, or null if no selection exists
   */
  @Nullable
  Range getCurrentSelection();

}

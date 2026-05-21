// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * Interface for handling working set actions.
 */
public interface WorkingSetHandler {
  /**
   * Handles the action of keeping changes to a file.
   *
   * @param file the file to keep changes for
   */
  void onKeepChange(IFile file) throws IOException, CoreException;

  /**
   * Handles the action of keeping changes to a local file.
   *
   * @param file the local file to keep changes for
   */
  void onKeepChange(Path file) throws IOException, CoreException;

  /**
   * Handles the action of undoing changes to a file.
   *
   * @param file the file to undo changes for
   *
   * @throws CoreException if an error occurs during the undo operation, such as a failure to delete a file
   * @throws IOException if an error occurs while writing to the file
   */
  void onUndoChange(IFile file) throws CoreException, IOException;

  /**
   * Handles the action of undoing changes to a local file.
   *
   * @param file the local file to undo changes for
   *
   * @throws CoreException if an error occurs during the undo operation, such as a failure to delete a file
   * @throws IOException if an error occurs while writing to the file
   */
  void onUndoChange(Path file) throws CoreException, IOException;

  /**
   * Handles the action of viewing the diff of a file.
   *
   * @param file the file to view the diff for
   */
  void onViewDiff(IFile file);

  /**
   * Handles the action of viewing the diff of a local file.
   *
   * @param file the local file to view the diff for
   */
  void onViewDiff(Path file);

  /**
   * Handles the action of click done button to resolve all changes.
   */
  void onResolveAllChanges();
}

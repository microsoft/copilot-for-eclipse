package com.microsoft.copilot.eclipse.ui.chat.tools;

import org.eclipse.core.resources.IFile;

/**
 * Interface for handling file change summary actions.
 */
public interface FileChangeSummaryHandler {
  /**
   * Handles the action of keeping changes to a file.
   *
   * @param file the file to keep changes for
   */
  void onKeepChange(IFile file);

  /**
   * Handles the action of keeping all changes to files.
   */
  void onKeepAllChanges();

  /**
   * Handles the action of undoing changes to a file.
   *
   * @param file the file to undo changes for
   */
  void onUndoChange(IFile file);

  /**
   * Handles the action of undoing all changes to files.
   */
  void onUndoAllChanges();

  /**
   * Handles the action of removing a file.
   *
   * @param file the file to remove
   */
  void onRemoveFile(IFile file);

  /**
   * Handles the action of viewing the diff of a file.
   *
   * @param file the file to view the diff for
   */
  void onViewDiff(IFile file);

  /**
   * Handles the action of click done button to resolve all changes.
   */
  void onAllChangesResolved();
}

package com.microsoft.copilot.eclipse.core.chat.service;

import java.util.List;

import org.eclipse.core.resources.IFile;

/**
 * Interface for managing referenced files in the Copilot chat.
 */
public interface IReferencedFileService {
  /**
   * Get the current file being referenced in the Copilot chat.
   */
  IFile getCurrentFile();

  /**
   * Get the referenced files that is attached by user.
   */
  List<IFile> getReferencedFiles();

}

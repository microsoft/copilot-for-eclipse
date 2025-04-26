package com.microsoft.copilot.eclipse.core.chat.service;

import org.eclipse.core.resources.IFile;

/**
 * Interface for managing referenced files in the Copilot chat.
 */
public interface IReferencedFileService {
  /**
   * Get the current file being referenced in the Copilot chat.
   */
  IFile getCurrentFile();

}

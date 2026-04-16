// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Interface for a chat reference, which can be a file or directory.
 */
public interface ChatReference {

  /**
   * Enum representing the type of reference.
   */
  public enum ReferenceType {
    file, directory;
  }
}

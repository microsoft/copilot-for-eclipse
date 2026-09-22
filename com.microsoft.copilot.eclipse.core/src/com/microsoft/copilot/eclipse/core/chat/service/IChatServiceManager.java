// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat.service;

/**
 * Interface for managing chat services in the Copilot chat.
 */
public interface IChatServiceManager {

  /**
   * Get the referenced file service.
   *
   * @return the referenced file service.
   */
  IReferencedFileService getReferencedFileService();

  /**
   * Get the MCP config service.
   *
   * @return the MCP config service.
   */
  IMcpConfigService getMcpConfigService();

  /**
   * Get the customization file service tracking skill/prompt/instruction/agent file locations.
   *
   * @return the customization file service.
   */
  ICustomizationFileService getCustomizationFileService();
}

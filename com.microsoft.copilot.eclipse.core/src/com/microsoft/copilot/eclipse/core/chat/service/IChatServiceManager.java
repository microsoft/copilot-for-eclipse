package com.microsoft.copilot.eclipse.core.chat.service;

/**
 * Interface for managing chat services in the Copilot chat.
 */
public interface IChatServiceManager {

  /**
   * Get the referenced file service.
   */
  IReferencedFileService getReferencedFileService();

  /**
   * Get the MCP config service.
   */
  IMcpConfigService getMcpConfigService();
}

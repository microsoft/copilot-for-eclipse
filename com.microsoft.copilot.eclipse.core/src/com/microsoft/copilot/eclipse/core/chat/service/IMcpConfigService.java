package com.microsoft.copilot.eclipse.core.chat.service;

import com.microsoft.copilot.eclipse.core.lsp.mcp.McpOauthRequest;

/**
 * Interface for the MCP config service.
 * This service handles the OAuth confirmation process.
 */
public interface IMcpConfigService {
  /**
   * Handles the OAuth confirmation request.
   */
  boolean mcpOauth(McpOauthRequest request);
}

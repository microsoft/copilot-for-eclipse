// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat.service;

import java.util.Map;

import com.microsoft.copilot.eclipse.core.lsp.mcp.McpOauthRequest;

/**
 * Interface for the MCP config service.
 * This service handles the Dynamic OAuth process for MCP servers.
 */
public interface IMcpConfigService {
  /**
   * Handles the Dynamic OAuth request from MCP servers.
   */
  Map<String, String> mcpOauth(McpOauthRequest request);
}

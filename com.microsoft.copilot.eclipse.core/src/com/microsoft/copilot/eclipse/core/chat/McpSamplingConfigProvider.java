// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import com.microsoft.copilot.eclipse.core.lsp.protocol.McpSamplingConfig;

/**
 * Provides the persisted MCP sampling (inference) approval preferences for a server, so that the
 * {@code copilot/readMcpSamplingConfig} language server request reflects the user's actual
 * previously-cached decisions instead of always requiring re-confirmation.
 */
public interface McpSamplingConfigProvider {

  /**
   * Reads the sampling preferences for the given MCP server.
   *
   * @param serverName the MCP server name, may be {@code null}
   * @return the persisted sampling config for the server
   */
  McpSamplingConfig getMcpSamplingConfig(String serverName);
}

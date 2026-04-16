// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for getting a specific server from the MCP Registry.
 *
 * @param baseUrl    The base URL for the server API ({baseApiUrl}/{apiVersion}/servers).
 * @param serverName The name of the server to retrieve.
 * @param version    The version of the server.
 */
public record GetServerParams(String baseUrl, String serverName, String version) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("baseUrl", baseUrl);
    builder.append("serverName", serverName);
    builder.append("version", version);
    return builder.toString();
  }
}
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A list of MCP servers from the MCP Registry.
 *
 * @param servers  The list of server responses.
 * @param metadata The pagination metadata.
 */
public record ServerList(List<ServerResponse> servers, Metadata metadata) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("servers", servers);
    builder.append("metadata", metadata);
    return builder.toString();
  }
}

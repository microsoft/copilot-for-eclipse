package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

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
    builder.add("servers", servers);
    builder.add("metadata", metadata);
    return builder.toString();
  }
}

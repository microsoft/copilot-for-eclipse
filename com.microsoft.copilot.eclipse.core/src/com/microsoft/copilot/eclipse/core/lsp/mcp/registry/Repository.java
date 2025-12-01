package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Information about a repository in the MCP Registry.
 *
 * @param url       The URL of the repository.
 * @param source    The source of the repository.
 * @param id        The unique identifier of the repository.
 * @param subfolder Optional relative path from repository root to the server location within a monorepo structure.
 */
public record Repository(String url, String source, String id, String subfolder) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("url", url);
    builder.add("source", source);
    builder.add("id", id);
    builder.add("subfolder", subfolder);
    return builder.toString();
  }
}

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import org.apache.commons.lang3.builder.ToStringBuilder;

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
    builder.append("url", url);
    builder.append("source", source);
    builder.append("id", id);
    builder.append("subfolder", subfolder);
    return builder.toString();
  }
}

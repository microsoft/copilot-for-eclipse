package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Official metadata about the server.
 *
 * @param status      The status of the server.
 * @param publishedAt The timestamp when the server was published.
 * @param updatedAt   The timestamp when the server was last updated.
 * @param isLatest    Whether this is the latest version of the server.
 */
public record OfficialMeta(ServerStatus status, String publishedAt, String updatedAt, boolean isLatest) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("status", status);
    builder.add("publishedAt", publishedAt);
    builder.add("updatedAt", updatedAt);
    builder.add("isLatest", isLatest);
    return builder.toString();
  }

  /**
   * Enum for server status.
   */
  public enum ServerStatus {
    active, deprecated, deleted
  }
}
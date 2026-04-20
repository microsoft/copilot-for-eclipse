// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import org.apache.commons.lang3.builder.ToStringBuilder;

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
    builder.append("status", status);
    builder.append("publishedAt", publishedAt);
    builder.append("updatedAt", updatedAt);
    builder.append("isLatest", isLatest);
    return builder.toString();
  }

  /**
   * Enum for server status.
   */
  public enum ServerStatus {
    active, deprecated, deleted
  }
}
package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Pagination metadata for MCP Registry responses.
 *
 * @param nextCursor The cursor for the next page of results, or null if there are no more results.
 * @param count      The total number of servers available.
 */
public record Metadata(String nextCursor, @SerializedName("count") int count) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("nextCursor", nextCursor);
    builder.append("count", count);
    return builder.toString();
  }
}

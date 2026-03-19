package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Information about a remote server configuration in the MCP Registry.
 *
 * @param transportType The transport type (e.g., STREAMABLE, SSE).
 * @param url           The URL of the remote server.
 * @param headers       The headers to include in requests to the remote server.
 */
public record Remote(
    @SerializedName("type") String transportType,
    String url,
    List<KeyValueInput> headers) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("transportType", transportType);
    builder.append("url", url);
    builder.append("headers", headers);
    return builder.toString();
  }
}

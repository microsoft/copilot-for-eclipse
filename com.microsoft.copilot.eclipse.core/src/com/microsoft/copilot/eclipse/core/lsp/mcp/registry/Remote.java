package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

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
    builder.add("transportType", transportType);
    builder.add("url", url);
    builder.add("headers", headers);
    return builder.toString();
  }
}

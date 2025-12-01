package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import com.google.gson.annotations.SerializedName;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * The server response from the MCP Registry.
 *
 * @param serverDetail The detailed information about the server.
 * @param meta         The metadata about the server response.
 */
public record ServerResponse(
    @SerializedName("server") ServerDetail serverDetail,
    @SerializedName("_meta") ServerResponseMeta meta) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("serverDetail", serverDetail);
    builder.add("meta", meta);
    return builder.toString();
  }

  /**
   * Gets the server detail.
   *
   * @return The server detail.
   */
  public ServerDetail getDetail() {
    return serverDetail;
  }
}

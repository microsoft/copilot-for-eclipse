package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import com.google.gson.annotations.SerializedName;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Metadata about the server response from the MCP Registry.
 *
 * @param official The official metadata about the server.
 */
public record ServerResponseMeta(
    @SerializedName("io.modelcontextprotocol.registry/official") OfficialMeta official) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("official", official);
    return builder.toString();
  }
}
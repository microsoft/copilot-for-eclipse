package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

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
    builder.append("official", official);
    return builder.toString();
  }
}
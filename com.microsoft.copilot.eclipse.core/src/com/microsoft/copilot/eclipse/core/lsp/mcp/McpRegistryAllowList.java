package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result type for the getMcpRegistryAllowlist method. Contains the complete list of MCP registries that are allowed for
 * the current user or organization, along with their access permissions and ownership details.
 */
public class McpRegistryAllowList {
  /**
   * List of allowed MCP registry entries. Each entry represents a registry that the user/organization has access to,
   * including the registry URL, access level, and owner information.
   */
  @SerializedName("mcp_registries")
  public List<McpRegistryEntry> mcpRegistries;

  public List<McpRegistryEntry> getMcpRegistries() {
    return mcpRegistries;
  }

  public void setMcpRegistries(List<McpRegistryEntry> mcpRegistries) {
    this.mcpRegistries = mcpRegistries;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mcpRegistries);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof McpRegistryAllowList)) {
      return false;
    }
    McpRegistryAllowList other = (McpRegistryAllowList) obj;
    return Objects.equals(mcpRegistries, other.mcpRegistries);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("mcpRegistries", mcpRegistries);
    return builder.toString();
  }

}

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsCollection;

/**
 * Parameters for the mcp server tools.
 */
public class OnChangeMcpServerToolsParams {

  private List<McpServerToolsCollection> servers;

  public List<McpServerToolsCollection> getServers() {
    return servers;
  }

  public void setServers(List<McpServerToolsCollection> servers) {
    this.servers = servers;
  }

  @Override
  public int hashCode() {
    return Objects.hash(servers);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    OnChangeMcpServerToolsParams other = (OnChangeMcpServerToolsParams) obj;
    return Objects.equals(servers, other.servers);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("servers", servers);
    return builder.toString();
  }
}
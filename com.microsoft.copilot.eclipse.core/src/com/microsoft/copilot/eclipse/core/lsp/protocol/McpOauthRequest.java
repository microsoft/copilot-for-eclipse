package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents a request for OAuth authentication in the MCP.
 */
public class McpOauthRequest {
  
  private String mcpServer;
  
  private String authLabel;
  
  public String getMcpServer() {
    return mcpServer;
  }
  
  public void setMcpServer(String mcpServer) {
    this.mcpServer = mcpServer;
  }
  
  public String getAuthLabel() {
    return authLabel;
  }
  
  public void setAuthLabel(String authLabel) {
    this.authLabel = authLabel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(authLabel, mcpServer);
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
    McpOauthRequest other = (McpOauthRequest) obj;
    return Objects.equals(authLabel, other.authLabel) && Objects.equals(mcpServer, other.mcpServer);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("mcpServer", mcpServer);
    builder.add("authLabel", authLabel);
    return builder.toString();
  }
}

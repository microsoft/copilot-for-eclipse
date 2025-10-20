package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents a response to an OAuth confirmation request.
 */
public class McpOauthResponse {
  private Boolean confirm;

  /**
   * Constructor.
   */
  public McpOauthResponse(Boolean confirm) {
    this.confirm = confirm;
  }
  
  public Boolean getConfirm() {
    return confirm;
  }

  public void setConfirm(Boolean confirm) {
    this.confirm = confirm;
  }

  @Override
  public int hashCode() {
    return Objects.hash(confirm);
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
    McpOauthResponse other = (McpOauthResponse) obj;
    return Objects.equals(confirm, other.confirm);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("confirm", confirm);
    return builder.toString();
  }
  
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Gets the MCP tools with their status. Referenced by McpServerToolsStatusCollection, which is the request param of the
 * endpoint mcp/updateToolsStatus.
 */
public class McpToolsStatusCollection {

  private String name;

  private String status;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, status);
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
    McpToolsStatusCollection other = (McpToolsStatusCollection) obj;
    return Objects.equals(name, other.name) && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name);
    builder.append("status", status);
    return builder.toString();
  }

}

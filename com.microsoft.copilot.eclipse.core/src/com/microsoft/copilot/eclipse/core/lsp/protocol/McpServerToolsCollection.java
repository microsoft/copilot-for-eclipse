package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * This class is used to provide a collection of tools that can be used by a language model. Referenced by: 1.The
 * notification from the endpoint copilot/mcpTools. 2.The response from the endpoint mcp/updateToolsStatus.
 */
public class McpServerToolsCollection {
  private String name;

  private McpServerStatus status;

  private List<McpToolInformation> tools;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public McpServerStatus getStatus() {
    return status;
  }

  public void setStatus(McpServerStatus status) {
    this.status = status;
  }

  public List<McpToolInformation> getTools() {
    return tools;
  }

  public void setTools(List<McpToolInformation> tools) {
    this.tools = tools;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, status, tools);
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
    McpServerToolsCollection other = (McpServerToolsCollection) obj;
    return Objects.equals(name, other.name) && status == other.status && Objects.equals(tools, other.tools);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("name", name);
    builder.add("status", status);
    builder.add("tools", tools);
    return builder.toString();
  }
}
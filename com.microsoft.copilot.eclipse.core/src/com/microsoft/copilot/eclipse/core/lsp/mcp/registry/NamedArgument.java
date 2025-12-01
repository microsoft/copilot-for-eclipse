package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;


/**
 * A named argument for an MCP server.
 */
public class NamedArgument extends Argument {
  private String name;
  private boolean isRepeated;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean getIsRepeated() {
    return isRepeated;
  }

  public void setIsRepeated(boolean isRepeated) {
    this.isRepeated = isRepeated;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(isRepeated, name);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    NamedArgument other = (NamedArgument) obj;
    return Objects.equals(isRepeated, other.isRepeated) && Objects.equals(name, other.name);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("type", getType());
    builder.add("name", name);
    builder.add("isRepeated", isRepeated);
    return builder.toString();
  }
}

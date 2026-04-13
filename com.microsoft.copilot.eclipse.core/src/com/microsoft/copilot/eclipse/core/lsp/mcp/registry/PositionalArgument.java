package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A positional argument for an MCP server.
 */
public class PositionalArgument extends Argument {
  private String valueHint;
  private boolean isRepeated;

  public String getValueHint() {
    return valueHint;
  }

  public void setValueHint(String valueHint) {
    this.valueHint = valueHint;
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
    result = prime * result + Objects.hash(isRepeated, valueHint);
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
    PositionalArgument other = (PositionalArgument) obj;
    return Objects.equals(isRepeated, other.isRepeated) && Objects.equals(valueHint, other.valueHint);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("type", getType());
    builder.append("valueHint", valueHint);
    builder.append("isRepeated", isRepeated);
    return builder.toString();
  }
}

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.Objects;

import com.google.gson.annotations.JsonAdapter;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Base class for different types of arguments in the MCP Registry API. Can be extended by PositionalArgument or
 * NamedArgument, or used directly for polymorphic deserialization.
 */
@JsonAdapter(ArgumentTypeAdapter.class)
public class Argument extends InputWithVariables {
  protected String type;

  /**
   * Get the type of this argument.
   *
   * @return "positional" for PositionalArgument, "named" for NamedArgument
   */
  public String getType() {
    return type;
  }

  /**
   * Set the type of this argument.
   *
   * @param type The type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(type);
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
    Argument other = (Argument) obj;
    return Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("type", type);
    return builder.toString();
  }

}

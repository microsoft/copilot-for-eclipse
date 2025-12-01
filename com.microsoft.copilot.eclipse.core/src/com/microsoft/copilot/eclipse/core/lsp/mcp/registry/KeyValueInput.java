package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents a key-value input parameter for configuring MCP servers.
 */
public class KeyValueInput extends InputWithVariables {
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(name);
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
    KeyValueInput other = (KeyValueInput) obj;
    return Objects.equals(name, other.name);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("name", name);
    builder.add("getVariables()", getVariables());
    builder.add("toString()", super.toString());
    builder.add("getDescription()", getDescription());
    builder.add("getIsRequired()", getIsRequired());
    builder.add("getFormat()", getFormat());
    builder.add("getValue()", getValue());
    builder.add("getIsSecret()", getIsSecret());
    builder.add("getDefaultValue()", getDefaultValue());
    builder.add("getChoices()", getChoices());
    builder.add("getClass()", getClass());
    return builder.toString();
  }

}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

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
    builder.append("name", name);
    builder.append("getVariables()", getVariables());
    builder.append("toString()", super.toString());
    builder.append("getDescription()", getDescription());
    builder.append("getIsRequired()", getIsRequired());
    builder.append("getFormat()", getFormat());
    builder.append("getValue()", getValue());
    builder.append("getIsSecret()", getIsSecret());
    builder.append("getDefaultValue()", getDefaultValue());
    builder.append("getChoices()", getChoices());
    builder.append("getClass()", getClass());
    return builder.toString();
  }

}

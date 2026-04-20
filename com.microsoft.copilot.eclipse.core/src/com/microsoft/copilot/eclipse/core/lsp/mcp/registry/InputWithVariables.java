// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents an input parameter that can contain nested variables for an MCP package or runtime. Extends the Input
 * class to include a map of variable inputs.
 */
public class InputWithVariables extends Input {
  private Map<String, Input> variables;

  public Map<String, Input> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, Input> variables) {
    this.variables = variables;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(variables);
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
    InputWithVariables other = (InputWithVariables) obj;
    return Objects.equals(variables, other.variables);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("variables", variables);
    return builder.toString();
  }

}

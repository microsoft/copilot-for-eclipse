package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents an input parameter that can contain nested variables for an MCP package or runtime. Extends the Input
 * class to include a map of variable inputs.
 */
public class InputWithVariables extends Input {
  private Map<String, Input> variables;

  /**
   * Parameterized constructor.
   *
   * @param description Description of the input
   * @param isRequired Whether the input is required
   * @param format Format of the input
   * @param value Value of the input
   * @param isSecret Whether the input is secret
   * @param defaultValue Default value of the input
   * @param choices List of valid choices for the input
   * @param variables Map of variable inputs
   */
  public InputWithVariables(String description, Boolean isRequired, String format, String value, Boolean isSecret,
      String defaultValue, List<String> choices, Map<String, Input> variables) {
    super(description, isRequired, format, value, isSecret, defaultValue, choices);
    this.variables = variables;
  }

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

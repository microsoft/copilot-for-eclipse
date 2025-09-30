package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a key-value input parameter for configuring MCP servers.
 */
public class KeyValueInput extends InputWithVariables {
  private String name;

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
   * @param name Name of the key-value input
   */
  public KeyValueInput(String description, Boolean isRequired, String format, String value, Boolean isSecret,
      String defaultValue, List<String> choices, Map<String, Input> variables, String name) {
    super(description, isRequired, format, value, isSecret, defaultValue, choices, variables);
    this.name = name;
  }

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

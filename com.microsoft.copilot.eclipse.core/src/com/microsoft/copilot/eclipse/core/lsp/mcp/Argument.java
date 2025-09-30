package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.JsonAdapter;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Base class for different types of arguments in the MCP Registry API. Can be extended by PositionalArgument or
 * NamedArgument, or used directly for polymorphic deserialization.
 */
@JsonAdapter(ArgumentTypeAdapter.class)
public class Argument extends InputWithVariables {
  protected String type;

  /**
   * Parameterized constructor.
   *
   * @param description Description of the argument
   * @param isRequired Whether the argument is required
   * @param format Format of the argument
   * @param value Value of the argument
   * @param isSecret Whether the argument is secret
   * @param defaultValue Default value of the argument
   * @param choices List of valid choices for the argument
   * @param variables Map of variable inputs associated with the argument
   * @param type Type of the argument ("positional" or "named")
   */
  public Argument(String description, Boolean isRequired, String format, String value, Boolean isSecret,
      String defaultValue, List<String> choices, Map<String, Input> variables, String type) {
    super(description, isRequired, format, value, isSecret, defaultValue, choices, variables);
    this.type = type;
  }

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
    builder.append("type", type);
    return builder.toString();
  }

}

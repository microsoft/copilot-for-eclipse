package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A positional argument for an MCP server.
 */
public class PositionalArgument extends Argument {
  @SerializedName("value_hint")
  private String valueHint;

  @SerializedName("is_repeated")
  private boolean isRepeated;

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
   * @param valueHint Hint about the expected value for the positional argument
   * @param isRepeated Whether the positional argument can be repeated
   */
  public PositionalArgument(String description, boolean isRequired, String format, String value, boolean isSecret,
      String defaultValue, List<String> choices, Map<String, Input> variables, String valueHint, boolean isRepeated) {
    super(description, isRequired, format, value, isSecret, defaultValue, choices, variables, "positional");
    this.valueHint = valueHint;
    this.isRepeated = isRepeated;
  }

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

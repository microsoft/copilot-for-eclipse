package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A named argument for an MCP server.
 */
public class NamedArgument extends Argument {
  @SerializedName("name")
  private String name;

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
   * @param name Name of the named argument
   * @param isRepeated Whether the named argument can be repeated
   */
  public NamedArgument(String description, boolean isRequired, String format, String value, boolean isSecret,
      String defaultValue, List<String> choices, Map<String, Input> variables, String name, boolean isRepeated) {
    super(description, isRequired, format, value, isSecret, defaultValue, choices, variables, "named");
    this.name = name;
    this.isRepeated = isRepeated;
  }

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
    builder.append("type", getType());
    builder.append("name", name);
    builder.append("isRepeated", isRepeated);
    return builder.toString();
  }
}

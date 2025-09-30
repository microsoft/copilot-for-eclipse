package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents an input parameter for an MCP package or runtime.
 */
public class Input {
  private String description;

  @SerializedName("is_required")
  private Boolean isRequired;
  private InputFormat format;
  private String value;

  @SerializedName("is_secret")
  private Boolean isSecret;

  @SerializedName("default")
  private String defaultValue;
  private List<String> choices;

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
   */
  public Input(String description, Boolean isRequired, InputFormat format, String value, Boolean isSecret,
      String defaultValue, List<String> choices) {
    this.description = description;
    this.isRequired = isRequired;
    this.format = format;
    this.value = value;
    this.isSecret = isSecret;
    this.defaultValue = defaultValue;
    this.choices = choices;
  }

  /**
   * Parameterized constructor that accepts format as a string.
   *
   * @param description Description of the input
   * @param isRequired Whether the input is required
   * @param format Format of the input as a string
   * @param value Value of the input
   * @param isSecret Whether the input is secret
   * @param defaultValue Default value of the input
   * @param choices List of valid choices for the input
   */
  public Input(String description, Boolean isRequired, String format, String value, Boolean isSecret,
      String defaultValue, List<String> choices) {
    this(description, isRequired, InputFormat.fromString(format), value, isSecret, defaultValue, choices);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getIsRequired() {
    return isRequired;
  }

  public void setIsRequired(Boolean isRequired) {
    this.isRequired = isRequired;
  }

  public InputFormat getFormat() {
    return format;
  }

  public void setFormat(InputFormat format) {
    this.format = format;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Boolean getIsSecret() {
    return isSecret;
  }

  public void setIsSecret(Boolean isSecret) {
    this.isSecret = isSecret;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public List<String> getChoices() {
    return choices;
  }

  public void setChoices(List<String> choices) {
    this.choices = choices;
  }

  @Override
  public int hashCode() {
    return Objects.hash(choices, defaultValue, description, format, isRequired, isSecret, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Input other = (Input) obj;
    return Objects.equals(choices, other.choices) && Objects.equals(defaultValue, other.defaultValue)
        && Objects.equals(description, other.description) && Objects.equals(format, other.format)
        && Objects.equals(isRequired, other.isRequired) && Objects.equals(isSecret, other.isSecret)
        && Objects.equals(value, other.value);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("description", description);
    builder.append("isRequired", isRequired);
    builder.append("format", format);
    builder.append("value", value);
    builder.append("isSecret", isSecret);
    builder.append("defaultValue", defaultValue);
    builder.append("choices", choices);
    return builder.toString();
  }

  /**
   * Enum representing the possible input formats.
   */
  public static enum InputFormat {
    @SerializedName("string")
    STRING,
    @SerializedName("number")
    NUMBER,
    @SerializedName("boolean")
    BOOLEAN,
    @SerializedName("filepath")
    FILEPATH;

    /**
     * Backwards-compatible accessor returning the JSON wire value. (No backing field named 'value' to avoid LSP4J enum
     * adapter integer check.)
     *
     * @return lowercase JSON token for this format
     */
    public String getValue() {
      return toString();
    }

    /**
     * Converts a string to an InputFormat enum value.
     *
     * @param value The string representation of the format
     * @return The corresponding InputFormat enum value, or null if value is null
     * @throws IllegalArgumentException if the format is unknown
     */
    public static InputFormat fromString(String value) {
      if (value == null) {
        return null;
      }
      switch (value) {
        case "string":
          return STRING;
        case "number":
          return NUMBER;
        case "boolean":
          return BOOLEAN;
        case "filepath":
          return FILEPATH;
        default:
          throw new IllegalArgumentException("Unknown format: " + value);
      }
    }

    @Override
    public String toString() {
      switch (this) {
        case STRING:
          return "string";
        case NUMBER:
          return "number";
        case BOOLEAN:
          return "boolean";
        case FILEPATH:
          return "filepath";
        default:
          return name();
      }
    }
  }

}

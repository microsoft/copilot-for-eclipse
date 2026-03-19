package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents an input parameter for an MCP package or runtime.
 */
public class Input {
  private String description;
  private Boolean isRequired;
  private Format format;
  private String value;
  private Boolean isSecret;

  @SerializedName("default")
  private String defaultValue;
  private String placeholder;
  private List<String> choices;

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

  public Format getFormat() {
    return format;
  }

  public void setFormat(Format format) {
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

  public String getPlaceholder() {
    return placeholder;
  }

  public void setPlaceholder(String placeholder) {
    this.placeholder = placeholder;
  }

  public List<String> getChoices() {
    return choices;
  }

  public void setChoices(List<String> choices) {
    this.choices = choices;
  }

  @Override
  public int hashCode() {
    return Objects.hash(choices, defaultValue, description, format, isRequired, isSecret, placeholder, value);
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
        && Objects.equals(description, other.description) && format == other.format
        && Objects.equals(isRequired, other.isRequired) && Objects.equals(isSecret, other.isSecret)
        && Objects.equals(placeholder, other.placeholder) && Objects.equals(value, other.value);
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
    builder.append("placeholder", placeholder);
    builder.append("choices", choices);
    return builder.toString();
  }

  /**
   * Enum representing the possible input formats.
   */
  public static enum Format {
    @SerializedName("string")
    STRING, @SerializedName("number")
    NUMBER, @SerializedName("boolean")
    BOOLEAN, @SerializedName("filepath")
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
     * Converts a string to an Format enum value.
     *
     * @param value The string representation of the format
     * @return The corresponding Format enum value, or null if value is null
     * @throws IllegalArgumentException if the format is unknown
     */
    public static Format fromString(String value) {
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

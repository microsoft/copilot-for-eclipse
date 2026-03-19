package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A JSON schema for the input this tool accepts. The input must be an object at the top level. A particular language
 * model may not support all JSON schema features. See the documentation for the language model family you are using for
 * more information.
 */
public class InputSchema {
  private String type;
  private Map<String, InputSchemaPropertyValue> properties;
  private List<String> required;

  /**
   * Constructor for InputSchema.
   */
  public InputSchema() {
    this.properties = new HashMap<>();
    this.required = new ArrayList<>();
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, InputSchemaPropertyValue> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, InputSchemaPropertyValue> properties) {
    this.properties = properties;
  }

  public List<String> getRequired() {
    return required;
  }

  public void setRequired(List<String> required) {
    this.required = required;
  }

  @Override
  public int hashCode() {
    return Objects.hash(properties, required, type);
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
    InputSchema other = (InputSchema) obj;
    return Objects.equals(properties, other.properties) && Objects.equals(required, other.required)
        && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("type", type);
    builder.append("properties", properties);
    builder.append("required", required);
    return builder.toString();
  }
}

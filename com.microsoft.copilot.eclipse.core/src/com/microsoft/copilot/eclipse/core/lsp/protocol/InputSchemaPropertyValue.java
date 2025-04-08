package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Property value for input schema.
 */
public class InputSchemaPropertyValue {
  private String type;
  private String description;

  /**
   * Constructor for InputSchemaPropertyValue.
   */
  public InputSchemaPropertyValue(String type, String description) {
    this.type = type;
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, type);
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
    InputSchemaPropertyValue other = (InputSchemaPropertyValue) obj;
    return Objects.equals(description, other.description) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("type", type);
    builder.add("description", description);
    return builder.toString();
  }
}

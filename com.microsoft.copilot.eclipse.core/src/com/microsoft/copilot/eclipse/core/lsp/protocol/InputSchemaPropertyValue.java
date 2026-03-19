package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Property value for input schema.
 */
public class InputSchemaPropertyValue {
  private String type;
  private String description;
  private InputSchemaPropertyValue items;

  /**
   * Constructor for InputSchemaPropertyValue.
   */
  public InputSchemaPropertyValue(String type) {
    this(type, "");
  }

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

  public InputSchemaPropertyValue getItems() {
    return items;
  }

  public void setItems(InputSchemaPropertyValue items) {
    this.items = items;
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, items, type);
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
    return Objects.equals(description, other.description) && Objects.equals(items, other.items)
        && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("type", type);
    builder.append("description", description);
    builder.append("items", items);
    return builder.toString();
  }
}

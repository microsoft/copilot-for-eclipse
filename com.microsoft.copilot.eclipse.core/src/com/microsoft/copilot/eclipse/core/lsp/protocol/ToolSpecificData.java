// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Tool-specific data passed from CLS to IDE for rendering.
 *
 * <p>Contains data specific to certain tools, such as todo list items.
 */
public class ToolSpecificData {
  private static final Gson GSON = new Gson();

  /**
   * The kind of tool-specific data (e.g., "todoList").
   */
  private String kind;

  /**
   * Generic data payload. The actual type depends on the kind.
   */
  private Object data;

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  /**
   * Convenience accessor for todo list data when kind == "todoList".
   * Converts the raw data (LinkedTreeMap from Gson) to List of TodoItem.
   */
  public List<TodoItem> getTodoList() {
    if (!"todoList".equals(kind) || data == null) {
      return null;
    }
    // Gson deserializes as List<LinkedTreeMap>, need to convert via JSON round-trip
    if (data instanceof List) {
      String json = GSON.toJson(data);
      Type listType = new TypeToken<List<TodoItem>>() {}.getType();
      return GSON.fromJson(json, listType);
    }
    return null;
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, data);
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
    ToolSpecificData other = (ToolSpecificData) obj;
    return Objects.equals(kind, other.kind) && Objects.equals(data, other.data);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("kind", kind);
    builder.append("data", data);
    return builder.toString();
  }
}

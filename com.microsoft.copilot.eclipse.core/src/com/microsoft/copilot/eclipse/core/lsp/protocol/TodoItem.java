// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a todo item in the chat todo list. This mirrors the structure used in the language server protocol.
 */
public class TodoItem {
  /**
   * Enum representing the status of a todo item.
   */
  public enum Status {
    NOT_STARTED("not-started"),
    IN_PROGRESS("in-progress"),
    COMPLETED("completed");

    private final String value;

    Status(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private int id;
  private String title;
  private String description;
  private String status;

  /**
   * Default constructor.
   */
  public TodoItem() {
    this.status = Status.NOT_STARTED.getValue();
  }

  /**
   * Constructor with all fields.
   *
   * @param id the todo item ID
   * @param title the todo title
   * @param description the optional description
   * @param status the status
   */
  public TodoItem(int id, String title, String description, String status) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.status = status;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isCompleted() {
    return Status.COMPLETED.getValue().equals(status);
  }

  public boolean isInProgress() {
    return Status.IN_PROGRESS.getValue().equals(status);
  }

  public boolean isNotStarted() {
    return Status.NOT_STARTED.getValue().equals(status);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TodoItem todoItem = (TodoItem) o;
    return id == todoItem.id && Objects.equals(title, todoItem.title)
        && Objects.equals(description, todoItem.description) && Objects.equals(status, todoItem.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, description, status);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("id", id);
    builder.append("title", title);
    builder.append("description", description);
    builder.append("status", status);
    return builder.toString();
  }
}

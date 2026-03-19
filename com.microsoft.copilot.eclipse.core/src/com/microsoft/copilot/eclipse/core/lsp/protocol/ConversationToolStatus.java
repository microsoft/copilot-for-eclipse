package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a tool status for conversation mode.
 */
public class ConversationToolStatus {
  private String name;
  private String status;

  /**
   * Creates a new ConversationToolStatus.
   */
  public ConversationToolStatus() {
  }

  /**
   * Creates a new ConversationToolStatus.
   *
   * @param name   the tool name
   * @param status the tool status (enabled/disabled)
   */
  public ConversationToolStatus(String name, String status) {
    this.name = name;
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, status);
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
    ConversationToolStatus other = (ConversationToolStatus) obj;
    return Objects.equals(name, other.name) && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name);
    builder.append("status", status);
    return builder.toString();
  }
}

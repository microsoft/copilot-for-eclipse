package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Tool call from agents.
 */
public class AgentToolCall {

  /**
   * Client can use this ID to track the tool call invocation, and apply the updates to that tool call.
   */
  private String id;

  /**
   * Tool name.
   */
  private String name;

  /**
   * Progress message to be displayed in the UI.
   */
  private String progressMessage;
  private String status;
  private String error;

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getProgressMessage() {
    return progressMessage;
  }

  public String getStatus() {
    return status;
  }

  public String getError() {
    return error;
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, id, name, progressMessage, status);
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
    AgentToolCall other = (AgentToolCall) obj;
    return Objects.equals(error, other.error) && Objects.equals(id, other.id) && Objects.equals(name, other.name)
        && Objects.equals(progressMessage, other.progressMessage) && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("id", id);
    builder.add("name", name);
    builder.add("progressMessage", progressMessage);
    builder.add("status", status);
    builder.add("error", error);
    return builder.toString();
  }

}

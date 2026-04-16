// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

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

  /**
   * Tool-specific data passed from CLS for rendering in the UI.
   */
  private ToolSpecificData toolSpecificData;

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

  public ToolSpecificData getToolSpecificData() {
    return toolSpecificData;
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, id, name, progressMessage, status, toolSpecificData);
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
        && Objects.equals(progressMessage, other.progressMessage)
        && Objects.equals(status, other.status) && Objects.equals(toolSpecificData, other.toolSpecificData);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("id", id);
    builder.append("name", name);
    builder.append("progressMessage", progressMessage);
    builder.append("status", status);
    builder.append("error", error);
    builder.append("toolSpecificData", toolSpecificData);
    return builder.toString();
  }

}

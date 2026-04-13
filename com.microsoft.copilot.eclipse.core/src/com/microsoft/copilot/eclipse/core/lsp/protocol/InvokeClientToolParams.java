// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for invoking a client tool.
 */
public class InvokeClientToolParams {
  /**
   * The name of the tool to be invoked.
   */
  private String name;

  /**
   * The input to the tool.
   */
  private Object input;

  /**
   * The ID of the conversation this tool invocation belongs to.
   */
  private String conversationId;

  /**
   * The ID of the turn this tool invocation belongs to.
   */
  private String turnId;

  /**
   * The ID of the round this tool invocation belongs to.
   */
  private int roundId;

  /**
   * The unique ID for this specific tool call.
   */
  private String toolCallId;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Object getInput() {
    return input;
  }

  public void setInput(Object input) {
    this.input = input;
  }

  public String getConversationId() {
    return conversationId;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public String getTurnId() {
    return turnId;
  }

  public void setTurnId(String turnId) {
    this.turnId = turnId;
  }

  public int getRoundId() {
    return roundId;
  }

  public void setRoundId(int roundId) {
    this.roundId = roundId;
  }

  public String getToolCallId() {
    return toolCallId;
  }

  public void setToolCallId(String toolCallId) {
    this.toolCallId = toolCallId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conversationId, input, name, roundId, toolCallId, turnId);
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
    InvokeClientToolParams other = (InvokeClientToolParams) obj;
    return Objects.equals(conversationId, other.conversationId) && Objects.equals(input, other.input)
        && Objects.equals(name, other.name) && roundId == other.roundId && Objects.equals(toolCallId, other.toolCallId)
        && Objects.equals(turnId, other.turnId);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name);
    builder.append("input", input);
    builder.append("conversationId", conversationId);
    builder.append("turnId", turnId);
    builder.append("roundId", roundId);
    builder.append("toolCallId", toolCallId);
    return builder.toString();
  }
}

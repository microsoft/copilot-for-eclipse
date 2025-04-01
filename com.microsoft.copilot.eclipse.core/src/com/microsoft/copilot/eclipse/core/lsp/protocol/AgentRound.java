package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * A round of a conversation with an agent. Usually a conversation consists of multiple rounds in agent mode.
 */
public class AgentRound {
  /**
   * The round ID is a number to organize the rounds in the order they were exploited.
   */
  private int roundId;

  /**
   * The reply from the LLM for this round, it will be a streamed response, So client can expect to collect the reply in
   * the same round.
   */
  private String reply;

  private List<AgentToolCall> toolCalls;

  public int getRoundId() {
    return roundId;
  }

  public String getReply() {
    return reply;
  }

  public List<AgentToolCall> getToolCalls() {
    return toolCalls;
  }

  @Override
  public int hashCode() {
    return Objects.hash(reply, roundId, toolCalls);
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
    AgentRound other = (AgentRound) obj;
    return Objects.equals(reply, other.reply) && roundId == other.roundId && Objects.equals(toolCalls, other.toolCalls);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("roundId", roundId);
    builder.add("reply", reply);
    builder.add("toolCalls", toolCalls);
    return builder.toString();
  }

}

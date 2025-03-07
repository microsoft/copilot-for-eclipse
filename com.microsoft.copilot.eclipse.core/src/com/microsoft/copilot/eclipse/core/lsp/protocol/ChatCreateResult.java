package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Result of a chat creation.
 */
public class ChatCreateResult {
  @NonNull
  private String conversationId;
  @NonNull
  private String turnId;
  private String agentSlug;
  private String modelFamily;

  public String getConversationId() {
    return conversationId;
  }

  public String getTurnId() {
    return turnId;
  }

  public String getAgentSlug() {
    return agentSlug;
  }

  public String getModelFamily() {
    return modelFamily;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public void setTurnId(String turnId) {
    this.turnId = turnId;
  }

  public void setAgentSlug(String agentSlug) {
    this.agentSlug = agentSlug;
  }

  public void setModelFamily(String modelFamily) {
    this.modelFamily = modelFamily;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conversationId, turnId, agentSlug, modelFamily);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChatCreateResult that = (ChatCreateResult) o;
    return Objects.equals(conversationId, that.conversationId) && Objects.equals(turnId, that.turnId)
        && Objects.equals(agentSlug, that.agentSlug) && Objects.equals(modelFamily, that.modelFamily);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("conversationId", conversationId);
    builder.add("turnId", turnId);
    builder.add("agentSlug", agentSlug);
    builder.add("modelFamily", modelFamily);
    return builder.toString();
  }
}

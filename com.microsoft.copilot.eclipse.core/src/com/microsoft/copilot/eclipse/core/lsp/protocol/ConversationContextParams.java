package com.microsoft.copilot.eclipse.core.lsp.protocol;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Parameters for the conversation context (skills) request.
 */
public record ConversationContextParams(String conversationId, String turnId, String skillId) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("conversationId", conversationId);
    builder.add("turnId", turnId);
    builder.add("skillId", skillId);
    return builder.toString();
  }
}

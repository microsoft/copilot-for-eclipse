package com.microsoft.copilot.eclipse.core.lsp.protocol;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for the conversation context (skills) request.
 */
public record ConversationContextParams(String conversationId, String turnId, String skillId) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("conversationId", conversationId);
    builder.append("turnId", turnId);
    builder.append("skillId", skillId);
    return builder.toString();
  }
}

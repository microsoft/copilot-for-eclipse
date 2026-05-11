// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for destroying a conversation.
 */
public class ConversationDestroyParams {
  private String conversationId;

  /**
   * Creates a new ConversationDestroyParams.
   */
  public ConversationDestroyParams(String conversationId) {
    this.conversationId = conversationId;
  }

  public String getConversationId() {
    return conversationId;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conversationId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ConversationDestroyParams other = (ConversationDestroyParams) obj;
    return Objects.equals(conversationId, other.conversationId);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("conversationId", conversationId);
    return builder.toString();
  }
}

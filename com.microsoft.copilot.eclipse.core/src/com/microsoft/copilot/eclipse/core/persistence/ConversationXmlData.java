package com.microsoft.copilot.eclipse.core.persistence;

import java.time.Instant;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Summary information for a conversation used in the conversation index.
 */
public class ConversationXmlData {
  private String conversationId;
  private String title;
  private Instant creationDate;
  private Instant lastMessageDate;

  /**
   * Default constructor initializing default values.
   */
  public ConversationXmlData(String conversationId, String title, Instant creationDate, Instant lastMessageDate) {
    this.conversationId = conversationId;
    this.title = title;
    this.creationDate = creationDate;
    this.lastMessageDate = lastMessageDate;
  }

  public String getConversationId() {
    return conversationId;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Instant getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Instant creationDate) {
    this.creationDate = creationDate;
  }

  public Instant getLastMessageDate() {
    return lastMessageDate;
  }

  public void setLastMessageDate(Instant lastMessageDate) {
    this.lastMessageDate = lastMessageDate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conversationId, creationDate, lastMessageDate, title);
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
    ConversationXmlData other = (ConversationXmlData) obj;
    return Objects.equals(conversationId, other.conversationId) && Objects.equals(creationDate, other.creationDate)
        && Objects.equals(lastMessageDate, other.lastMessageDate) && Objects.equals(title, other.title);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("conversationId", conversationId);
    builder.add("title", title);
    builder.add("creationDate", creationDate);
    builder.add("lastMessageDate", lastMessageDate);
    return builder.toString();
  }
}

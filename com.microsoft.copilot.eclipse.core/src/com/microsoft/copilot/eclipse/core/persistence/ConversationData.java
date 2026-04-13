// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.microsoft.copilot.eclipse.core.lsp.protocol.TodoItem;

/**
 * Full conversation data for persistence.
 */
public class ConversationData {
  private String conversationId;
  private String title;
  private String requesterUsername;
  private String responderUsername;
  private List<AbstractTurnData> turns;
  private Map<String, Object> data;
  private Instant creationDate;
  private Instant lastMessageDate;
  private List<TodoItem> todos;

  /**
   * Default constructor initializing default values.
   */
  public ConversationData() {
    this.responderUsername = "GitHub Copilot";
    this.turns = new ArrayList<>();
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

  public String getRequesterUsername() {
    return requesterUsername;
  }

  public void setRequesterUsername(String requesterUsername) {
    this.requesterUsername = requesterUsername;
  }

  public String getResponderUsername() {
    return responderUsername;
  }

  public void setResponderUsername(String responderUsername) {
    this.responderUsername = responderUsername;
  }

  public List<AbstractTurnData> getTurns() {
    return turns;
  }

  public void setTurns(List<AbstractTurnData> turns) {
    this.turns = turns;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
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

  public List<TodoItem> getTodos() {
    return todos;
  }

  public void setTodos(List<TodoItem> todos) {
    this.todos = todos;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conversationId, creationDate, data, lastMessageDate, requesterUsername, responderUsername,
        title, todos, turns);
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
    ConversationData other = (ConversationData) obj;
    return Objects.equals(conversationId, other.conversationId) && Objects.equals(creationDate, other.creationDate)
        && Objects.equals(data, other.data) && Objects.equals(lastMessageDate, other.lastMessageDate)
        && Objects.equals(requesterUsername, other.requesterUsername)
        && Objects.equals(responderUsername, other.responderUsername) && Objects.equals(title, other.title)
        && Objects.equals(todos, other.todos) && Objects.equals(turns, other.turns);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("conversationId", conversationId);
    builder.append("title", title);
    builder.append("requesterUsername", requesterUsername);
    builder.append("responderUsername", responderUsername);
    builder.append("turns", turns);
    builder.append("todos", todos);
    builder.append("data", data);
    builder.append("creationDate", creationDate);
    builder.append("lastMessageDate", lastMessageDate);
    return builder.toString();
  }
}

package com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for a coding agent message request.
 */
public class CodingAgentMessageRequestParams {
  private String title;
  private String description;
  private String prLink;
  private String conversationId;
  private String turnId;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getPrLink() {
    return prLink;
  }

  public void setPrLink(String prLink) {
    this.prLink = prLink;
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

  @Override
  public int hashCode() {
    return Objects.hash(conversationId, description, prLink, title, turnId);
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
    CodingAgentMessageRequestParams other = (CodingAgentMessageRequestParams) obj;
    return Objects.equals(conversationId, other.conversationId) && Objects.equals(description, other.description)
        && Objects.equals(prLink, other.prLink) && Objects.equals(title, other.title)
        && Objects.equals(turnId, other.turnId);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("title", title);
    builder.append("description", description);
    builder.append("prLink", prLink);
    builder.append("conversationId", conversationId);
    builder.append("turnId", turnId);
    return builder.toString();
  }

}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.lsp4j.WorkDoneProgressNotification;

/**
 * Creates a new ChatProgressValue.
 */
public class ChatProgressValue implements WorkDoneProgressNotification {
  private WorkDoneProgressKind kind;
  private String title;
  private String conversationId;
  private String turnId;
  private String parentTurnId;
  private String reply;
  private CopilotAnnotation[] annotations;
  private ChatReference[] references;
  private boolean hideText;
  private String[] notifications;
  private ChatStep[] steps;
  private String cancellationReason;
  private ConversationError error;
  private List<AgentRound> editAgentRounds;
  private String suggestedTitle;
  private ContextSizeInfo contextSize;

  public WorkDoneProgressKind getKind() {
    return kind;
  }

  public String getTitle() {
    return title;
  }

  public String getConversationId() {
    return conversationId;
  }

  public String getTurnId() {
    return turnId;
  }

  public String getParentTurnId() {
    return parentTurnId;
  }

  public String getReply() {
    return reply;
  }

  public CopilotAnnotation[] getAnnotations() {
    return annotations;
  }

  public ChatReference[] getReferences() {
    return references;
  }

  public ConversationError getConversationError() {
    return error;
  }

  public String getSuggestedTitle() {
    return suggestedTitle;
  }

  public boolean isHideText() {
    return hideText;
  }

  public String[] getNotifications() {
    return notifications;
  }

  public ChatStep[] getSteps() {
    return steps;
  }

  public String getErrorMessage() {
    return error != null ? error.getMessage() : null;
  }

  public int getCode() {
    return error != null ? error.getCode() : 0;
  }

  public String getErrorReason() {
    return error != null ? error.getReason() : null;
  }

  public List<AgentRound> getAgentRounds() {
    return editAgentRounds;
  }

  public void setKind(WorkDoneProgressKind kind) {
    this.kind = kind;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public void setTurnId(String turnId) {
    this.turnId = turnId;
  }

  public void setParentTurnId(String parentTurnId) {
    this.parentTurnId = parentTurnId;
  }

  public void setReply(String reply) {
    this.reply = reply;
  }

  public void setAnnotations(CopilotAnnotation[] annotations) {
    this.annotations = annotations;
  }

  public void setReferences(ChatReference[] references) {
    this.references = references;
  }

  public void setHideText(boolean hideText) {
    this.hideText = hideText;
  }

  public void setNotifications(String[] notifications) {
    this.notifications = notifications;
  }

  public void setSteps(ChatStep[] steps) {
    this.steps = steps;
  }

  public void setSuggestedTitle(String suggestedTitle) {
    this.suggestedTitle = suggestedTitle;
  }

  public ContextSizeInfo getContextSize() {
    return contextSize;
  }

  public void setContextSize(ContextSizeInfo contextSize) {
    this.contextSize = contextSize;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public void setConversationError(ConversationError error) {
    this.error = error;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(annotations);
    result = prime * result + Arrays.hashCode(notifications);
    result = prime * result + Arrays.hashCode(references);
    result = prime * result + Arrays.hashCode(steps);
    result = prime * result + Objects.hash(editAgentRounds, cancellationReason, contextSize, conversationId, error,
        hideText, kind, reply, title, turnId, parentTurnId, suggestedTitle);
    return result;
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
    ChatProgressValue other = (ChatProgressValue) obj;
    return Objects.equals(editAgentRounds, other.editAgentRounds) && Arrays.equals(annotations, other.annotations)
        && Objects.equals(cancellationReason, other.cancellationReason)
        && Objects.equals(contextSize, other.contextSize)
        && Objects.equals(conversationId, other.conversationId) && Objects.equals(error, other.error)
        && hideText == other.hideText && kind == other.kind && Arrays.equals(notifications, other.notifications)
        && Arrays.equals(references, other.references) && Objects.equals(reply, other.reply)
        && Arrays.equals(steps, other.steps) && Objects.equals(title, other.title)
        && Objects.equals(turnId, other.turnId) && Objects.equals(parentTurnId, other.parentTurnId)
        && Objects.equals(suggestedTitle, other.suggestedTitle);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("kind", kind);
    builder.append("title", title);
    builder.append("conversationId", conversationId);
    builder.append("turnId", turnId);
    builder.append("parentTurnId", parentTurnId);
    builder.append("reply", reply);
    builder.append("annotations", Arrays.toString(annotations));
    builder.append("references", Arrays.toString(references));
    builder.append("hideText", hideText);
    builder.append("notifications", Arrays.toString(notifications));
    builder.append("steps", Arrays.toString(steps));
    builder.append("cancellationReason", cancellationReason);
    builder.append("error", error);
    builder.append("editAgentRounds", editAgentRounds);
    builder.append("suggestedTitle", suggestedTitle);
    builder.append("contextSize", contextSize);
    return builder.toString();
  }
}
package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.lsp4j.WorkDoneProgressNotification;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Creates a new ChatProgressValue.
 */
public class ChatProgressValue implements WorkDoneProgressNotification {
  private WorkDoneProgressKind kind;
  private String title;
  private String conversationId;
  private String turnId;
  private String reply;
  private CopilotAnnotation[] annotations;
  private FileReferenceParams[] references;
  private boolean hideText;
  private String[] notifications;
  private ChatStep[] steps;
  private String cancellationReason;
  private ConversationError error;
  private List<AgentRound> editAgentRounds;
  private String suggestedTitle;

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

  public String getReply() {
    return reply;
  }

  public CopilotAnnotation[] getAnnotations() {
    return annotations;
  }

  public FileReferenceParams[] getReferences() {
    return references;
  }

  public ConversationError getConversationError() {
    return error;
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

  public String getSuggestedTitle() {
    return suggestedTitle;
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

  public void setReply(String reply) {
    this.reply = reply;
  }

  public void setAnnotations(CopilotAnnotation[] annotations) {
    this.annotations = annotations;
  }

  public void setReferences(FileReferenceParams[] references) {
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

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public void setConversationError(ConversationError error) {
    this.error = error;
  }

  public void setSuggestedTitle(String suggestedTitle) {
    this.suggestedTitle = suggestedTitle;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(annotations);
    result = prime * result + Arrays.hashCode(notifications);
    result = prime * result + Arrays.hashCode(references);
    result = prime * result + Arrays.hashCode(steps);
    result = prime * result + Objects.hash(editAgentRounds, cancellationReason, conversationId, error, hideText, kind,
        reply, title, turnId, suggestedTitle);
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
        && Objects.equals(conversationId, other.conversationId) && Objects.equals(error, other.error)
        && hideText == other.hideText && kind == other.kind && Arrays.equals(notifications, other.notifications)
        && Arrays.equals(references, other.references) && Objects.equals(reply, other.reply)
        && Arrays.equals(steps, other.steps) && Objects.equals(title, other.title)
        && Objects.equals(turnId, other.turnId) && Objects.equals(suggestedTitle, other.suggestedTitle);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("kind", kind);
    builder.add("title", title);
    builder.add("conversationId", conversationId);
    builder.add("turnId", turnId);
    builder.add("reply", reply);
    builder.add("annotations", Arrays.toString(annotations));
    builder.add("references", Arrays.toString(references));
    builder.add("hideText", hideText);
    builder.add("notifications", Arrays.toString(notifications));
    builder.add("steps", Arrays.toString(steps));
    builder.add("cancellationReason", cancellationReason);
    builder.add("error", error);
    builder.add("editAgentRounds", editAgentRounds);
    builder.add("suggestedTitle", suggestedTitle);
    return builder.toString();
  }
}
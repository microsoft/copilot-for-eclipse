package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Arrays;
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

  @Override
  public int hashCode() {
    return Objects.hash(kind, title, conversationId, turnId, reply, annotations, references, hideText, notifications,
        steps, cancellationReason, error);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChatProgressValue that = (ChatProgressValue) o;
    return hideText == that.hideText && Objects.equals(kind, that.kind) && Objects.equals(title, that.title)
        && Objects.equals(conversationId, that.conversationId) && Objects.equals(turnId, that.turnId)
        && Objects.equals(reply, that.reply) && Arrays.equals(annotations, that.annotations)
        && Arrays.equals(references, that.references) && Arrays.equals(notifications, that.notifications)
        && Objects.equals(cancellationReason, that.cancellationReason) && Arrays.equals(steps, that.steps)
        && Objects.equals(error, that.error);
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
    builder.add("cancellationReason", cancellationReason);
    builder.add("notifications", Arrays.toString(notifications));
    builder.add("steps", Arrays.toString(steps));
    if (this.error != null) {
      builder.add("error", this.error.toString());
    }
    return builder.toString();
  }
}
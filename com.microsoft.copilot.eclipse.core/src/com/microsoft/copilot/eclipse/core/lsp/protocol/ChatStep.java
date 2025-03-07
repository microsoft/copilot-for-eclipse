package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Creates a new ChatStep.
 * ref:https://github.com/microsoft/copilot-client/blob/936dc4407e15d877ea9b445f19faab32749aa7c6/lib/src/conversation/steps.ts#L12
 */
public class ChatStep {
  private String id;
  private String title;
  private String status;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChatStep chatStep = (ChatStep) o;
    return Objects.equals(id, chatStep.id) && Objects.equals(title, chatStep.title)
        && Objects.equals(status, chatStep.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, status);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("id", id);
    builder.add("title", title);
    builder.add("status", status);
    return builder.toString();
  }
}
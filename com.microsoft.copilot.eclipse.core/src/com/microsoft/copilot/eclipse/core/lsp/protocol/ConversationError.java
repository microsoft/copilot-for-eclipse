package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Error that occurred during a conversation. See:
 * https://github.com/microsoft/copilot-client/blob/936dc4407e15d877ea9b445f19faab32749aa7c6/lib/src/conversation/conversationProgress.ts#L21
 */
public class ConversationError {
  @NonNull
  private String message;
  private int code;
  private String reason;
  private boolean responseIsIncomplete;
  private boolean responseIsFiltered;

  public void setMessage(String message) {
    this.message = message;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public void setResponseIsIncomplete(boolean responseIsIncomplete) {
    this.responseIsIncomplete = responseIsIncomplete;
  }

  public void setResponseIsFiltered(boolean responseIsFiltered) {
    this.responseIsFiltered = responseIsFiltered;
  }

  public String getMessage() {
    return message;
  }

  public int getCode() {
    return code;
  }

  public String getReason() {
    return reason;
  }

  public boolean getResponseIsIncomplete() {
    return responseIsIncomplete;
  }

  public boolean getResponseIsFiltered() {
    return responseIsFiltered;
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, code, reason, responseIsIncomplete, responseIsFiltered);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConversationError that = (ConversationError) o;
    return Objects.equals(message, that.message) && code == that.code && Objects.equals(reason, that.reason)
        && responseIsIncomplete == that.responseIsIncomplete && responseIsFiltered == that.responseIsFiltered;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("message", message);
    builder.append("code", code);
    builder.append("reason", reason);
    builder.append("responseIsIncomplete", responseIsIncomplete);
    builder.append("responseIsFiltered", responseIsFiltered);
    return builder.toString();
  }
}

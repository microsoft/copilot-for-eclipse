package com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Result of a coding agent message.
 */
public class CodingAgentMessageResult {
  @NonNull
  private boolean success;

  private String error;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, success);
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
    CodingAgentMessageResult other = (CodingAgentMessageResult) obj;
    return Objects.equals(error, other.error) && success == other.success;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("success", success);
    builder.add("error", error);
    return builder.toString();
  }

}

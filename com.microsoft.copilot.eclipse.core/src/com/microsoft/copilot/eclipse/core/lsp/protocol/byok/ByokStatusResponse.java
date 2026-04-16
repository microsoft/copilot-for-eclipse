// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Response model for BYOK operations.
 */
public class ByokStatusResponse {
  private boolean success;
  private String message;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, message);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ByokStatusResponse other = (ByokStatusResponse) obj;
    return success == other.success && Objects.equals(message, other.message);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("success", success);
    builder.append("message", message);
    return builder.toString();
  }
}

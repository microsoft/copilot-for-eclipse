package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Parameter for SignInConfirm request.
 */
public class SignInConfirmParams {
  @NonNull
  public String userCode;

  /**
   * Create a new parameter for SignInConfirm request.
   */
  public SignInConfirmParams(String userCode) {
    this.userCode = userCode;
  }

  public String getUserCode() {
    return userCode;
  }

  public void setUserCode(String userCode) {
    this.userCode = userCode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(userCode);
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
    SignInConfirmParams other = (SignInConfirmParams) obj;
    return Objects.equals(userCode, other.userCode);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("userCode", userCode);
    return builder.toString();
  }
}

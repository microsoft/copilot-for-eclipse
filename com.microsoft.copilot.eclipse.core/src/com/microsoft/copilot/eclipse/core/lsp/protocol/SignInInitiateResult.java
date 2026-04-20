// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result for signInInitiate.
 */
public class SignInInitiateResult {

  public static final String ALREADY_SIGNED_IN = "AlreadySignedIn";

  private String status;
  private Integer expiresIn;
  private Integer interval;
  private String userCode;
  private String verificationUri;

  /**
   * Create a new SignInInitiateResult.
   */
  public SignInInitiateResult() {
  }

  public boolean isAlreadySignedIn() {
    return ALREADY_SIGNED_IN.equals(status);
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
  }

  public Integer getInterval() {
    return interval;
  }

  public void setInterval(Integer interval) {
    this.interval = interval;
  }

  public String getUserCode() {
    return userCode;
  }

  public void setUserCode(String userCode) {
    this.userCode = userCode;
  }

  public String getVerificationUri() {
    return verificationUri;
  }

  public void setVerificationUri(String verificationUri) {
    this.verificationUri = verificationUri;
  }

  @Override
  public int hashCode() {
    return Objects.hash(expiresIn, interval, status, userCode, verificationUri);
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
    SignInInitiateResult other = (SignInInitiateResult) obj;
    return Objects.equals(expiresIn, other.expiresIn) && Objects.equals(interval, other.interval)
        && Objects.equals(status, other.status) && Objects.equals(userCode, other.userCode)
        && Objects.equals(verificationUri, other.verificationUri);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("status", status);
    builder.append("expiresIn", expiresIn);
    builder.append("interval", interval);
    builder.append("userCode", userCode);
    builder.append("verificationUri", verificationUri);
    return builder.toString();
  }
}

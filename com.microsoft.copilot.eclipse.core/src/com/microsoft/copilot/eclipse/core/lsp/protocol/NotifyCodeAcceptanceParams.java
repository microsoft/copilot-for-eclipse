// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameter used for notifying code acceptance.
 */
public class NotifyCodeAcceptanceParams {
  private String turnId;

  /**
   * The number of files accepted by the user.
   */
  private int acceptedFileCount;

  /**
   * The initial number of files pending decision in this turn.
   */
  private int totalFileCount;

  /**
   * Constructor.
   */
  public NotifyCodeAcceptanceParams(String turnId, int acceptedFileCount, int totalFileCount) {
    this.turnId = turnId;
    this.acceptedFileCount = acceptedFileCount;
    this.totalFileCount = totalFileCount;
  }

  public String getTurnId() {
    return turnId;
  }

  public void setTurnId(String turnId) {
    this.turnId = turnId;
  }

  public int getAcceptedFileCount() {
    return acceptedFileCount;
  }

  public void setAcceptedFileCount(int acceptedFileCount) {
    this.acceptedFileCount = acceptedFileCount;
  }

  public int getTotalFileCount() {
    return totalFileCount;
  }

  public void setTotalFileCount(int totalFileCount) {
    this.totalFileCount = totalFileCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(acceptedFileCount, totalFileCount, turnId);
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
    NotifyCodeAcceptanceParams other = (NotifyCodeAcceptanceParams) obj;
    return acceptedFileCount == other.acceptedFileCount && totalFileCount == other.totalFileCount
        && Objects.equals(turnId, other.turnId);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("turnId", turnId);
    builder.append("acceptedFileCount", acceptedFileCount);
    builder.append("totalFileCount", totalFileCount);
    return builder.toString();
  }
}

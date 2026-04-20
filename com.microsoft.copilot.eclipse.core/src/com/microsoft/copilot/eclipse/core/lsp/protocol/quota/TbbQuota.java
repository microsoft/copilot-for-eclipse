// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Token based billing quota information with a reset timestamp.
 */
public class TbbQuota extends Quota {
  private String resetAt;

  public String getResetAt() {
    return resetAt;
  }

  public void setResetAt(String resetAt) {
    this.resetAt = resetAt;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), resetAt);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TbbQuota)) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    TbbQuota other = (TbbQuota) obj;
    return Objects.equals(resetAt, other.resetAt);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.appendSuper(super.toString());
    builder.append("resetAt", resetAt);
    return builder.toString();
  }
}

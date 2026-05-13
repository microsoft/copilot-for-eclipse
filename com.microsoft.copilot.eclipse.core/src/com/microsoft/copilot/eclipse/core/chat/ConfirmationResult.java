// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result of evaluating an auto-approve confirmation request.
 * Either AUTO_APPROVED (no UI needed) or NEEDS_CONFIRMATION with content for the dialog.
 */
public class ConfirmationResult {

  /** Auto-approved, no user confirmation needed. */
  public static final ConfirmationResult AUTO_APPROVED = new ConfirmationResult(true, null);

  private final boolean autoApproved;
  private final ConfirmationContent content;

  private ConfirmationResult(boolean autoApproved, ConfirmationContent content) {
    this.autoApproved = autoApproved;
    this.content = content;
  }

  /** Creates a result that requires user confirmation with the given content. */
  public static ConfirmationResult needsConfirmation(
      ConfirmationContent content) {
    return new ConfirmationResult(false, content);
  }

  public boolean isAutoApproved() {
    return autoApproved;
  }

  /** Returns the confirmation content, or null if auto-approved or using defaults. */
  public ConfirmationContent getContent() {
    return content;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoApproved, content);
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
    ConfirmationResult other = (ConfirmationResult) obj;
    return autoApproved == other.autoApproved
        && Objects.equals(content, other.content);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("autoApproved", autoApproved)
        .append("content", content)
        .toString();
  }
}

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
  public static final ConfirmationResult AUTO_APPROVED = new ConfirmationResult(true, false, null);

  /** Dismissed — malformed or unhandleable request; CLS should be told to skip the tool. */
  public static final ConfirmationResult DISMISSED = new ConfirmationResult(false, true, null);

  private final boolean autoApproved;
  private final boolean dismissed;
  private final ConfirmationContent content;

  private ConfirmationResult(boolean autoApproved, boolean dismissed, ConfirmationContent content) {
    this.autoApproved = autoApproved;
    this.dismissed = dismissed;
    this.content = content;
  }

  /** Creates a result that requires user confirmation with the given content. */
  public static ConfirmationResult needsConfirmation(
      ConfirmationContent content) {
    return new ConfirmationResult(false, false, content);
  }

  public boolean isAutoApproved() {
    return autoApproved;
  }

  /** Returns true if the request should be dismissed without showing UI. */
  public boolean isDismissed() {
    return dismissed;
  }

  /** Returns the confirmation content, or null if auto-approved or using defaults. */
  public ConfirmationContent getContent() {
    return content;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoApproved, dismissed, content);
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
        && dismissed == other.dismissed
        && Objects.equals(content, other.content);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("autoApproved", autoApproved)
        .append("dismissed", dismissed)
        .append("content", content)
        .toString();
  }
}

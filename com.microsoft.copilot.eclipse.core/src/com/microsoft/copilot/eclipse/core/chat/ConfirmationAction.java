// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a button action in the confirmation UI. Each action has a label,
 * a decision (accept or dismiss), a persistence scope, and optional metadata
 * for the handler to know what to persist.
 */
public class ConfirmationAction {

  /** Metadata key for the action type enum name. */
  public static final String META_ACTION = "action";

  private final String label;
  private final boolean accept;
  private final ConfirmationActionScope scope;
  private final Map<String, String> metadata;
  private final boolean primary;

  /**
   * Creates a new confirmation action.
   *
   * @param label the button label
   * @param accept true for accept, false for dismiss
   * @param scope the persistence scope (null for dismiss actions)
   * @param metadata extra data for the handler (e.g., command names, server name)
   * @param primary whether this is the primary/default button
   */
  public ConfirmationAction(String label, boolean accept,
      ConfirmationActionScope scope, Map<String, String> metadata,
      boolean primary) {
    this.label = label;
    this.accept = accept;
    this.scope = scope;
    this.metadata = metadata != null ? metadata : Map.of();
    this.primary = primary;
  }

  public String getLabel() {
    return label;
  }

  public boolean isAccept() {
    return accept;
  }

  public ConfirmationActionScope getScope() {
    return scope;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public boolean isPrimary() {
    return primary;
  }

  /** Creates a primary accept action (scope = ONCE). */
  public static ConfirmationAction allowOnce(String label) {
    return new ConfirmationAction(label, true,
        ConfirmationActionScope.ONCE, null, true);
  }

  /** Creates a dismiss action. */
  public static ConfirmationAction skip(String label) {
    return new ConfirmationAction(label, false, null, null, false);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accept, label, metadata, primary, scope);
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
    ConfirmationAction other = (ConfirmationAction) obj;
    return accept == other.accept
        && Objects.equals(label, other.label)
        && Objects.equals(metadata, other.metadata)
        && primary == other.primary && scope == other.scope;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("label", label)
        .append("accept", accept)
        .append("scope", scope)
        .append("metadata", metadata)
        .append("primary", primary)
        .toString();
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A single file-operation auto-approve rule mapping a glob pattern to an allow/deny decision.
 */
public class FileOperationAutoApproveRule {
  private String pattern;
  private String description;
  private boolean autoApprove;
  private transient boolean isDefault;

  /**
   * Creates a new rule.
   *
   * @param pattern the glob pattern (e.g., "**\/.github/instructions/*")
   * @param description human-readable description of what this pattern matches
   * @param autoApprove true to auto-approve, false to always require confirmation
   */
  public FileOperationAutoApproveRule(String pattern, String description, boolean autoApprove) {
    this(pattern, description, autoApprove, false);
  }

  /**
   * Creates a new rule.
   *
   * @param pattern the glob pattern
   * @param description human-readable description
   * @param autoApprove true to auto-approve, false to always require confirmation
   * @param isDefault true if this is a CLS default rule (non-removable)
   */
  public FileOperationAutoApproveRule(String pattern, String description,
      boolean autoApprove, boolean isDefault) {
    this.pattern = pattern;
    this.description = description;
    this.autoApprove = autoApprove;
    this.isDefault = isDefault;
  }

  /** Default constructor for Gson deserialization. */
  public FileOperationAutoApproveRule() {
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isAutoApprove() {
    return autoApprove;
  }

  public void setAutoApprove(boolean autoApprove) {
    this.autoApprove = autoApprove;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pattern, description, autoApprove);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FileOperationAutoApproveRule other = (FileOperationAutoApproveRule) obj;
    return Objects.equals(pattern, other.pattern)
        && Objects.equals(description, other.description)
        && autoApprove == other.autoApprove;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("pattern", pattern)
        .append("description", description)
        .append("autoApprove", autoApprove)
        .toString();
  }
}

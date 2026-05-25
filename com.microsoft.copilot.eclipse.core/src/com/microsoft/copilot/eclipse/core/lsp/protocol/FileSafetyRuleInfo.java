// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A file safety rule as returned by CLS {@code getDefaultFileSafetyRules}.
 *
 * <p>Field names match the CLS JSON-RPC response exactly.</p>
 */
public class FileSafetyRuleInfo {

  private String pattern;
  private boolean requiresConfirmation;
  private String description;

  /** Default constructor for Gson deserialization. */
  public FileSafetyRuleInfo() {
  }

  /**
   * Creates a new FileSafetyRuleInfo.
   *
   * @param pattern the glob pattern
   * @param requiresConfirmation whether the file requires confirmation
   * @param description description of the rule
   */
  public FileSafetyRuleInfo(String pattern, boolean requiresConfirmation,
      String description) {
    this.pattern = pattern;
    this.requiresConfirmation = requiresConfirmation;
    this.description = description;
  }

  public String getPattern() {
    return pattern;
  }


  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public boolean isRequiresConfirmation() {
    return requiresConfirmation;
  }

  public void setRequiresConfirmation(boolean requiresConfirmation) {
    this.requiresConfirmation = requiresConfirmation;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, pattern, requiresConfirmation);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FileSafetyRuleInfo other = (FileSafetyRuleInfo) obj;
    return Objects.equals(description, other.description)
        && Objects.equals(pattern, other.pattern)
        && requiresConfirmation == other.requiresConfirmation;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("pattern", pattern)
        .append("requiresConfirmation", requiresConfirmation)
        .append("description", description)
        .toString();
  }

}

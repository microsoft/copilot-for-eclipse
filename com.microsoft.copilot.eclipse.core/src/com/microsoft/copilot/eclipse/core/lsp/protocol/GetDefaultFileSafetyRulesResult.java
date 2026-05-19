// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result of the {@code getDefaultFileSafetyRules} CLS request.
 */
public class GetDefaultFileSafetyRulesResult {

  private List<FileSafetyRuleInfo> defaultRules;

  /** Default constructor for Gson deserialization. */
  public GetDefaultFileSafetyRulesResult() {
  }

  /**
   * Creates a new result with the given default rules.
   *
   * @param defaultRules the list of default file safety rules
   */
  public GetDefaultFileSafetyRulesResult(
      List<FileSafetyRuleInfo> defaultRules) {
    this.defaultRules = defaultRules;
  }

  public List<FileSafetyRuleInfo> getDefaultRules() {
    return defaultRules;
  }

  public void setDefaultRules(List<FileSafetyRuleInfo> defaultRules) {
    this.defaultRules = defaultRules;
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultRules);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    GetDefaultFileSafetyRulesResult other = (GetDefaultFileSafetyRulesResult) obj;
    return Objects.equals(defaultRules, other.defaultRules);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("defaultRules", defaultRules)
        .toString();
  }
}

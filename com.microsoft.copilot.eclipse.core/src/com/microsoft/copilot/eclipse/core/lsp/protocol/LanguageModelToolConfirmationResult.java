// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * The result of the confirmation.
 */
public class LanguageModelToolConfirmationResult {
  String result;

  /**
   * Construct a new LanguageModelToolConfirmationResult by ToolConfirmationResult.
   */
  public LanguageModelToolConfirmationResult(ToolConfirmationResult result) {
    this.result = result.toString();
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  @Override
  public int hashCode() {
    return Objects.hash(result);
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
    LanguageModelToolConfirmationResult other = (LanguageModelToolConfirmationResult) obj;
    return result == other.result;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("result", result);
    return builder.toString();
  }

  /**
   * Specifies the result of the confirmation.
   */
  public enum ToolConfirmationResult {
    /**
     * The user accepted the tool invocation.
     */
    ACCEPT("accept"),

    /**
     * The user dismissed the tool invocation.
     */
    DISMISS("dismiss");

    private final String value;

    ToolConfirmationResult(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}

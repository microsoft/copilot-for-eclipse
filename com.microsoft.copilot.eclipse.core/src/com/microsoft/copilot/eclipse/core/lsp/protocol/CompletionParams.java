// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Parameter for getCompletion request.
 */
public class CompletionParams {

  @NonNull
  private CompletionDocument doc;

  private Map<Object, Object> options;

  /**
   * Create a new parameter for getCompletion request.
   */
  public CompletionParams(@NonNull CompletionDocument doc) {
    this.doc = doc;
  }

  public CompletionDocument getDoc() {
    return doc;
  }

  public void setDoc(CompletionDocument doc) {
    this.doc = doc;
  }

  public Map<Object, Object> getOptions() {
    return options;
  }

  public void setOptions(Map<Object, Object> options) {
    this.options = options;
  }

  @Override
  public int hashCode() {
    return Objects.hash(doc, options);
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
    CompletionParams other = (CompletionParams) obj;
    return Objects.equals(doc, other.doc) && Objects.equals(options, other.options);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("doc", doc);
    builder.append("options", options);
    return builder.toString();
  }

}

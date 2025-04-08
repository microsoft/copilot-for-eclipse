package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * The text content of the part.
 */
public class LanguageModelTextPart {

  private String value;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
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
    LanguageModelTextPart other = (LanguageModelTextPart) obj;
    return Objects.equals(value, other.value);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("value", value);
    return builder.toString();
  }
}

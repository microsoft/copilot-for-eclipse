package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Result Language Model Tool.
 */
public class LanguageModelToolResult {
  /**
   * A list of tool result content parts. Includes `unknown` because this list may be extended with new content types in
   * the future.
   */
  private List<LanguageModelTextPart> content;

  /**
   * Creates a new LanguageModelToolResult.
   */
  public LanguageModelToolResult() {
    this.content = new ArrayList<>();
  }

  public List<LanguageModelTextPart> getContent() {
    return content;
  }

  public void setContent(List<LanguageModelTextPart> content) {
    this.content = content;
  }

  @Override
  public int hashCode() {
    return Objects.hash(content);
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
    LanguageModelToolResult other = (LanguageModelToolResult) obj;
    return Objects.equals(content, other.content);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("content", content);
    return builder.toString();
  }
}

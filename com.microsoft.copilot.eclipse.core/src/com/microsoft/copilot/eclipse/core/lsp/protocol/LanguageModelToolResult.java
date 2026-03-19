package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result Language Model Tool.
 */
public class LanguageModelToolResult {
  /**
   * The status of the tool invocation.
   */
  private String status;

  /**
   * A list of tool result content parts. Includes `unknown` because this list may be extended with new content types in
   * the future.
   */
  private List<LanguageModelTextPart> content;

  /**
   * Creates a new LanguageModelToolResult.
   */
  public LanguageModelToolResult() {
    this.status = ToolInvocationStatus.success.toString();
    this.content = new ArrayList<>();
  }

  /**
   * Creates a new LanguageModelToolResult with content and ToolInvocationStatus.
   */
  public LanguageModelToolResult(String resultContent, ToolInvocationStatus status) {
    this.status = status.toString();
    this.content = new ArrayList<>();
    this.content.add(new LanguageModelTextPart(resultContent));
  }

  /**
   * Creates a new LanguageModelToolResult with the specified content value.
   *
   * @param contentValue the content value to set to the LanguageModelTextPart.
   */
  public void addContent(String contentValue) {
    LanguageModelTextPart textPart = new LanguageModelTextPart(contentValue);
    this.content.add(textPart);
  }

  public List<LanguageModelTextPart> getContent() {
    return content;
  }

  public void setContent(List<LanguageModelTextPart> content) {
    this.content = content;
  }

  public ToolInvocationStatus getStatus() {
    return ToolInvocationStatus.valueOf(status);
  }

  public void setStatus(ToolInvocationStatus status) {
    this.status = status.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, status);
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
    return Objects.equals(content, other.content) && status == other.status;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("status", status);
    builder.append("content", content);
    return builder.toString();
  }

  /**
   * The status of the tool invocation.
   */
  public static enum ToolInvocationStatus {
    success, error, cancelled
  }
}

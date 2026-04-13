// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a text content part in a chat completion.
 * This class implements the ChatCompletionContentPart interface and contains
 * a type and text content.
 */
public class TextContentPart implements ChatCompletionContentPart {
  private final String type = ContentType.TEXT.getValue();
  private final String text;

  /**
   * Constructs a TextContentPart with the specified text.
   *
   * @param text The text content of this part.
   */
  public TextContentPart(String text) {
    this.text = text;
  }

  @Override
  public String getType() {
    return type;
  }

  public String getText() {
    return text;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, text);
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
    TextContentPart other = (TextContentPart) obj;
    return Objects.equals(type, other.type) && Objects.equals(text, other.text);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("type", type);
    builder.append("text", text);
    return builder.toString();
  }
}
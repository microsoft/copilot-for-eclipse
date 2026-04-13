// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result of reading a file, containing both text content and file stats.
 */
public class ReadFileResult {

  private String text;
  private FileStat stat;

  /**
   * Constructor with text and stat.
   *
   * @param text the text content of the file
   * @param stat the file stats
   */
  public ReadFileResult(String text, FileStat stat) {
    this.text = text;
    this.stat = stat;
  }

  /**
   * Gets the text content of the file.
   *
   * @return the text content
   */
  public String getText() {
    return text;
  }

  /**
   * Sets the text content of the file.
   *
   * @param text the text content
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * Gets the file stats.
   *
   * @return the file stats
   */
  public FileStat getStat() {
    return stat;
  }

  /**
   * Sets the file stats.
   *
   * @param stat the file stats
   */
  public void setStat(FileStat stat) {
    this.stat = stat;
  }

  @Override
  public int hashCode() {
    return Objects.hash(stat, text);
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
    ReadFileResult other = (ReadFileResult) obj;
    return Objects.equals(stat, other.stat) && Objects.equals(text, other.text);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("text", text);
    builder.append("stat", stat);
    return builder.toString();
  }

}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for saving a BYOK model configuration.
 */
public class ByokModelCapabilities {
  private String name;
  private Integer maxInputTokens;
  private Integer maxOutputTokens;
  private boolean toolCalling;
  private boolean vision;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getMaxInputTokens() {
    return maxInputTokens;
  }

  public void setMaxInputTokens(Integer maxInputTokens) {
    this.maxInputTokens = maxInputTokens;
  }

  public Integer getMaxOutputTokens() {
    return maxOutputTokens;
  }

  public void setMaxOutputTokens(Integer maxOutputTokens) {
    this.maxOutputTokens = maxOutputTokens;
  }

  public boolean isToolCalling() {
    return toolCalling;
  }

  public void setToolCalling(boolean toolCalling) {
    this.toolCalling = toolCalling;
  }

  public boolean isVision() {
    return vision;
  }

  public void setVision(boolean vision) {
    this.vision = vision;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, maxInputTokens, maxOutputTokens, toolCalling, vision);
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
    ByokModelCapabilities other = (ByokModelCapabilities) obj;
    return Objects.equals(name, other.name) && Objects.equals(maxInputTokens, other.maxInputTokens)
        && Objects.equals(maxOutputTokens, other.maxOutputTokens) && toolCalling == other.toolCalling
        && vision == other.vision;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name);
    builder.append("maxInputTokens", maxInputTokens);
    builder.append("maxOutputTokens", maxOutputTokens);
    builder.append("toolCalling", toolCalling);
    builder.append("vision", vision);
    return builder.toString();
  }
}

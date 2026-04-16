// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.HashMap;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * This class is a representation of the CopilotAnnotation type in the Language Server Protocol.
 */
public class CopilotAnnotation {
  private int id; // Unique ID for this annotation
  @SerializedName("start_offset")
  private int startOffset; // Offset of the start of the annotation
  @SerializedName("stop_offset")
  private int stopOffset; // Offset of the end of the annotation
  private HashMap<String, Object> details; // Details about the annotation
  private HashMap<String, String> citations; // Details about the code citations, only used in RAI annotations(chat,

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getStartOffset() {
    return startOffset;
  }

  public void setStartOffset(int startOffset) {
    this.startOffset = startOffset;
  }

  public int getStopOffset() {
    return stopOffset;
  }

  public void setStopOffset(int stopOffset) {
    this.stopOffset = stopOffset;
  }

  public HashMap<String, Object> getDetails() {
    return details;
  }

  public void setDetails(HashMap<String, Object> details) {
    this.details = details;
  }

  public HashMap<String, String> getCitations() {
    return citations;
  }

  public void setCitations(HashMap<String, String> citations) {
    this.citations = citations;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, startOffset, stopOffset, details, citations);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    CopilotAnnotation that = (CopilotAnnotation) obj;
    return id == that.id && startOffset == that.startOffset && stopOffset == that.stopOffset
        && Objects.equals(details, that.details) && Objects.equals(citations, that.citations);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("id", id);
    builder.append("startOffset", startOffset);
    builder.append("stopOffset", stopOffset);
    builder.append("details", details);
    builder.append("citations", citations);
    return builder.toString();
  }
}
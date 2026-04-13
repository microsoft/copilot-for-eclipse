package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.core.resources.IFile;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.microsoft.copilot.eclipse.core.utils.FileUtils;

/**
 * Parameters for a file reference request.
 */
public class FileChatReference implements ChatReference {
  private String type = ReferenceType.file.toString();
  private String status;
  private Range range;
  private String uri = "";
  private Position position;
  private Range visibleRange;
  private Range selection;
  private String openedAt;
  private String activeAt;

  /**
   * Creates a new FileReferenceParams.
   *
   * @param file The file for which the reference is created.
   */
  public FileChatReference(IFile file) {
    this.uri = FileUtils.getResourceUri(file);
  }

  public String getType() {
    return type;
  }

  public String getStatus() {
    return status;
  }

  public Range getRange() {
    return range;
  }

  public String getUri() {
    return uri;
  }

  public Position getPosition() {
    return position;
  }

  public Range getVisibleRange() {
    return visibleRange;
  }

  public Range getSelection() {
    return selection;
  }

  public String getOpenedAt() {
    return openedAt;
  }

  public String getActiveAt() {
    return activeAt;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setRange(Range range) {
    this.range = range;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public void setVisibleRange(Range visibleRange) {
    this.visibleRange = visibleRange;
  }

  public void setSelection(Range selection) {
    this.selection = selection;
  }

  public void setOpenedAt(String openedAt) {
    this.openedAt = openedAt;
  }

  public void setActiveAt(String activeAt) {
    this.activeAt = activeAt;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, status, range, uri, position, visibleRange, selection, openedAt, activeAt);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileChatReference that = (FileChatReference) o;
    return Objects.equals(type, that.type) && Objects.equals(status, that.status) && Objects.equals(range, that.range)
        && Objects.equals(uri, that.uri) && Objects.equals(position, that.position)
        && Objects.equals(visibleRange, that.visibleRange) && Objects.equals(selection, that.selection)
        && Objects.equals(openedAt, that.openedAt) && Objects.equals(activeAt, that.activeAt);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("type", type);
    builder.append("status", status);
    builder.append("range", range);
    builder.append("uri", uri);
    builder.append("position", position);
    builder.append("visibleRange", visibleRange);
    builder.append("selection", selection);
    builder.append("openedAt", openedAt);
    builder.append("activeAt", activeAt);
    return builder.toString();
  }
}

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.net.URI;
import java.util.Objects;

import org.eclipse.core.resources.IFile;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Parameters for a file reference request.
 */
public class FileChatReference implements ChatReference {
  private String type = ReferenceType.FILE.getValue();
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
    URI uri = file.getLocationURI();
    if (uri != null) {
      this.uri = uri.toString();
    }
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
    builder.add("type", type);
    builder.add("status", status);
    builder.add("range", range);
    builder.add("uri", uri);
    builder.add("position", position);
    builder.add("visibleRange", visibleRange);
    builder.add("selection", selection);
    builder.add("openedAt", openedAt);
    builder.add("activeAt", activeAt);
    return builder.toString();
  }
}

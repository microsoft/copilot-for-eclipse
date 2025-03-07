package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Result of a conversation context request.
 */
public class ConversationContextResult {
  @NonNull
  String uri;
  Position position;
  Range visibleRange;
  Range selection;
  String openedAt;
  String activeAt;

  /**
   * Creates a new ConversationContextResult.
   */
  public ConversationContextResult(String uri, Position position, String openedAt, String activeAt) {
    this.uri = uri;
    this.position = position;
    this.openedAt = openedAt;
    this.activeAt = activeAt;
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
    return Objects.hash(uri, position, visibleRange, selection, openedAt, activeAt);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConversationContextResult that = (ConversationContextResult) o;
    return Objects.equals(uri, that.uri) && Objects.equals(position, that.position)
        && Objects.equals(visibleRange, that.visibleRange) && Objects.equals(selection, that.selection)
        && Objects.equals(openedAt, that.openedAt) && Objects.equals(activeAt, that.activeAt);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("uri", uri);
    builder.add("position", position);
    builder.add("visibleRange", visibleRange);
    builder.add("selection", selection);
    builder.add("openedAt", openedAt);
    builder.add("activeAt", activeAt);
    return builder.toString();
  }
}

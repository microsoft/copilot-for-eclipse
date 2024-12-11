package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Document information for completion.
 */
public class CompletionDocument {

  @NonNull
  private String uri;

  @NonNull
  private Position position;

  private boolean insertSpaces;

  private int tabSize;

  private int version;

  /**
   * Create a new CompletionDocument.
   */
  public CompletionDocument(@NonNull String uri, @NonNull Position position) {
    this.uri = uri;
    this.position = position;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public boolean isInsertSpaces() {
    return insertSpaces;
  }

  public void setInsertSpaces(boolean insertSpaces) {
    this.insertSpaces = insertSpaces;
  }

  public int getTabSize() {
    return tabSize;
  }

  public void setTabSize(int tabSize) {
    this.tabSize = tabSize;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(insertSpaces, position, tabSize, uri, version);
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
    CompletionDocument other = (CompletionDocument) obj;
    return insertSpaces == other.insertSpaces && Objects.equals(position, other.position) && tabSize == other.tabSize
        && Objects.equals(uri, other.uri) && version == other.version;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("uri", uri);
    builder.add("position", position);
    builder.add("insertSpaces", insertSpaces);
    builder.add("tabSize", tabSize);
    builder.add("version", version);
    return builder.toString();
  }

}

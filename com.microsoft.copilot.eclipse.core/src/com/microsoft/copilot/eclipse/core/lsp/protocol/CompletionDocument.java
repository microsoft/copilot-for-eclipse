package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Document information for completion.
 */
public class CompletionDocument extends TextDocumentIdentifier {

  @NonNull
  private Position position;

  private boolean insertSpaces;

  private int tabSize;

  private int version;

  /**
   * Create a new CompletionDocument.
   */
  public CompletionDocument(@NonNull String uri, @NonNull Position position) {
    super(uri);
    this.position = position;
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
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(insertSpaces, position, tabSize, version);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CompletionDocument other = (CompletionDocument) obj;
    return insertSpaces == other.insertSpaces && Objects.equals(position, other.position) && tabSize == other.tabSize
        && version == other.version;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("position", position);
    builder.append("insertSpaces", insertSpaces);
    builder.append("tabSize", tabSize);
    builder.append("version", version);
    builder.append("uri", getUri());
    return builder.toString();
  }

}

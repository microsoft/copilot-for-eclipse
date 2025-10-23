package com.microsoft.copilot.eclipse.core.lsp.protocol;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Params for textDocument/copilotInlineEdit (Next Edit Suggestions request).
 */
public class NextEditSuggestionsParams {

  @NonNull
  private VersionedTextDocumentIdentifier textDocument;

  @NonNull
  private Position position;


  /**
   * Constructor.
   */
  public NextEditSuggestionsParams() {
  }

  /**
   * Constructor with fields.
   */
  public NextEditSuggestionsParams(VersionedTextDocumentIdentifier textDocument, Position position) {
    this.textDocument = textDocument;
    this.position = position;
  }

  public VersionedTextDocumentIdentifier getTextDocument() {
    return textDocument;
  }

  public void setTextDocument(VersionedTextDocumentIdentifier textDocument) {
    this.textDocument = textDocument;
  }

  public Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  @Override
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("textDocument", textDocument);
    b.add("position", position);
    return b.toString();
  }
}

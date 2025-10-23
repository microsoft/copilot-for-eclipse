package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Result for textDocument/copilotInlineEdit. edits: CopilotInlineEdit[]
 */
public class NextEditSuggestionsResult {

  @NonNull
  private List<CopilotInlineEdit> edits;

  public List<CopilotInlineEdit> getEdits() {
    return edits;
  }

  public void setEdits(List<CopilotInlineEdit> edits) {
    this.edits = edits;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).add("edits", edits).toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(edits);
  }

  @Override
  public boolean equals(Object o) {
    return this == o || (o instanceof NextEditSuggestionsResult r && Objects.equals(edits, r.edits));
  }

  /**
   * CopilotInlineEdit: single next edit suggestion.
   */
  public static class CopilotInlineEdit {
    @NonNull
    private String text; // new text
    @NonNull
    private VersionedTextDocumentIdentifier textDocument; // uri + version
    @NonNull
    private Range range; // replacement range
    private Command command;

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

    public VersionedTextDocumentIdentifier getTextDocument() {
      return textDocument;
    }

    public void setTextDocument(VersionedTextDocumentIdentifier textDocument) {
      this.textDocument = textDocument;
    }

    public Range getRange() {
      return range;
    }

    public void setRange(Range range) {
      this.range = range;
    }

    public Command getCommand() {
      return command;
    }

    public void setCommand(Command command) {
      this.command = command;
    }

    /** Convenience for future use (may return null). */
    public String getUuid() {
      if (command == null || command.getArguments() == null || command.getArguments().isEmpty()) {
        return null;
      }
      Object arg0 = command.getArguments().get(0);
      return arg0 == null ? null : arg0.toString();
    }
  }
}

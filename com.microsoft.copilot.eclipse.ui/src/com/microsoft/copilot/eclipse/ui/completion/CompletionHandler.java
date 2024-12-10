package com.microsoft.copilot.eclipse.ui.completion;

import java.io.IOException;
import java.net.URI;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handle completion for an ITextEditor.
 */
public class CompletionHandler implements ITextListener, CaretListener {

  private CopilotLanguageServerConnection lsConnection;
  private ITextEditor editor;
  private ITextViewer textViewer;
  private IDocument document;
  private URI documentUri;
  private int documentVersion;

  /**
   * Creates a new completion handler.
   */
  public CompletionHandler(CopilotLanguageServerConnection lsConnection, ITextEditor editor) {
    this.lsConnection = lsConnection;
    this.editor = editor;
    this.textViewer = (ITextViewer) this.editor.getAdapter(ITextOperationTarget.class);
    this.document = LSPEclipseUtils.getDocument(editor);
    this.documentUri = UiUtils.getUriFromTextEditor(editor);
    this.documentVersion = 0;
    try {
      lsConnection.connectDocument(this.document);
    } catch (IOException e) {
      // TODO: log & send telemetry
      return;
    }
    registerListeners();
  }

  @Override
  public void caretMoved(CaretEvent event) {
    // it's guaranteed that the document change event comes earlier than caret
    // change event. See org.eclipse.swt.custom.StyledText#modifyContent()
    int currentVersion = this.lsConnection.getDocumentVersion(this.documentUri);
    if (currentVersion == this.documentVersion) {
      // if the caret position is changed without document version change, we should remove the ghost text.
      // TODO: remove ghost text
    } else {
      this.documentVersion = currentVersion;
      // TODO: trigger completion
    }

  }

  @Override
  public void textChanged(TextEvent event) {
    // this event comes earlier than caret change event. So we should check if the typed characters
    // are the same as the ghost. Then determine whether a re-redering or a new completion
    // request is needed.
    // TODO: check changed text
  }

  /**
   * Disposes the resources of this completion handler.
   */
  public void dispose() {
    lsConnection.disconnectDocument(this.documentUri);
    if (this.textViewer != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        this.textViewer.getTextWidget().removeCaretListener(this);
        this.textViewer.removeTextListener(this);
      });
    }

  }

  void registerListeners() {
    // if the text viewer is null, we will not register listeners.
    // the side effect is that the completion will not be triggered for this editor.
    if (this.textViewer == null) {
      // TODO: log & send telemetry
      return;
    }

    SwtUtils.invokeOnDisplayThread(() -> {
      this.textViewer.getTextWidget().addCaretListener(this);
      this.textViewer.addTextListener(this);
    });
  }

}

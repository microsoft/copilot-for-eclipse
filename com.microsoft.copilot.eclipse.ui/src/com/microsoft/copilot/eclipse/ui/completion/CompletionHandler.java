package com.microsoft.copilot.eclipse.ui.completion;

import java.io.IOException;
import java.net.URI;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.completion.CompletionCollection;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A class to listen events which are completion related and notify the completion manager to render the ghost text or
 * apply the suggestion to document.
 */
public class CompletionHandler implements ITextListener, CaretListener {

  private CopilotLanguageServerConnection lsConnection;
  private CompletionProvider provider;
  private ITextViewer textViewer;
  private IDocument document;
  private URI documentUri;
  private int documentVersion;
  private org.eclipse.jface.text.Position triggerPosition;

  private DefaultPositionUpdater positionUpdater;
  private CompletionManager completionManager;

  /**
   * Creates a new completion handler.
   */
  public CompletionHandler(CopilotLanguageServerConnection lsConnection, CompletionProvider provider,
      ITextEditor editor) {
    this.lsConnection = lsConnection;
    this.textViewer = (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
    // if the text viewer is null, we will not register listeners.
    // the side effect is that the completion will not be triggered for this editor.
    if (textViewer == null) {
      // TODO: log & send telemetry
      return;
    }
    this.document = LSPEclipseUtils.getDocument(editor);
    if (this.document == null) {
      // TODO: log & send telemetry
      return;
    }
    this.documentUri = LSPEclipseUtils.toUri(document);
    if (this.documentUri == null) {
      // TODO: log & send telemetry
      return;
    }
    try {
      lsConnection.connectDocument(this.document);
    } catch (IOException e) {
      // TODO: log & send telemetry
      return;
    }
    this.documentVersion = -1;
    this.triggerPosition = new org.eclipse.jface.text.Position(0);
    this.completionManager = new CompletionManager(lsConnection, provider, this.textViewer, this.document,
        this.documentUri);
    registerListeners();

    // position updater is used to update the position when the document is changed.
    // this is needed because the completion ghost text is rendered based on the
    // position in the document. If the document is changed, the position will be
    // invalidated.
    this.positionUpdater = new DefaultPositionUpdater(this.getCategory());
    this.document.addPositionCategory(this.getCategory());
    this.document.addPositionUpdater(this.positionUpdater);
  }

  /**
   * Check if the completion handler has any completion suggestions.
   */
  public boolean hasCompletion() {
    return this.completionManager.hasCompletion();
  }

  /**
   * Accept the full completion suggestion.
   */
  public void acceptFullSuggestion() {
    try {
      this.document.addPosition(this.triggerPosition);
      this.completionManager.acceptSuggestion();
      this.document.removePosition(this.triggerPosition);
    } catch (BadLocationException e) {
      // TODO: log & send telemetry
      return;
    }
    this.clearCompletionRendering();
    SwtUtils.invokeOnDisplayThread(() -> {
      this.textViewer.getSelectionProvider().setSelection(new TextSelection(this.triggerPosition.offset, 0));
    }, this.textViewer.getTextWidget());
  }

  void registerListeners() {
    SwtUtils.invokeOnDisplayThread(() -> {
      this.textViewer.getTextWidget().addCaretListener(this);
      this.textViewer.addTextListener(this);
    });
  }

  /**
   * Trigger the inline completion.
   */
  public void triggerCompletion() {
    clearCompletionRendering();
    this.completionManager.triggerCompletion(this.triggerPosition, this.documentVersion);
  }

  /**
   * Clear the completion ghost text.
   */
  public void clearCompletionRendering() {
    this.completionManager.clearGhostText(this.triggerPosition);
  }

  public CompletionCollection getCompletions() {
    return this.completionManager.getCompletions();
  }

  @Override
  public void caretMoved(CaretEvent event) {
    int caretOffset = UiUtils.getCaretOffset(this.textViewer);
    this.triggerPosition = new org.eclipse.jface.text.Position(caretOffset);

    // it's guaranteed that the document change event comes earlier than caret
    // change event. See org.eclipse.swt.custom.StyledText#modifyContent()
    int currentVersion = this.lsConnection.getDocumentVersion(this.documentUri);

    // initialize the document version and return. This avoids the ghost text
    // being rendered when user opens the editor and just clicks in it.
    if (this.documentVersion < 0) {
      this.documentVersion = currentVersion;
      return;
    }
    if (currentVersion == this.documentVersion) {
      // if the caret position is changed without document version change, we should remove the ghost text.
      clearCompletionRendering();
    } else {
      this.documentVersion = currentVersion;
      triggerCompletion();
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
   * Get category for the position updater of this document.
   */
  private String getCategory() {
    return "GCE-" + this.documentUri.toASCIIString();
  }

  /**
   * Disposes the resources of this completion handler.
   */
  public void dispose() {
    if (this.completionManager != null) {
      this.completionManager.dispose();
      this.completionManager = null;
    }
    lsConnection.disconnectDocument(this.documentUri);
    try {
      this.document.removePositionCategory(this.getCategory());
    } catch (BadPositionCategoryException e) {
      // TODO: log & send telemetry
    }
    this.document.removePositionUpdater(this.positionUpdater);
    if (this.textViewer != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        if (this.textViewer.getTextWidget() != null) {
          this.textViewer.getTextWidget().removeCaretListener(this);
        }
        this.textViewer.removeTextListener(this);
      });
    }

  }

}

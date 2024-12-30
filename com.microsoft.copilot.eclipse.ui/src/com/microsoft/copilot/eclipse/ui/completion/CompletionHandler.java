package com.microsoft.copilot.eclipse.ui.completion;

import java.io.IOException;
import java.net.URI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.completion.CompletionCollection;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A class to listen events which are completion related and notify the completion manager to render the ghost text or
 * apply the suggestion to document.
 */
public class CompletionHandler implements CaretListener, IPropertyChangeListener {

  private CopilotLanguageServerConnection lsConnection;
  private CompletionProvider provider;
  private ITextViewer textViewer;
  private IDocument document;
  private URI documentUri;
  private int documentVersion;
  private org.eclipse.jface.text.Position triggerPosition;

  private DefaultPositionUpdater positionUpdater;
  private CompletionManager completionManager;
  private boolean autoShowCompletion;
  private IPreferenceStore preferenceStore;

  /**
   * Creates a new completion handler.
   */
  public CompletionHandler(CopilotLanguageServerConnection lsConnection, CompletionProvider provider,
      ITextEditor editor, IPreferenceStore preferenceStore) {
    this.lsConnection = lsConnection;
    this.textViewer = (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
    // if the text viewer is null, we will not register listeners.
    // the side effect is that the completion will not be triggered for this editor.
    if (textViewer == null) {
      CopilotUi.LOGGER.log(LogLevel.INFO, "Text viewer is null for editor: " + editor.getTitle());
      return;
    }
    this.document = LSPEclipseUtils.getDocument(editor);
    if (this.document == null) {
      CopilotUi.LOGGER.log(LogLevel.INFO, "Document is null for editor: " + editor.getTitle());
      return;
    }
    this.documentUri = LSPEclipseUtils.toUri(document);
    if (this.documentUri == null) {
      CopilotUi.LOGGER.log(LogLevel.INFO, "Document URI is null for editor: " + editor.getTitle());
      return;
    }
    try {
      lsConnection.connectDocument(this.document);
    } catch (IOException e) {
      CopilotUi.LOGGER.log(LogLevel.ERROR, e);
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

    // initialize the auto show completion preference and add listener to update it.
    this.preferenceStore = preferenceStore;
    this.autoShowCompletion = preferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION);
    preferenceStore.addPropertyChangeListener(this);
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
      CopilotUi.LOGGER.log(LogLevel.ERROR, e);
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
    this.completionManager.clearGhostText();
  }

  public CompletionCollection getCompletions() {
    return this.completionManager.getCompletions();
  }

  @Override
  public void caretMoved(CaretEvent event) {
    int modelOffset = UiUtils.widgetOffset2ModelOffset(textViewer, event.caretOffset);
    this.triggerPosition = new org.eclipse.jface.text.Position(modelOffset);

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
      if (this.autoShowCompletion) {
        triggerCompletion();
      }
    }

  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().equals(Constants.AUTO_SHOW_COMPLETION)) {
      this.autoShowCompletion = Boolean.parseBoolean(event.getNewValue().toString());
    }
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
    if (this.completionManager == null) {
      // null manager means the handler is not initialized.
      return;
    }

    this.completionManager.dispose();
    this.completionManager = null;
    preferenceStore.removePropertyChangeListener(this);
    lsConnection.disconnectDocument(this.documentUri);
    try {
      this.document.removePositionCategory(this.getCategory());
    } catch (BadPositionCategoryException e) {
      CopilotUi.LOGGER.log(LogLevel.ERROR, e);
    }
    this.document.removePositionUpdater(this.positionUpdater);
    SwtUtils.invokeOnDisplayThread(() -> {
      if (this.textViewer.getTextWidget() != null) {
        this.textViewer.getTextWidget().removeCaretListener(this);
      }
    });

  }

}

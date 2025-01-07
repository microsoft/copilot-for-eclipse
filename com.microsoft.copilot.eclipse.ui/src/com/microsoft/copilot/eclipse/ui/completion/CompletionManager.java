package com.microsoft.copilot.eclipse.ui.completion;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.completion.CompletionCollection;
import com.microsoft.copilot.eclipse.core.completion.CompletionListener;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyShownParams;
import com.microsoft.copilot.eclipse.ui.completion.codemining.BlockGhostText;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A class to listen events which are completion related and notify the completion manager to render the ghost text or
 * apply the suggestion to document.
 */
public class CompletionManager implements CaretListener, CompletionListener, IPropertyChangeListener {

  private CopilotLanguageServerConnection lsConnection;
  private CompletionProvider provider;
  private CompletionCollection completions;
  private ITextViewer textViewer;
  private StyledText styledText;
  private IDocument document;
  private URI documentUri;
  private int documentVersion;
  private org.eclipse.jface.text.Position triggerPosition;
  private List<ICodeMining> codeMinings;

  private DefaultPositionUpdater positionUpdater;
  private RenderingManager renderingManager;
  private boolean autoShowCompletion;
  private IPreferenceStore preferenceStore;

  /**
   * Creates a new completion manager. The manager is responsible for trigger the completion, apply suggestions to the
   * document. And schedule the rendering of ghost text.
   */
  public CompletionManager(CopilotLanguageServerConnection lsConnection, CompletionProvider provider,
      ITextEditor editor, IPreferenceStore preferenceStore) {
    this.codeMinings = new ArrayList<>();
    this.textViewer = (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
    // if the text viewer is null, we will not register listeners.
    // the side effect is that the completion will not be triggered for this editor.
    if (textViewer == null) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "Text viewer is null for editor: " + editor.getTitle());
      return;
    }
    this.styledText = this.textViewer.getTextWidget();
    if (this.styledText == null) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "Styled text is null for editor: " + editor.getTitle());
      return;
    }
    this.document = LSPEclipseUtils.getDocument(editor);
    if (this.document == null) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "Document is null for editor: " + editor.getTitle());
      return;
    }
    this.documentUri = LSPEclipseUtils.toUri(document);
    if (this.documentUri == null) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "Document URI is null for editor: " + editor.getTitle());
      return;
    }
    try {
      lsConnection.connectDocument(this.document);
    } catch (IOException e) {
      CopilotCore.LOGGER.log(LogLevel.ERROR, e);
      return;
    }

    this.renderingManager = new RenderingManager(this.textViewer);

    this.lsConnection = lsConnection;
    this.provider = provider;
    this.provider.addCompletionListener(this);
    this.completions = null;
    this.documentVersion = -1;
    this.triggerPosition = new org.eclipse.jface.text.Position(0);

    // initialize the auto show completion preference and add listener to update it.
    this.preferenceStore = preferenceStore;
    this.autoShowCompletion = preferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION);

    // position updater is used to update the position when the document is changed.
    // this is needed because the completion ghost text is rendered based on the
    // position in the document. If the document is changed, the position will be
    // invalidated.
    this.positionUpdater = new DefaultPositionUpdater(this.getCategory());
    this.document.addPositionCategory(this.getCategory());
    this.document.addPositionUpdater(this.positionUpdater);

    registerListeners();
  }

  void registerListeners() {
    SwtUtils.invokeOnDisplayThread(() -> {
      this.styledText.addCaretListener(this);
    }, this.styledText);

    this.preferenceStore.addPropertyChangeListener(this);
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
  public void onCompletionResolved(CompletionCollection completions) {
    if (!Objects.equals(completions.getUriString(), this.documentUri.toASCIIString())) {
      return;
    }

    if (completions.getDocumentVersion() != this.lsConnection.getDocumentVersion(this.documentUri)) {
      return;
    }

    this.completions = completions;

    // render the first line by ourself to make sure cursor position not change.
    List<GhostText> ghostTexts = resolveGhostTexts();
    if (!ghostTexts.isEmpty()) {
      this.renderingManager.setGhostTexts(ghostTexts);
      this.renderingManager.redraw();
      this.notifyShown();
    }

    // render the remaining lines by code mining api.
    resolveCodeMiningGhostTexts();
    this.updateCodeMinings();
  }

  private List<GhostText> resolveGhostTexts() {
    if (this.completions == null || this.completions.getSize() == 0) {
      return Collections.emptyList();
    }
    List<GhostText> ghostTexts = new ArrayList<>();
    String firstLine = this.completions.getFirstLine();
    if (StringUtils.isNotBlank(firstLine)) {
      ghostTexts.add(new EolGhostText(firstLine, triggerPosition.getOffset()));
    }
    return ghostTexts;
  }

  private void resolveCodeMiningGhostTexts() {
    if (this.completions == null || this.completions.getSize() == 0) {
      this.codeMinings.clear();
      return;
    }
    List<ICodeMining> cm = new ArrayList<>();
    String remainingLines = this.completions.getRemainingLines();
    if (StringUtils.isNotEmpty(remainingLines)) {
      try {
        cm.add(
            new BlockGhostText(document.getLineOfOffset(triggerPosition.offset) + 1, document, null, remainingLines));
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.log(LogLevel.ERROR, e);
      }
    }
    this.codeMinings = cm;
  }

  private void updateCodeMinings() {
    if (textViewer instanceof ISourceViewerExtension5 sve) {
      sve.updateCodeMinings();
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().equals(Constants.AUTO_SHOW_COMPLETION)) {
      this.autoShowCompletion = Boolean.parseBoolean(event.getNewValue().toString());
    }
  }

  /**
   * Trigger the inline completion.
   */
  public void triggerCompletion() {
    clearCompletionRendering();
    try {
      this.provider.triggerCompletion(documentUri.toASCIIString(),
          LSPEclipseUtils.toPosition(this.triggerPosition.getOffset(), this.document), documentVersion);
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.log(LogLevel.ERROR, e);
    }
  }

  /**
   * Clear the completion ghost text.
   */
  public void clearCompletionRendering() {
    this.codeMinings.clear();
    this.updateCodeMinings();

    this.renderingManager.clearGhostText();
    this.completions = null;
  }

  /**
   * Accept the full completion suggestion.
   */
  public void acceptFullSuggestion() {
    try {
      this.document.addPosition(this.triggerPosition);
      this.acceptSuggestion();
      this.document.removePosition(this.triggerPosition);
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.log(LogLevel.ERROR, e);
      return;
    }
    this.clearCompletionRendering();
    SwtUtils.invokeOnDisplayThread(() -> {
      this.textViewer.getSelectionProvider().setSelection(new TextSelection(this.triggerPosition.offset, 0));
    }, this.textViewer.getTextWidget());
  }

  /**
   * Apply the completion suggestion to document.
   *
   * @throws BadLocationException if the offset is invalid.
   */
  void acceptSuggestion() throws BadLocationException {
    if (this.completions == null || this.completions.getSize() == 0) {
      return;
    }
    int startOffset = LSPEclipseUtils.toOffset(this.completions.getTriggerPosition(), this.document);
    String text = this.completions.getText();
    if (StringUtils.isEmpty(text)) {
      return;
    }
    int endOffset = LSPEclipseUtils.toOffset(this.completions.getRange().getEnd(), this.document);
    this.document.replace(startOffset, endOffset - startOffset, text);
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
    if (this.provider != null) {
      this.provider.removeCompletionListener(this);
    }
    if (this.renderingManager != null) {
      this.renderingManager.dispose();
      this.renderingManager = null;
    }

    if (this.preferenceStore != null) {
      preferenceStore.removePropertyChangeListener(this);
    }

    if (this.lsConnection != null) {
      this.lsConnection.disconnectDocument(this.documentUri);
    }

    if (this.document != null) {
      try {
        this.document.removePositionCategory(this.getCategory());
      } catch (BadPositionCategoryException e) {
        CopilotCore.LOGGER.log(LogLevel.ERROR, e);
      }
      this.document.removePositionUpdater(this.positionUpdater);
    }

    if (this.styledText != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        this.styledText.removeCaretListener(this);
      }, this.styledText);
    }
  }

  /**
   * Will be used when notifying the completion rejection/acceptance.
   */
  public CompletionCollection getCompletions() {
    return this.completions;
  }

  /**
   * Check if the completion handler has any completion suggestions.
   */
  public boolean hasCompletion() {
    return this.completions != null;
  }

  private void notifyShown() {
    if (this.completions == null || this.completions.getSize() == 0) {
      return;
    }

    CompletionItem item = this.completions.getCurrentItem();
    if (item == null) {
      return;
    }

    NotifyShownParams params = new NotifyShownParams(item.getUuid());
    this.lsConnection.notifyShown(params);
  }

  public List<ICodeMining> getCodeMinings() {
    return codeMinings;
  }

}

package com.microsoft.copilot.eclipse.ui.completion;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextEvent;
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
import com.microsoft.copilot.eclipse.core.completion.AcceptSuggestionType;
import com.microsoft.copilot.eclipse.core.completion.CompletionListener;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.completion.SuggestionUpdateManager;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyShownParams;
import com.microsoft.copilot.eclipse.ui.completion.codemining.BlockGhostText;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.CompletionUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A class to listen events which are completion related and notify the completion manager to render the ghost text or
 * apply the suggestion to document.
 */
public class CompletionManager implements CaretListener, ITextListener, CompletionListener, IPropertyChangeListener {

  private CopilotLanguageServerConnection lsConnection;
  private CompletionProvider provider;
  private SuggestionUpdateManager suggestionUpdateManager;
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
  private LanguageServerSettingManager settingsManager;

  /**
   * Creates a new completion manager. The manager is responsible for trigger the completion, apply suggestions to the
   * document. And schedule the rendering of ghost text.
   */
  public CompletionManager(CopilotLanguageServerConnection lsConnection, CompletionProvider provider,
      ITextEditor editor, LanguageServerSettingManager settingsManager) {
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
    IFile file = LSPEclipseUtils.getFile(document);
    if (file == null || !file.exists()) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "File is null or removed for editor: " + editor.getTitle());
      return;
    }
    this.documentUri = LSPEclipseUtils.toUri(document);
    if (this.documentUri == null) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "Document URI is null for editor: " + editor.getTitle());
      return;
    }
    this.suggestionUpdateManager = new SuggestionUpdateManager(this.document);
    try {
      lsConnection.connectDocument(this.document);
    } catch (IOException e) {
      CopilotCore.LOGGER.log(LogLevel.ERROR, e);
      return;
    }

    this.renderingManager = new RenderingManager(this.textViewer);

    this.lsConnection = lsConnection;
    this.settingsManager = settingsManager;
    this.provider = provider;
    this.provider.addCompletionListener(this);
    this.documentVersion = -1;
    this.triggerPosition = new org.eclipse.jface.text.Position(0);

    // initialize the auto show completion preference and add listener to update it.
    this.autoShowCompletion = settingsManager.getSettings().isEnableAutoCompletions();

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
      this.textViewer.addTextListener(this);
    }, this.styledText);

    this.settingsManager.registerPropertyChangeListener(this);
  }

  /**
   * {@inheritDoc} Listen to the text change event and update the ghost text if the change is still among the part of
   * the completion.
   */
  @Override
  public void textChanged(TextEvent event) {
    DocumentEvent documentEvent = event.getDocumentEvent();
    if (documentEvent == null) {
      return;
    }

    int modelOffset = UiUtils.widgetOffset2ModelOffset(textViewer, event.getOffset());
    if (isReplacement(event)) {
      this.triggerPosition = new org.eclipse.jface.text.Position(modelOffset + event.getText().length());
      clearCompletionRendering();
      this.suggestionUpdateManager.reset();
    } else if (isDeletion(event)) {
      this.triggerPosition = new org.eclipse.jface.text.Position(modelOffset);
      if (this.suggestionUpdateManager.getSize() > 0
          && this.suggestionUpdateManager.delete(event.getReplacedText().length())) {
        this.updateGhostTexts();
      } else {
        clearCompletionRendering();
        this.suggestionUpdateManager.reset();
      }
    } else if (isInsertion(event)) {
      this.triggerPosition = new org.eclipse.jface.text.Position(modelOffset + event.getText().length());
      if (this.suggestionUpdateManager.getSize() > 0 && this.suggestionUpdateManager.insert(event.getText())) {
        this.updateGhostTexts();
      } else {
        clearCompletionRendering();
        this.suggestionUpdateManager.reset();
      }
    }
  }

  /**
   * Return if the event contains deletion action. Please note that both pure deletion and replacement contain deletion
   * action.
   */
  private boolean isDeletion(TextEvent event) {
    return StringUtils.isNotEmpty(event.getReplacedText()) && StringUtils.isEmpty(event.getText());
  }

  private boolean isReplacement(TextEvent event) {
    return StringUtils.isNotEmpty(event.getReplacedText()) && StringUtils.isNotEmpty(event.getText());
  }

  private boolean isInsertion(TextEvent event) {
    return StringUtils.isEmpty(event.getReplacedText()) && StringUtils.isNotEmpty(event.getText());
  }

  /**
   * {@inheritDoc} Listen to the caret move event and update the cached document version. A completion will be triggered
   * if the document version is changed.
   */
  @Override
  public void caretMoved(CaretEvent event) {
    // it's guaranteed that the document change event comes earlier than caret
    // change event. See org.eclipse.swt.custom.StyledText#modifyContent()
    int currentVersion = this.lsConnection.getDocumentVersion(this.documentUri);

    // initialize the document version and return. This avoids the ghost text
    // being rendered when user opens the editor and just clicks in it.
    if (this.documentVersion < 0) {
      this.documentVersion = currentVersion;
      return;
    }

    // always update the trigger position for caret move event to makes sure trigger position is correct.
    // Sometimes, for example, deleting, the caret event comes before the text changed event.
    int modelOffset = UiUtils.widgetOffset2ModelOffset(textViewer, event.caretOffset);
    this.triggerPosition = new org.eclipse.jface.text.Position(modelOffset);
    if (currentVersion == this.documentVersion) {
      // if the caret position is changed without document version change, we should remove the ghost text.
      clearCompletionRendering();
    } else {
      this.documentVersion = currentVersion;
      // only automatically trigger completion when there is no suggestion available.
      // Which means the characters user typed are not part of the completion.
      if (this.autoShowCompletion && this.suggestionUpdateManager.getSize() == 0) {
        triggerCompletion();
      }
    }

  }

  @Override
  public void onCompletionResolved(String uriString, List<CompletionItem> completions) {
    if (!Objects.equals(uriString, this.documentUri.toASCIIString())) {
      return;
    }

    if (completions.isEmpty()) {
      return;
    }

    if (completions.get(0).getDocVersion() != this.lsConnection.getDocumentVersion(this.documentUri)) {
      return;
    }

    this.suggestionUpdateManager.setCompletionItems(completions);
    this.updateGhostTexts();
    this.notifyShown();
  }

  private void updateGhostTexts() {
    // render the first line by ourself to make sure cursor position not change.
    List<GhostText> ghostTexts = resolveGhostTexts();
    this.renderingManager.setGhostTexts(ghostTexts);
    this.renderingManager.redraw();

    // render the remaining lines by code mining api.
    resolveCodeMiningGhostTexts();
    this.updateCodeMinings();
  }

  private List<GhostText> resolveGhostTexts() {
    if (this.suggestionUpdateManager.getSize() == 0) {
      return Collections.emptyList();
    }

    String firstLine = this.suggestionUpdateManager.getFirstLine();
    if (StringUtils.isNotEmpty(firstLine)) {
      String documentContent = this.document.get();
      int triggerOffset = triggerPosition.getOffset();
      String documentLine = "";
      for (int i = triggerOffset; i < this.document.getLength(); i++) {
        if (documentContent.charAt(i) == '\n') {
          documentLine = documentContent.substring(triggerOffset, i);
          break;
        }
      }
      return CompletionUtils.getGhostTexts(documentLine, firstLine, triggerOffset);
    }

    return Collections.emptyList();
  }

  private void resolveCodeMiningGhostTexts() {
    if (this.suggestionUpdateManager.getSize() == 0) {
      this.codeMinings.clear();
      return;
    }
    List<ICodeMining> cm = new ArrayList<>();
    String remainingLines = this.suggestionUpdateManager.getRemainingLines();
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
    try {
      IFile file = LSPEclipseUtils.getFile(document);
      this.provider.triggerCompletion(file, LSPEclipseUtils.toPosition(this.triggerPosition.getOffset(), this.document),
          documentVersion);
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.log(LogLevel.ERROR, e);
    }
  }

  /**
   * Clear the completion ghost text.
   */
  public void clearCompletionRendering() {
    this.suggestionUpdateManager.reset();
    this.codeMinings.clear();
    this.updateCodeMinings();

    this.renderingManager.clearGhostText();
  }

  /**
   * Accept completion suggestion.
   */
  public void acceptSuggestion(AcceptSuggestionType type) {
    try {
      this.document.addPosition(this.triggerPosition);
      switch (type) {
        case FULL:
          this.acceptEntireSuggestion();
          break;
        default:
          break;
      }
      this.document.removePosition(this.triggerPosition);
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.log(LogLevel.ERROR, e);
      return;
    }
    SwtUtils.invokeOnDisplayThread(() -> {
      this.textViewer.getSelectionProvider().setSelection(new TextSelection(this.triggerPosition.offset, 0));
    }, this.textViewer.getTextWidget());
  }

  /**
   * Apply the entire completion suggestion to document.
   *
   * @throws BadLocationException if the offset is invalid.
   */
  void acceptEntireSuggestion() throws BadLocationException {
    CompletionItem item = this.suggestionUpdateManager.getCurrentItem();
    if (item == null) {
      return;
    }
    int startOffset = LSPEclipseUtils.toOffset(item.getPosition(), this.document);
    String text = this.suggestionUpdateManager.getText();
    if (StringUtils.isEmpty(text)) {
      return;
    }
    int endOffset = LSPEclipseUtils.toOffset(item.getRange().getEnd(), this.document);
    this.document.replace(startOffset, endOffset - startOffset, text);

    this.clearCompletionRendering();
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

    if (this.settingsManager != null) {
      this.settingsManager.unregisterPropertyChangeListener(this);
    }

    if (this.lsConnection != null && this.documentUri != null) {
      this.lsConnection.disconnectDocument(this.documentUri);
    }

    if (this.document != null && this.documentUri != null) {
      try {
        this.document.removePositionCategory(this.getCategory());
      } catch (BadPositionCategoryException e) {
        CopilotCore.LOGGER.log(LogLevel.ERROR, e);
      }
      this.document.removePositionUpdater(this.positionUpdater);
    }

    if (this.textViewer != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        this.textViewer.removeTextListener(this);
      });
    }

    if (this.styledText != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        this.styledText.removeCaretListener(this);
      }, this.styledText);
    }
  }

  public SuggestionUpdateManager getSuggestionUpdateManager() {
    return suggestionUpdateManager;
  }

  private void notifyShown() {
    if (this.suggestionUpdateManager.getSize() == 0) {
      return;
    }

    CompletionItem item = this.suggestionUpdateManager.getCurrentItem();
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

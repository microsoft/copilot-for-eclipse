package com.microsoft.copilot.eclipse.ui.completion;

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
import org.eclipse.jface.text.ITextInputListener;
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
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.history.IRefactoringExecutionListener;
import org.eclipse.ltk.core.refactoring.history.RefactoringExecutionEvent;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.completion.AcceptSuggestionType;
import com.microsoft.copilot.eclipse.core.completion.CompletionListener;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.completion.SuggestionUpdateManager;
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
public class CompletionManager implements CaretListener, ITextListener, CompletionListener, IPropertyChangeListener,
    ITextInputListener, IRefactoringExecutionListener {

  private static final String COMPLETION_CONTEXT = "com.microsoft.copilot.eclipse.completionAvailableContext";
  private static IContextActivation completionContextActivation;

  private CopilotLanguageServerConnection lsConnection;
  private CompletionProvider provider;
  private SuggestionUpdateManager suggestionUpdateManager;
  private ITextViewer textViewer;
  private StyledText styledText;
  private String editorTitle;
  private IDocument document;
  private URI documentUri;
  private int documentVersion;
  private org.eclipse.jface.text.Position triggerPosition;
  private List<ICodeMining> codeMinings;
  private int cachedModelOffset;

  private DefaultPositionUpdater positionUpdater;
  private RenderingManager renderingManager;
  private boolean isRefactoring;
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
    this.editorTitle = editor.getTitle();
    // if the text viewer is null, we will not register listeners.
    // the side effect is that the completion will not be triggered for this editor.
    if (textViewer == null) {
      CopilotCore.LOGGER.info("Text viewer is null for editor: " + this.editorTitle);
      return;
    }
    this.styledText = this.textViewer.getTextWidget();
    if (this.styledText == null) {
      CopilotCore.LOGGER.info("Styled text is null for editor: " + this.editorTitle);
      return;
    }
    this.lsConnection = lsConnection;
    this.document = LSPEclipseUtils.getDocument(editor);
    if (!initializeDocument()) {
      return;
    }

    RefactoringCore.getHistoryService().addExecutionListener(this);
    this.renderingManager = new RenderingManager(this.textViewer);
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

    // Cache the model offset to clear line vertical offset when the line is out of the visible range.
    // We cache the model offset here because the caret offset won't update when code blocks are collapsed.
    SwtUtils.invokeOnDisplayThread(() -> {
      this.cachedModelOffset = UiUtils.widgetOffset2ModelOffset(textViewer, this.styledText.getCaretOffset());
    }, this.styledText);

    registerListeners();
  }

  private boolean initializeDocument() {
    if (this.document == null) {
      CopilotCore.LOGGER.info("Document is null for editor: " + this.editorTitle);
      return false;
    }
    IFile file = LSPEclipseUtils.getFile(document);
    if (file == null || !file.exists()) {
      CopilotCore.LOGGER.info("File is null or removed for editor: " + this.editorTitle);
      return false;
    }
    this.documentUri = LSPEclipseUtils.toUri(document);
    if (this.documentUri == null) {
      CopilotCore.LOGGER.info("Document URI is null for editor: " + this.editorTitle);
      return false;
    }
    this.suggestionUpdateManager = new SuggestionUpdateManager(this.document);
    return true;
  }

  private void registerListeners() {
    SwtUtils.invokeOnDisplayThread(() -> {
      this.styledText.addCaretListener(this);
      this.textViewer.addTextListener(this);
      this.textViewer.addTextInputListener(this);
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

    this.cachedModelOffset = UiUtils.widgetOffset2ModelOffset(textViewer, event.getOffset());
    if (isReplacement(event)) {
      this.triggerPosition = new org.eclipse.jface.text.Position(this.cachedModelOffset + event.getText().length());
      clearCompletionRendering();
    } else if (isDeletion(event)) {
      this.triggerPosition = new org.eclipse.jface.text.Position(this.cachedModelOffset);
      if (this.suggestionUpdateManager.getSize() > 0
          && this.suggestionUpdateManager.delete(event.getReplacedText().length())) {
        this.updateGhostTexts();
      } else {
        clearCompletionRendering();
      }
    } else if (isInsertion(event)) {
      this.triggerPosition = new org.eclipse.jface.text.Position(this.cachedModelOffset + event.getText().length());
      if (this.suggestionUpdateManager.getSize() > 0 && this.suggestionUpdateManager.insert(event.getText())) {
        this.updateGhostTexts();
      } else {
        clearCompletionRendering();
      }
    }
  }

  /**
   * {@inheritDoc} Listen to the refactoring event and log the refactoring event type.
   */
  @Override
  public void executionNotification(RefactoringExecutionEvent event) {
    int eventType = event.getEventType();
    switch (eventType) {
      case RefactoringExecutionEvent.ABOUT_TO_PERFORM:
      case RefactoringExecutionEvent.ABOUT_TO_REDO:
      case RefactoringExecutionEvent.ABOUT_TO_UNDO:
        isRefactoring = true;
        break;
      case RefactoringExecutionEvent.PERFORMED:
      case RefactoringExecutionEvent.REDONE:
      case RefactoringExecutionEvent.UNDONE:
        isRefactoring = false;
        break;
      default:
        isRefactoring = false;
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
      // Though the suggestionUpdateManager will update the items according to the text change, but that is not always
      // correct. Thus we will always trigger another completion, whenever cursor position changed, to get the correct
      // items. This will not affect the ghost text rendering because the CLS also has cache so it will not return items
      // that are different from the last time as long as the text change is the same as the original completion item.
      if (this.autoShowCompletion && !isRefactoring) {
        triggerCompletion();
      }
    }

    // Redraw the block ghost text line at both old and new offsets to fix legacy vertical indentation issues when text
    // is outside the visible range or code blocks are collapsed.
    // Fix issue: https://github.com/microsoft/copilot-eclipse/issues/105
    // Fix issue: https://github.com/microsoft/copilot-eclipse/issues/137
    if (modelOffset != this.cachedModelOffset) {
      // The collapsed code block will cause the model offset to flicker, so we need to redraw the block ghost text line
      // at both old and new offsets.
      SwtUtils.redrawBlockLineAtModelOffset(textViewer, this.cachedModelOffset);
      SwtUtils.redrawBlockLineAtModelOffset(textViewer, modelOffset);
      this.cachedModelOffset = modelOffset;
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
    enableContext();
    this.updateGhostTexts();
    this.notifyShown();
  }

  @Override
  public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    this.document = newInput;
    initializeDocument();
    CopilotCore.LOGGER.info("Completion handler is refreshed for the document: " + this.documentUri);
  }

  @Override
  public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
    // do nothing
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

    List<GhostText> ghostTexts = new ArrayList<>();

    String firstLine = this.suggestionUpdateManager.getFirstLine();
    if (StringUtils.isNotEmpty(firstLine)) {
      String documentContent = this.document.get();
      int triggerOffset = triggerPosition.getOffset();
      String documentLine = "";
      try {
        int lineOffset = document.getLineOfOffset(triggerOffset);
        if (lineOffset == document.getNumberOfLines() - 1) {
          // this is the last line
          documentLine = documentContent.substring(triggerOffset);
        } else {
          for (int i = triggerOffset; i < this.document.getLength(); i++) {
            if (isNewLineCharacter(documentContent, i)) {
              documentLine = documentContent.substring(triggerOffset, i);
              break;
            }
          }
        }
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error(e);
      }
      ghostTexts.addAll(CompletionUtils.getGhostTexts(documentLine, firstLine, triggerOffset));
    }

    try {
      int lineOffset = document.getLineOfOffset(triggerPosition.offset);
      String remainingLines = this.suggestionUpdateManager.getRemainingLines();
      if (lineOffset == document.getNumberOfLines() - 1 && StringUtils.isNotEmpty(remainingLines)) {
        // this is the last line
        ghostTexts.add(new BlockBottomGhostText(remainingLines, triggerPosition.offset, this.document));
        return ghostTexts;
      }
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.error(e);
    }

    return ghostTexts;
  }

  private boolean isNewLineCharacter(String documentContent, int index) {
    char currentChar = documentContent.charAt(index);
    boolean isLineFeed = currentChar == '\n';
    if (isLineFeed) {
      return true;
    } else {
      return currentChar == '\r' && index + 1 < this.document.getLength() && documentContent.charAt(index + 1) == '\n';
    }
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
        int lineOffset = document.getLineOfOffset(triggerPosition.offset) + 1;
        if (lineOffset >= document.getNumberOfLines()) {
          return;
        }
        cm.add(new BlockGhostText(lineOffset, document, null, remainingLines));
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error(e);
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
      CopilotCore.LOGGER.error(e);
    }
  }

  /**
   * Clear the completion ghost text.
   */
  public void clearCompletionRendering() {
    disableContext();
    this.suggestionUpdateManager.reset();
    this.codeMinings.clear();
    this.updateCodeMinings();

    this.renderingManager.clearGhostText();

    // Clear legacy vertical indentation for the block ghost text line when the line is out of the visible range.
    // Fix issue: https://github.com/microsoft/copilot-eclipse/issues/105
    SwtUtils.redrawBlockLineAtModelOffset(textViewer, this.cachedModelOffset);
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
        case NEXT_WORD:
          this.acceptNextWord();
          break;
        default:
          break;
      }
      this.document.removePosition(this.triggerPosition);
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.error(e);
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
  private void acceptEntireSuggestion() throws BadLocationException {
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
   * Apply the next word of the completion suggestion to document.
   */
  private void acceptNextWord() throws BadLocationException {
    CompletionItem item = this.suggestionUpdateManager.getCurrentItem();
    if (item == null) {
      return;
    }
    String nextWord = this.suggestionUpdateManager.getNextWord();
    if (StringUtils.isEmpty(nextWord)) {
      return;
    }
    int startOffset = LSPEclipseUtils.toOffset(item.getPosition(), this.document);
    int length = 0;
    while (length < nextWord.length() && startOffset + length < this.document.getLength()) {
      char c = this.document.getChar(startOffset + length);
      if (c != nextWord.charAt(length)) {
        break;
      }
      length++;
    }
    this.document.replace(startOffset, length, nextWord);
  }

  /**
   * Get category for the position updater of this document.
   */
  private String getCategory() {
    return this.toString();
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

    if (this.document != null && this.documentUri != null) {
      try {
        this.document.removePositionCategory(this.getCategory());
      } catch (BadPositionCategoryException e) {
        CopilotCore.LOGGER.error(e);
      }
      this.document.removePositionUpdater(this.positionUpdater);
    }

    RefactoringCore.getHistoryService().removeExecutionListener(this);

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

  private static void enableContext() {
    if (completionContextActivation == null) {
      IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
      if (contextService != null) {
        completionContextActivation = contextService.activateContext(COMPLETION_CONTEXT);
      }
    }
  }

  private static void disableContext() {
    if (completionContextActivation != null) {
      IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
      if (contextService != null) {
        contextService.deactivateContext(completionContextActivation);
        completionContextActivation = null;
      }
    }
  }
}

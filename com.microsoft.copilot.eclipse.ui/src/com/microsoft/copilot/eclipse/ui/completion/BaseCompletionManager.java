package com.microsoft.copilot.eclipse.ui.completion;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.progress.WorkbenchJob;
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
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.nes.RenderManager;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Abstract base class for completion managers. Provides common functionality for listening to events which are
 * completion related and managing completion suggestions.
 */
public abstract class BaseCompletionManager implements KeyListener, MouseListener, ITextListener, CompletionListener,
    IPropertyChangeListener, ITextInputListener {

  private static final String COMPLETION_CONTEXT = "com.microsoft.copilot.eclipse.completionAvailableContext";
  private static IContextActivation completionContextActivation;

  protected CopilotLanguageServerConnection lsConnection;
  protected CompletionProvider provider;
  protected SuggestionUpdateManager suggestionUpdateManager;
  protected ITextViewer textViewer;
  protected StyledText styledText;
  protected String editorTitle;
  protected IDocument document;
  protected URI documentUri;
  protected int documentVersion;
  protected org.eclipse.jface.text.Position triggerPosition;
  protected List<ICodeMining> codeMinings;
  protected int cachedModelOffset;

  protected DefaultPositionUpdater positionUpdater;
  protected boolean autoShowCompletion;
  // Whether NES suggestions are enabled
  protected boolean enableNes;
  protected LanguageServerSettingManager settingsManager;

  /**
   * Creates a new completion manager. The manager is responsible for trigger the completion, apply suggestions to the
   * document. And schedule the rendering of ghost text.
   */
  public BaseCompletionManager(CopilotLanguageServerConnection lsConnection, CompletionProvider provider,
      ITextEditor editor, LanguageServerSettingManager settingsManager) {
    this.codeMinings = new ArrayList<>();
    this.textViewer = (ITextViewer) editor.getAdapter(ITextViewer.class);
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

    // position updater is used to update the position when the document is changed.
    // this is needed because the completion ghost text is rendered based on the
    // position in the document. If the document is changed, the position will be
    // invalidated.
    this.positionUpdater = new DefaultPositionUpdater(this.getCategory());
    if (!initializeDocument()) {
      return;
    }
    this.settingsManager = settingsManager;
    this.provider = provider;
    this.provider.addCompletionListener(this);
    this.documentVersion = -1;
    this.triggerPosition = new Position(0);

    // initialize the auto show completion preference and add listener to update it.
    this.autoShowCompletion = settingsManager.getSettings().isEnableAutoCompletions();

    // initialize NES preference
    this.enableNes = CopilotUi.getBooleanPreference(Constants.ENABLE_NEXT_EDIT_SUGGESTION, false);

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
    this.document.addPositionCategory(this.getCategory());
    this.document.addPositionUpdater(this.positionUpdater);
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
      this.styledText.addKeyListener(this);
      this.styledText.addMouseListener(this);
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
      clearGhostTexts();
    } else if (isDeletion(event)) {
      if (this.suggestionUpdateManager.getSize() > 0
          && this.suggestionUpdateManager.delete(event.getReplacedText().length())) {
        this.updateGhostTexts(new Position(this.cachedModelOffset));
      } else {
        clearGhostTexts();
      }
    } else if (isInsertion(event)) {
      if (this.suggestionUpdateManager.getSize() > 0 && this.suggestionUpdateManager.insert(event.getText())) {
        this.updateGhostTexts(new Position(this.cachedModelOffset + event.getText().length()));
      } else {
        clearGhostTexts();
      }
    }

  }

  /**
   * Return if the text change event is a deletion event.
   */
  protected boolean isDeletion(TextEvent event) {
    return StringUtils.isNotEmpty(event.getReplacedText()) && StringUtils.isEmpty(event.getText());
  }

  /**
   * Return if the text change event is a replacement event.
   */
  protected boolean isReplacement(TextEvent event) {
    return StringUtils.isNotEmpty(event.getReplacedText()) && StringUtils.isNotEmpty(event.getText());
  }

  /**
   * Return if the text change event is an insertion event.
   */
  protected boolean isInsertion(TextEvent event) {
    return StringUtils.isEmpty(event.getReplacedText()) && StringUtils.isNotEmpty(event.getText());
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
    this.updateGhostTexts(this.triggerPosition);
    this.notifyShown();
  }

  @Override
  public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    // do nothing
  }

  @Override
  public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
    this.document = newInput;
    initializeDocument();
    CopilotCore.LOGGER.info("Completion handler is refreshed for the document: " + this.documentUri);
  }

  /**
   * Abstract method to update ghost texts. Subclasses must implement this method to provide specific ghost text
   * rendering behavior.
   */
  protected abstract void updateGhostTexts(Position inferredPosition);

  /**
   * Abstract method to clear completion rendering. Subclasses must implement this method to provide specific behavior
   * for clearing ghost text.
   */
  public abstract void clearGhostTexts();

  /**
   * Get the current document line based on the trigger position. This is used to get the current line of text where the
   * completion is triggered.
   *
   * @return the current document line as a string.
   * @throws BadLocationException if the trigger position is invalid.
   */
  protected String getCurrentLine(Position position) throws BadLocationException {
    String documentContent = this.document.get();
    int triggerOffset = position.getOffset();
    String documentLine = "";
    int lineOffset = this.document.getLineOfOffset(triggerOffset);
    if (lineOffset == this.document.getNumberOfLines() - 1) {
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
    return documentLine;
  }

  /**
   * Check if the character at the given index in the document content is a new line character.
   *
   * @param documentContent The content of the document.
   * @param index The index to check.
   * @return true if the character is a new line character, false otherwise.
   */
  protected boolean isNewLineCharacter(String documentContent, int index) {
    char currentChar = documentContent.charAt(index);
    boolean isLineFeed = currentChar == '\n';
    if (isLineFeed) {
      return true;
    } else {
      return currentChar == '\r' && index + 1 < this.document.getLength() && documentContent.charAt(index + 1) == '\n';
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().equals(Constants.AUTO_SHOW_COMPLETION)) {
      this.autoShowCompletion = Boolean.parseBoolean(event.getNewValue().toString());
    } else if (event.getProperty().equals(Constants.ENABLE_NEXT_EDIT_SUGGESTION)) {
      this.enableNes = Boolean.parseBoolean(event.getNewValue().toString());
    }
  }

  /**
   * Trigger the inline completion.
   */
  public void triggerCompletion() {
    try {
      // TODO: this logic cannot handle the case that nes doesn't have a valid suggetion while inline completion has
      // result. need merge two results later
      if (shouldSkipCompletionDueToNes()) {
        return;
      }

      IFile file = LSPEclipseUtils.getFile(document);
      this.provider.triggerCompletion(file, LSPEclipseUtils.toPosition(this.triggerPosition.getOffset(), this.document),
          documentVersion, this.enableNes);
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.error(e);
    }
  }

  /**
   * Check if completion should be skipped due to active or pending NES suggestion. This prevents race conditions when
   * NES is being fetched or displayed.
   *
   * @return true if should skip completion, false otherwise
   */
  private boolean shouldSkipCompletionDueToNes() {
    EditorsManager editorsManager = CopilotUi.getPlugin().getEditorsManager();
    if (editorsManager == null) {
      return false;
    }
    ITextEditor editor = editorsManager.getActiveEditor();
    if (editor == null) {
      return false;
    }
    RenderManager nesManager = editorsManager.getNesRenderManager(editor);
    if (nesManager == null) {
      return false;
    }
    // Check both pending and active states to prevent race conditions
    return nesManager.isNesPendingOrActive();
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
      // Since we removed caret listener, we need to manually clear the ghost text when the caret position is changed by
      // the entire suggestion acceptance to fix the line indentation not cleared issue.
      if (type == AcceptSuggestionType.FULL) {
        this.clearGhostTexts();
      }
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
  protected String getCategory() {
    return this.toString();
  }

  /**
   * Disposes the resources of this completion handler.
   */
  public void dispose() {
    if (this.provider != null) {
      this.provider.removeCompletionListener(this);
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

    // put the below dispose logic to a workbench job to avoid blocking shutdown.
    WorkbenchJob job = new WorkbenchJob("Dispose Completion Manager") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        BaseCompletionManager cm = BaseCompletionManager.this;
        if (cm.textViewer != null) {
          cm.textViewer.removeTextInputListener(cm);
        }

        if (cm.styledText != null && !cm.styledText.isDisposed()) {
          cm.styledText.removeKeyListener(cm);
          cm.styledText.removeMouseListener(cm);
        }
        return Status.OK_STATUS;
      }
    };
    job.setSystem(true);
    job.schedule();
  }

  public SuggestionUpdateManager getSuggestionUpdateManager() {
    return suggestionUpdateManager;
  }

  /**
   * Notify the language server that the completion suggestion is shown. This is used to track the usage of the
   * completion suggestions.
   */
  protected void notifyShown() {
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

  /**
   * Enable the completion context for the current workbench. This is used to activate the context when the completion
   * is available.
   */
  protected static void enableContext() {
    if (completionContextActivation == null) {
      IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
      if (contextService != null) {
        SwtUtils.invokeOnDisplayThread(() -> {
          completionContextActivation = contextService.activateContext(COMPLETION_CONTEXT);
        });
      }
    }
  }

  /**
   * Disable the completion context for the current workbench. This is used to deactivate the context when the
   * completion is not available.
   */
  protected static void disableContext() {
    if (completionContextActivation != null) {
      IContextService contextService = PlatformUI.getWorkbench().getService(IContextService.class);
      if (contextService != null) {
        SwtUtils.invokeOnDisplayThread(() -> {
          contextService.deactivateContext(completionContextActivation);
        });
        completionContextActivation = null;
      }
    }
  }

  @Override
  public void mouseDoubleClick(MouseEvent e) {
    // Do nothing
  }

  @Override
  public void mouseDown(MouseEvent e) {
    handleCaretPositionChange();
  }

  @Override
  public void mouseUp(MouseEvent e) {
    // Do nothing
  }

  @Override
  public void keyPressed(KeyEvent e) {
    // Do nothing
  }

  @Override
  public void keyReleased(KeyEvent e) {
    // Skip completion triggering when the key is ESC
    if (e.character != SWT.ESC) {
      handleCaretPositionChange();
    }
  }

  /**
   * Handle caret move, clear and update the ghost text accordingly.
   */
  protected void handleCaretPositionChange() {
    // it's guaranteed that the document change event comes earlier than keyReleased
    // To verify this behavior, set breakpoints in org.eclipse.lsp4e.DocumentContentSynchronizer
    // at the line: changeParamsToSend.getTextDocument().setVersion(++version); and this class's keyReleased method.
    // Then trigger completion to verify the event.
    int currentVersion = this.lsConnection.getDocumentVersion(this.documentUri);
    if (this.documentVersion < 0) {
      // initialize the document version and return. This avoids the ghost text
      // being rendered when user opens the editor and just clicks in it.
      this.documentVersion = currentVersion;
      return;
    }

    int modelOffset = getModelOffsetFromCaretPosition();

    if (this.triggerPosition.offset == modelOffset) {
      return;
    }
    this.triggerPosition = new Position(modelOffset);
    if (currentVersion == this.documentVersion) {
      // if the caret position is changed without document version change, we should remove the ghost text.
      clearGhostTexts();
    } else {
      this.documentVersion = currentVersion;
      if (this.autoShowCompletion) {
        triggerCompletion();
      }
    }
    redrawBlockLineAtModelOffset(modelOffset);
  }

  /**
   * Gets the model offset from the current caret position using the display thread. This ensures thread safety when
   * accessing UI components.
   */
  protected int getModelOffsetFromCaretPosition() {
    int[] modelOffsetHolder = new int[1];
    SwtUtils.invokeOnDisplayThread(() -> {
      modelOffsetHolder[0] = UiUtils.widgetOffset2ModelOffset(textViewer, styledText.getCaretOffset());
    }, this.styledText);
    return modelOffsetHolder[0];
  }

  /**
   * Redraw the block ghost text line at the given model offset. This is used to fix legacy vertical indentation issues
   * when the text is outside the visible range or code blocks are collapsed.
   *
   * @param modelOffset The model offset to redraw the block ghost text line at.
   */
  protected void redrawBlockLineAtModelOffset(int modelOffset) {
    // Redraw the block ghost text line at both old and new offsets to fix legacy vertical indentation issues when text
    // is outside the visible range or code blocks are collapsed.
    // Fix issue: https://github.com/microsoft/copilot-eclipse/issues/105
    // Fix issue: https://github.com/microsoft/copilot-eclipse/issues/137
    if (modelOffset != this.cachedModelOffset) {
      // The collapsed code block will cause the model offset to flicker, so we need to redraw the block ghost text line
      // at both old and new offsets.
      SwtUtils.redrawBlockLineAtModelOffset(textViewer, this.cachedModelOffset, false);
      SwtUtils.redrawBlockLineAtModelOffset(textViewer, modelOffset, false);
      this.cachedModelOffset = modelOffset;
    }
  }
}
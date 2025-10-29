package com.microsoft.copilot.eclipse.ui.nes;

import java.net.URI;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsResult.CopilotInlineEdit;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyAcceptedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyRejectedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyShownParams;
import com.microsoft.copilot.eclipse.core.nes.NextEditSuggestionListener;
import com.microsoft.copilot.eclipse.core.nes.NextEditSuggestionProvider;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Push-model controller: owns suggestion state & popup/menu; column is passive painter. Implements ITextListener to
 * distinguish between document content changes (clear suggestion) and pure view changes (update position).
 */
public class RenderManager implements NextEditSuggestionListener, ITextListener, ITextInputListener {

  private CopilotLanguageServerConnection lsConnection;
  private NextEditSuggestionProvider nesProvider;
  private ITextEditor editor; // editor instance for column matching
  private ITextViewer viewer;
  private StyledText text;
  private volatile RulerColumn column;

  /**
   * Current diff model (original + replacement text). Null if no active suggestion.
   */
  public static class DiffModel { // made public for DiffPopup access
    String original;
    String replacement;
    Range range; // range of the edit for line-start detection
    TextDiffCalculator.DualDiffResult diffResult; // Cached diff calculation result

    /**
     * Checks if this is a pure deletion (has original text but no replacement).
     */
    public boolean isPureDelete() {
      return StringUtils.isNotBlank(original) && StringUtils.isBlank(replacement);
    }

    /**
     * Checks if this is a pure insertion (has replacement text but no original).
     */
    public boolean isPureInsert() {
      return StringUtils.isBlank(original) && StringUtils.isNotBlank(replacement);
    }
  }

  private DiffModel diffModel;
  private Range lastRange; // precise range of current suggestion
  private IFile lastFile; // track file for extension-based fallback
  private String currentSuggestionUuid; // UUID for telemetry
  private int suggestionDocumentVersion = -1; // document version when suggestion was received
  private Position suggestionStartPosition; // start offset of suggestion range
  private Position suggestionEndPosition; // end offset of suggestion range
  private Position indentLinePosition; // track the actual line where indent is applied
  private DefaultPositionUpdater positionUpdater;
  private static final String POSITION_CATEGORY = 
      "com.microsoft.copilot.eclipse.ui.suggest.RenderManager.SuggestionPosition";

  private ActionMenu actionMenu;
  private DiffPopup diffPopup = new DiffPopup();
  private InlineHighlighter highlighter;
  private BottomBar bottomBar;

  // Listener references for cleanup
  private IPropertyChangeListener nesPropertyListener;
  private IEventBroker eventBroker;

  // Cache suggestion viewport state
  private boolean suggestionInViewport = false;

  /**
   * Constructor. Mirrors BaseCompletionManager pattern: accepts ITextEditor and extracts viewer/text internally.
   */
  public RenderManager(CopilotLanguageServerConnection lsConnection, NextEditSuggestionProvider nesProvider,
      ITextEditor editor) {
    this.lsConnection = lsConnection;
    this.nesProvider = nesProvider;
    this.editor = editor;
    this.viewer = editor.getAdapter(ITextViewer.class);
    this.text = viewer != null ? viewer.getTextWidget() : null;
    initializePositionTracking();
    this.highlighter = new InlineHighlighter(viewer, text);
    this.actionMenu = new ActionMenu(text);

    // Register all SWT listeners
    registerListeners();

    // Register as NES provider listener
    if (nesProvider != null) {
      nesProvider.addListener(this);
    }

    // Subscribe to ActionMenu events via event bus
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (this.eventBroker != null) {
      this.eventBroker.subscribe(CopilotEventConstants.TOPIC_NES_ACCEPT_SUGGESTION, event -> {
        Object data = event.getProperty(IEventBroker.DATA);
        if (data == this.text) {
          acceptSuggestion();
        }
      });
      this.eventBroker.subscribe(CopilotEventConstants.TOPIC_NES_REJECT_SUGGESTION, event -> {
        Object data = event.getProperty(IEventBroker.DATA);
        if (data == this.text) {
          clearSuggestion();
        }
      });
    }

    // Late attach: if column was created earlier (before controller) attach now via registry.
    if (this.column == null) {
      for (var col : RulerColumn.getLiveColumns()) {
        if (col != null && col.getTextEditor() == this.editor) {
          attachColumn(col);
          break;
        }
      }
    }
  }

  /**
   * Attach a ruler column after controller creation. Safe to call repeatedly; only the first effective column is used.
   */
  public synchronized void attachColumn(RulerColumn col) {
    if (col == null || col == this.column) {
      return;
    }
    this.column = col;
    if (hasActiveSuggestion()) {
      SwtUtils.invokeOnDisplayThread(() -> {
        refreshUi();
      });
    }
  }

  /**
   * Detach the current column (e.g. when UI column disposed) without disposing controller so suggestions keep flowing.
   */
  public synchronized void detachColumn(RulerColumn col) {
    if (this.column == col) {
      this.column = null;
    }
  }

  /**
   * Initialize Position tracking for suggestion offsets. Mirrors BaseCompletionManager.initializeDocument() pattern.
   */
  private void initializePositionTracking() {
    IDocument doc = viewer != null ? viewer.getDocument() : null;
    if (doc == null) {
      return;
    }
    this.positionUpdater = new DefaultPositionUpdater(POSITION_CATEGORY);
    doc.addPositionCategory(POSITION_CATEGORY);
    doc.addPositionUpdater(this.positionUpdater);
  }

  /**
   * Clean up Position tracking resources.
   */
  private void cleanupPositionTracking() {
    IDocument doc = viewer != null ? viewer.getDocument() : null;
    if (doc == null) {
      return;
    }
    try {
      if (suggestionStartPosition != null) {
        doc.removePosition(POSITION_CATEGORY, suggestionStartPosition);
        suggestionStartPosition = null;
      }
      if (suggestionEndPosition != null) {
        doc.removePosition(POSITION_CATEGORY, suggestionEndPosition);
        suggestionEndPosition = null;
      }
      if (indentLinePosition != null) {
        doc.removePosition(POSITION_CATEGORY, indentLinePosition);
        indentLinePosition = null;
      }
      if (this.positionUpdater != null) {
        doc.removePositionUpdater(this.positionUpdater);
      }
      doc.removePositionCategory(POSITION_CATEGORY);
    } catch (BadPositionCategoryException ex) {
      CopilotCore.LOGGER.error(ex);
    }
  }

  /**
   * Release all resources managed by this RenderManager.
   */
  public void dispose() {
    nesProvider.removeListener(this);
    if (text != null && !text.isDisposed()) {
      SwtUtils.invokeOnDisplayThread(() -> {
        // Clear UI first (including indent) before disposing resources
        clearSuggestionUi();
        highlighter.clear();
        if (actionMenu != null) {
          actionMenu.dispose();
        }
        diffPopup.dispose();
        if (bottomBar != null) {
          bottomBar.dispose();
        }
      }, text);
    }

    cleanupPositionTracking();
  }

  /**
   * Show suggestion UI for the given model line and texts.
   */
  public void showSuggestion(int modelLine, String removed, String added) {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (diffModel == null) {
        diffModel = new DiffModel();
      }
      diffModel.original = removed != null ? removed : "";
      diffModel.replacement = added != null ? added : "";
      diffModel.range = lastRange;
      diffModel.diffResult = TextDiffCalculator.calculateDiff(diffModel.original, diffModel.replacement);
      refreshUi();
      int startOff = (suggestionStartPosition != null && !suggestionStartPosition.isDeleted())
          ? suggestionStartPosition.getOffset()
          : -1;
      int endOff = (suggestionEndPosition != null && !suggestionEndPosition.isDeleted())
          ? suggestionEndPosition.getOffset()
          : -1;
      highlighter.apply(diffModel, startOff, endOff, lastRange);
      notifyShown();
    }, text);
  }

  private void clearSuggestionUi() {
    highlighter.clear();
    diffPopup.hideAndClearIndent(text, viewer, indentLinePosition);
    // Request layout with icon rendering disabled to ensure no icon is drawn
    if (column != null) {
      column.requestLayout(false);
    }
    if (bottomBar != null) {
      bottomBar.hide();
    }
  }

  private void clearSuggestionData() {
    IDocument doc = viewer != null ? viewer.getDocument() : null;
    if (doc != null) {
      try {
        if (suggestionStartPosition != null) {
          doc.removePosition(POSITION_CATEGORY, suggestionStartPosition);
          suggestionStartPosition = null;
        }
        if (suggestionEndPosition != null) {
          doc.removePosition(POSITION_CATEGORY, suggestionEndPosition);
          suggestionEndPosition = null;
        }
        if (indentLinePosition != null) {
          doc.removePosition(POSITION_CATEGORY, indentLinePosition);
          indentLinePosition = null;
        }
      } catch (BadPositionCategoryException ex) {
        CopilotCore.LOGGER.error(ex);
      }
    }
    diffModel = null;
    lastRange = null;
    currentSuggestionUuid = null;
    suggestionDocumentVersion = -1;
  }

  /**
   * Clear the current suggestion, notifying rejection.
   */
  public void clearSuggestion() {
    nesProvider.cancelCurrentRequest();
    notifyRejected();

    SwtUtils.invokeOnDisplayThread(() -> {
      clearSuggestionUi();
      clearSuggestionData();
    }, text);
  }

  /**
   * Check if there is an active NES suggestion.
   *
   * @return true if there is an active suggestion, false otherwise
   */
  public boolean hasActiveSuggestion() {
    return suggestionStartPosition != null && !suggestionStartPosition.isDeleted();
  }

  /**
   * Check if NES is currently pending (requesting) or has an active suggestion. This prevents race conditions with
   * inline completion triggering.
   *
   * @return true if NES is pending or active, false otherwise
   */
  public boolean isNesPendingOrActive() {
    return nesProvider.hasRequestInProgress() || hasActiveSuggestion();
  }

  /**
   * Open the action (accept / reject) menu at the given StyledText-relative coordinates.
   */
  public void openActionMenu(int textX, int textY) {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (text == null || text.isDisposed() || !hasActiveSuggestion()) {
        return;
      }
      actionMenu.show(textX, Math.max(0, textY + 2));
    }, text);
  }

  /**
   * Refresh suggestion UI.
   */
  private void refreshUi() {
    if (text == null || text.isDisposed()) {
      return;
    }
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (!hasActiveSuggestion()) {
        suggestionInViewport = false;
        return;
      }

      // Clear indentation area in ruler before updating position
      if (column != null && indentLinePosition != null) {
        int[] indentInfo = diffPopup.getAppliedIndentInfo(text, viewer, indentLinePosition);
        if (indentInfo != null) {
          column.clearIndentationArea(indentInfo[0], indentInfo[1]);
        }
      }
      suggestionInViewport = isSuggestionInViewport();
      // Pure delete: don't show DiffPopup
      boolean pureDelete = diffModel != null && diffModel.isPureDelete();
      if (!pureDelete && suggestionInViewport) {
        // Only show popup for insert/replace scenarios when in viewport
        diffPopup.updatePosition(text, viewer, lastFile, lastRange, indentLinePosition, diffModel);
      } else {
        // For pure delete or out-of-viewport: only hide popup without clearing indent
        // This avoids feedback loop: clearing indent → viewport changes → suggestion reappears
        diffPopup.hide();
      }

      if (bottomBar == null) {
        bottomBar = new BottomBar(text, this::jumpToSuggestionInternal);
      }
      if (suggestionInViewport || lastRange == null) {
        bottomBar.hide();
      } else {
        bottomBar.show();
      }

      // Refresh ruler column
      if (column != null) {
        column.requestLayout();
      }
    }, text);
  }

  /**
   * Check if the suggestion is currently visible in the viewport.
   */
  private boolean isSuggestionInViewport() {
    if (lastRange == null || text == null || text.isDisposed()) {
      return false;
    }
    int startModelLine = lastRange.getStart().getLine();
    int endModelLine = lastRange.getEnd().getLine();
    // Adjust end line for pure delete when range ends at line start
    if (endModelLine > startModelLine && lastRange.getEnd().getCharacter() == 0) {
      endModelLine--;
    }
    int endWidgetLine = UiUtils.modelLine2WidgetLine(viewer, endModelLine);
    if (endWidgetLine == -1) {
      return false;
    }
    // Check if endWidgetLine is in viewport
    int topIndex = text.getTopIndex();
    int bottomIndex = topIndex + Math.max(1, text.getClientArea().height / text.getLineHeight());
    return endWidgetLine >= topIndex && endWidgetLine <= bottomIndex;
  }

  /**
   * Get the current suggestion line number in the document, or null if no active suggestion.
   */
  public int getSuggestionLine() {
    if (suggestionStartPosition == null || suggestionStartPosition.isDeleted()) {
      return -1;
    }
    IDocument doc = viewer != null ? viewer.getDocument() : null;
    if (doc == null) {
      return -1;
    }
    try {
      return doc.getLineOfOffset(suggestionStartPosition.getOffset());
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.error(e);
      return -1;
    }
  }

  // ==== Listener implementation ====
  @Override
  public void onNextEditSuggestion(IFile file, CopilotInlineEdit edit) {
    if (edit == null || edit.getRange() == null) {
      return;
    }

    IDocument doc = viewer != null ? viewer.getDocument() : null;
    if (doc == null) {
      return;
    }
    if (!isCurrentDocument(doc, file)) {
      return;
    }

    // Validate document version to prevent showing stale suggestions
    if (edit.getTextDocument() != null) {
      Integer editVersion = edit.getTextDocument().getVersion();
      if (editVersion != null) {
        URI docUri = LSPEclipseUtils.toUri(doc);
        int currentVersion = lsConnection.getDocumentVersion(docUri);
        if (currentVersion != editVersion) {
          return;
        }
      }
    }
    Range range = edit.getRange();
    this.lastFile = file;
    this.lastRange = range;
    this.currentSuggestionUuid = edit.getUuid();
    this.suggestionDocumentVersion = edit.getTextDocument() != null && edit.getTextDocument().getVersion() != null
        ? edit.getTextDocument().getVersion()
        : -1;

    int startOffset;
    int endOffset;
    String removed = "";
    try {
      startOffset = LSPEclipseUtils.toOffset(range.getStart(), doc);
      endOffset = LSPEclipseUtils.toOffset(range.getEnd(), doc);
      if (startOffset < 0 || endOffset < startOffset) {
        return;
      }
      int length = endOffset - startOffset;
      if (length > 0) {
        removed = doc.get(startOffset, length);
      }
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.error(e);
      return;
    }
    setupPositionTracking(doc, startOffset, endOffset);
    SwtUtils.invokeOnDisplayThread(() -> expandFoldsIfNecessary(startOffset, endOffset));
    showSuggestion(range.getStart().getLine(), removed, edit.getText());
  }

  private boolean isCurrentDocument(IDocument doc, IFile file) {
    URI currentDocUri = LSPEclipseUtils.toUri(doc);
    String fileUriString = file != null ? com.microsoft.copilot.eclipse.core.utils.FileUtils.getResourceUri(file)
        : null;
    return currentDocUri != null && fileUriString != null && currentDocUri.toString().equals(fileUriString);
  }

  private void setupPositionTracking(IDocument doc, int startOffset, int endOffset) {
    try {
      if (suggestionStartPosition != null) {
        doc.removePosition(POSITION_CATEGORY, suggestionStartPosition);
      }
      if (suggestionEndPosition != null) {
        doc.removePosition(POSITION_CATEGORY, suggestionEndPosition);
      }
      if (indentLinePosition != null) {
        doc.removePosition(POSITION_CATEGORY, indentLinePosition);
      }

      // Create and register new positions
      suggestionStartPosition = new Position(startOffset, 0);
      suggestionEndPosition = new Position(endOffset, 0);
      doc.addPosition(POSITION_CATEGORY, suggestionStartPosition);
      doc.addPosition(POSITION_CATEGORY, suggestionEndPosition);
      int indentLineOffset = endOffset;
      int endLine = doc.getLineOfOffset(endOffset);
      if (endLine + 1 < doc.getNumberOfLines()) {
        indentLineOffset = doc.getLineOffset(endLine + 1);
      }
      indentLinePosition = new Position(indentLineOffset, 0);
      doc.addPosition(POSITION_CATEGORY, indentLinePosition);
    } catch (BadLocationException | BadPositionCategoryException e) {
      CopilotCore.LOGGER.error(e);
      suggestionStartPosition = null;
      suggestionEndPosition = null;
      indentLinePosition = null;
    }
  }

  /**
   * Register all SWT widget listeners.
   */
  private void registerListeners() {
    SwtUtils.invokeOnDisplayThread(() -> {
      text.addListener(SWT.Resize, e -> {
        refreshUi();
      });
      viewer.addViewportListener(v -> refreshUi());
      text.addListener(SWT.MouseHorizontalWheel, e -> refreshUi());
      // Add this listener to fix 2025-09 related issue that clicking cause indentation disappear and icon misalign
      text.addListener(SWT.MouseDown, e -> refreshUi());
      var horizontalBar = text.getHorizontalBar();
      if (horizontalBar != null) {
        horizontalBar.addListener(SWT.Selection, e -> refreshUi());
      }
      viewer.addTextListener(this);
      viewer.addTextInputListener(this);
      installProjectionListener();
    }, text);
    installNesListener();
  }

  /**
   * Install NES property change listener to clear suggestions when NES is disabled. This is NOT an SWT listener, so
   * it's safe to call directly without thread wrapper.
   */
  private void installNesListener() {
    IPreferenceStore preferenceStore = CopilotUi.getPlugin().getPreferenceStore();
    if (preferenceStore != null) {
      nesPropertyListener = event -> {
        if (Constants.ENABLE_NEXT_EDIT_SUGGESTION.equals(event.getProperty())) {
          boolean enabled = Boolean.parseBoolean(String.valueOf(event.getNewValue()));
          if (!enabled) {
            clearSuggestion();
          }
        }
      };
      preferenceStore.addPropertyChangeListener(nesPropertyListener);
    }
  }

  /**
   * Install projection listener to handle indent clearing when folding occurs.
   */
  private void installProjectionListener() {
    if (!(viewer instanceof ProjectionViewer pv)) {
      return;
    }
    var annotationModel = pv.getProjectionAnnotationModel();
    if (annotationModel != null) {
      // Listen to annotation changes (fold/unfold events)
      annotationModel.addAnnotationModelListener(new IAnnotationModelListener() {
        @Override
        public void modelChanged(IAnnotationModel model) {
          handleProjectionChange(pv);
        }
      });
    }
  }

  /**
   * Handle projection (fold) changes to detect if suggestion region is folded.
   */
  private void handleProjectionChange(ProjectionViewer pv) {
    if (!hasActiveSuggestion()) {
      return;
    }
    if (suggestionStartPosition == null || suggestionStartPosition.isDeleted() || suggestionEndPosition == null
        || suggestionEndPosition.isDeleted()) {
      return;
    }
    IDocument doc = viewer.getDocument();
    if (doc == null) {
      return;
    }
    var annotationModel = pv.getProjectionAnnotationModel();
    if (annotationModel == null) {
      return;
    }

    try {
      int suggestionStartOffset = suggestionStartPosition.getOffset();
      int suggestionEndOffset = suggestionEndPosition.getOffset();

      int foldStartOffset = findFoldContainingSuggestion(annotationModel, doc, suggestionStartOffset,
          suggestionEndOffset);
      if (foldStartOffset != -1) {
        clearSuggestion();
        SwtUtils.invokeOnDisplayThread(() -> {
          int widgetLine = UiUtils.modelOffset2WidgetLine(viewer, foldStartOffset);
          if (widgetLine >= 0) {
            UiUtils.setLineVerticalIndent(text, widgetLine + 1, 0);
          }
        }, text);
        return;
      }
      refreshUi();
    } catch (BadLocationException e) {
      CopilotCore.LOGGER.error(e);
    }
  }

  /**
   * Find the fold start offset if suggestion overlaps with any collapsed fold region. Returns the fold start offset, or
   * null if suggestion doesn't overlap with any fold.
   */
  private int findFoldContainingSuggestion(IAnnotationModel annotationModel, IDocument doc, int suggestionStartOffset,
      int suggestionEndOffset) throws BadLocationException {
    var iter = annotationModel.getAnnotationIterator();
    while (iter.hasNext()) {
      var ann = iter.next();
      if (!(ann instanceof ProjectionAnnotation projection)) {
        continue;
      }
      if (!projection.isCollapsed()) {
        continue;
      }

      var region = annotationModel.getPosition(ann);
      if (region == null) {
        continue;
      }

      int foldStartOffset = region.getOffset();
      int foldEndOffset = foldStartOffset + region.getLength();

      // Check if suggestion offset range overlaps with fold offset range
      if (suggestionStartOffset <= foldEndOffset && suggestionEndOffset >= foldStartOffset) {
        return foldStartOffset;
      }
    }
    return -1;
  }

  /**
   * Expand any collapsed projection (fold) annotations that overlap the given range.
   */
  private void expandFoldsIfNecessary(int startOffset, int endOffset) {
    if (!(viewer instanceof ProjectionViewer pv)) {
      return;
    }
    if (startOffset < 0 || endOffset < startOffset) {
      return;
    }
    int length = Math.max(1, endOffset - startOffset);
    Region region = new Region(startOffset, length);
    pv.exposeModelRange(region);
  }

  /**
   * Apply the current suggestion to the document, if it still matches the original text.
   */
  public void acceptSuggestion() {
    if (diffModel == null || nesProvider == null) {
      return;
    }
    IFile currentFile = lastFile;
    int capturedVersion = this.suggestionDocumentVersion;
    IDocument doc = viewer != null ? viewer.getDocument() : null;

    if (doc == null || !isValidSuggestionRange(doc)) {
      return;
    }

    // Re-validate document version before accepting
    if (capturedVersion >= 0) {
      URI docUri = LSPEclipseUtils.toUri(doc);
      int currentVersion = lsConnection.getDocumentVersion(docUri);
      if (currentVersion != capturedVersion) {
        // Document was modified after suggestion appeared, reject stale acceptance
        clearSuggestion();
        return;
      }
    }

    try {
      int startOff = suggestionStartPosition.getOffset();
      int endOff = suggestionEndPosition.getOffset();
      String current = doc.get(startOff, endOff - startOff);
      String expectedOriginal = diffModel.original == null ? "" : diffModel.original;

      if (!expectedOriginal.equals(current)) {
        return;
      }

      // UI operations must run on display thread
      SwtUtils.invokeOnDisplayThread(() -> {
        if (text == null || text.isDisposed()) {
          return;
        }
        // Clear indent BEFORE document changes to avoid position invalidation
        diffPopup.hideAndClearIndent(text, viewer, indentLinePosition);
      }, text);

      // Apply replacement (document operation)
      String replacement = diffModel.replacement == null ? "" : diffModel.replacement;
      doc.replace(startOff, endOff - startOff, replacement);
      int newCaretOffset = startOff + replacement.length();

      // UI operations: caret movement and reveal
      SwtUtils.invokeOnDisplayThread(() -> {
        if (text == null || text.isDisposed()) {
          return;
        }
        viewer.getSelectionProvider().setSelection(new TextSelection(newCaretOffset, 0));
        clearSuggestionUi();
      }, text);

      // Clear data and notify (non-UI operations)
      clearSuggestionData();
      notifyAccepted();
      scheduleNextSuggestionRequest(doc, currentFile, newCaretOffset);

    } catch (BadLocationException ex) {
      CopilotCore.LOGGER.error(ex);
    }
  }

  /**
   * Check if suggestion positions are valid for current document state.
   */
  private boolean isValidSuggestionRange(IDocument doc) {
    if (suggestionStartPosition == null || suggestionStartPosition.isDeleted()) {
      return false;
    }
    if (suggestionEndPosition == null || suggestionEndPosition.isDeleted()) {
      return false;
    }
    int startOff = suggestionStartPosition.getOffset();
    int endOff = suggestionEndPosition.getOffset();
    return startOff >= 0 && endOff >= startOff && endOff <= doc.getLength();
  }

  /**
   * Schedule next suggestion fetch after document settles.
   */
  private void scheduleNextSuggestionRequest(IDocument doc, IFile file, int caretOffset) {
    if (file == null) {
      return;
    }

    try {
      int line = doc.getLineOfOffset(caretOffset);
      int lineOffset = doc.getLineOffset(line);
      int character = caretOffset - lineOffset;
      org.eclipse.lsp4j.Position lspPosition = new org.eclipse.lsp4j.Position(line, character);
      nesProvider.fetchSuggestion(file, lspPosition);
    } catch (BadLocationException ex) {
      CopilotCore.LOGGER.error(ex);
    }
  }

  /** Jump to the suggestion line and show diff popup (used by bottom bar + Tab action). */
  private void jumpToSuggestionInternal() {
    int suggestionLine = getSuggestionLine();
    if (suggestionLine == -1 || text.isDisposed()) {
      return;
    }
    try {
      // Convert model line to widget line
      int widgetLine = UiUtils.modelLine2WidgetLine(viewer, suggestionLine);
      int lineHeight = text.getLineHeight();
      int visibleLines = Math.max(1, text.getClientArea().height / lineHeight);
      int totalLines = text.getLineCount();
      int targetTop = widgetLine - (visibleLines / 2);
      if (targetTop < 0) {
        targetTop = 0;
      }
      int maxTop = Math.max(0, totalLines - visibleLines);
      if (targetTop > maxTop) {
        targetTop = maxTop;
      }
      text.setTopIndex(targetTop);
      int lineOffset = viewer.getDocument().getLineOffset(suggestionLine);
      viewer.getSelectionProvider().setSelection(new TextSelection(lineOffset, 0));
    } catch (BadLocationException ex) {
      CopilotCore.LOGGER.error(ex);
    }
    // Hide notification bar and show normal diff popup
    if (bottomBar != null) {
      bottomBar.hide();
    }
    refreshUi();
  }

  /**
   * Handle a TAB action: if the suggestion line is in the current viewport, accept it; otherwise scroll (jump) to
   * reveal it (centered approximately). Returns true if a suggestion was present and action handled, false otherwise.
   */
  public boolean handleTabAcceptOrReveal() {
    if (!hasActiveSuggestion() || text == null || text.isDisposed()) {
      return false;
    }
    if (suggestionInViewport) {
      acceptSuggestion();
    } else {
      jumpToSuggestionInternal();
    }
    return true;
  }

  // ==== ITextListener implementation ====

  @Override
  public void textChanged(TextEvent event) {
    DocumentEvent docEvent = event.getDocumentEvent();
    if (docEvent != null && hasActiveSuggestion()) {
      clearSuggestion();
    }
  }

  @Override
  public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    // No action needed before change
  }

  @Override
  public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
    initializePositionTracking();
    clearSuggestion();
  }

  // ==== Telemetry methods ====

  /**
   * Notify Language Server that suggestion is shown (telemetry).
   */
  private void notifyShown() {
    if (currentSuggestionUuid == null || lsConnection == null) {
      return;
    }
    try {
      NotifyShownParams params = new NotifyShownParams(currentSuggestionUuid);
      lsConnection.notifyShown(params);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to notify shown telemetry", e);
    }
  }

  /**
   * Notify Language Server that suggestion is accepted (telemetry).
   */
  private void notifyAccepted() {
    if (currentSuggestionUuid == null || lsConnection == null) {
      return;
    }
    try {
      NotifyAcceptedParams params = new NotifyAcceptedParams(currentSuggestionUuid);
      lsConnection.notifyAccepted(params);
      currentSuggestionUuid = null; // Clear after accepting
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to notify accepted telemetry", e);
    }
  }

  /**
   * Notify Language Server that suggestion is rejected (telemetry).
   */
  private void notifyRejected() {
    if (currentSuggestionUuid == null || lsConnection == null) {
      return;
    }
    try {
      NotifyRejectedParams params = new NotifyRejectedParams(Collections.singletonList(currentSuggestionUuid));
      lsConnection.notifyRejected(params);
      currentSuggestionUuid = null; // Clear after rejecting
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to notify rejected telemetry", e);
    }
  }
}

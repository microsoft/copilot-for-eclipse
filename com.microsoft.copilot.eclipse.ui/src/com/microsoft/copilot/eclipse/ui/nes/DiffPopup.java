package com.microsoft.copilot.eclipse.ui.nes;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.nes.TextDiffCalculator.DiffSegment;
import com.microsoft.copilot.eclipse.ui.nes.TextDiffCalculator.DualDiffResult;
import com.microsoft.copilot.eclipse.ui.nes.TextDiffCalculator.DualDiffSpan;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.TextMateUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Displays a floating popup showing replacement text with syntax highlighting and diff coloring.
 */
public class DiffPopup {

  private static final int POPUP_H_MARGIN = 12;
  private static final int POPUP_V_MARGIN = 4;
  private static final int POPUP_VERTICAL_OFFSET = 2; // Gap between editor line and popup

  private StyledText popupText;
  private SourceViewer viewer;
  private Document document;

  private Color bgColor;
  private Color insertHighlightColor;

  private RenderManager.DiffModel diffModel;
  private int trimOffset = 0; // Characters removed from line start for coordinate mapping
  private boolean presentationListenerInstalled;
  private int appliedIndentLine = -1;
  private int appliedIndentHeight = 0;

  /**
   * Update popup position and show it.
   */
  public void updatePosition(StyledText text, ITextViewer editorViewer, IFile file, Range range, Position indentPos,
      RenderManager.DiffModel model) {

    if (text == null || text.isDisposed() || editorViewer == null || range == null) {
      hideAndClearIndent(text, editorViewer, indentPos);
      return;
    }
    // Convert LSP range to widget lines
    int startModelLine = range.getStart().getLine();
    int endModelLine = range.getEnd().getLine();
    // Adjust end line for pure delete when range ends at line start
    if (endModelLine > startModelLine && range.getEnd().getCharacter() == 0) {
      endModelLine--;
    }
    int startWidgetLine = UiUtils.modelLine2WidgetLine(editorViewer, startModelLine);
    int endWidgetLine = UiUtils.modelLine2WidgetLine(editorViewer, endModelLine);
    if (startWidgetLine == -1 || endWidgetLine == -1) {
      hideAndClearIndent(text, editorViewer, indentPos);
      return;
    }
    ensurePopupCreated(text, editorViewer, file, model);
    Point size = popupText.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    applyIndentation(text, editorViewer, indentPos, size.y + POPUP_V_MARGIN);
    positionPopup(text, editorViewer, range, startModelLine, endWidgetLine, size);
    show();
  }

  /**
   * Hide popup and clear editor indentation.
   */
  public void hideAndClearIndent(StyledText text, ITextViewer editorViewer, Position indentPos) {
    if (text == null || text.isDisposed()) {
      return;
    }
    hide();
    clearIndentation(text, editorViewer, indentPos);
  }

  /**
   * Dispose all resources.
   */
  public void dispose() {
    if (popupText != null && !popupText.isDisposed()) {
      popupText.dispose();
    }
    popupText = null;
  }

  /**
   * Ensure popup widget is created and content is updated.
   */
  private void ensurePopupCreated(StyledText editorText, ITextViewer editorViewer, IFile file,
      RenderManager.DiffModel model) {

    if (popupText == null || popupText.isDisposed()) {
      createPopupWidget(editorText, editorViewer, file, model);
    }
    if (model != null && document != null) {
      updatePopupContent(model);
    }
  }

  /**
   * Create popup widget and configure syntax highlighting.
   */
  private void createPopupWidget(StyledText editorText, ITextViewer editorViewer, IFile file,
      RenderManager.DiffModel model) {
    if (editorText == null || editorText.isDisposed()) {
      return;
    }
    Shell shell = editorText.getShell();
    Display display = shell.getDisplay();
    bgColor = CssConstants.getNesInsertBackground(display);
    insertHighlightColor = CssConstants.getNesInsertHighlight(display);
    viewer = new SourceViewer(shell, null, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER);
    document = new Document(model != null ? prepareDisplayText(model) : "");
    configureSyntaxHighlighting(viewer, file);
    viewer.setDocument(document);
    popupText = viewer.getTextWidget();
    copyFont(editorText, popupText);
    popupText.setBackground(bgColor);
    popupText.setMargins(POPUP_H_MARGIN, POPUP_V_MARGIN, POPUP_H_MARGIN, POPUP_V_MARGIN);
    popupText.pack();
    popupText.moveAbove(null);

    // Add dispose listener to clean up resources
    popupText.addDisposeListener(e -> {
      if (bgColor != null && !bgColor.isDisposed()) {
        bgColor.dispose();
        bgColor = null;
      }
      if (insertHighlightColor != null && !insertHighlightColor.isDisposed()) {
        insertHighlightColor.dispose();
        insertHighlightColor = null;
      }
    });

    this.diffModel = model;
    installDiffHighlighting();
    updateLayout();
  }

  /**
   * Update popup content and colors based on diff model.
   */
  private void updatePopupContent(RenderManager.DiffModel model) {
    this.diffModel = model;
    if (popupText != null && !popupText.isDisposed()) {
      Color newBgColor = CssConstants.getNesInsertBackground(popupText.getDisplay());
      if (bgColor != newBgColor) {
        if (bgColor != null && !bgColor.isDisposed()) {
          bgColor.dispose();
        }
        bgColor = newBgColor;
        popupText.setBackground(bgColor);
      }
    }
    String displayText = prepareDisplayText(model);
    document.set(displayText);
    updateLayout();
    refreshDiffHighlighting();
  }

  /**
   * Prepare display text: normalize newlines, apply trimming, add prefix for pure insert. Records trimOffset for
   * coordinate mapping.
   */
  private String prepareDisplayText(RenderManager.DiffModel model) {
    if (model == null) {
      trimOffset = 0;
      return "";
    }
    String replacement = normalizeNewlines(model.replacement);
    boolean startsFromLineStart = (model.range != null && model.range.getStart().getCharacter() == 0);
    if (!startsFromLineStart) {
      trimOffset = 0;
      return addPureInsertPrefix(replacement, model.isPureInsert());
    }
    String trimmedText = trimCommonIndentation(replacement);
    return addPureInsertPrefix(trimmedText, model.isPureInsert());
  }

  /**
   * Trim common indentation from all lines while preserving relative indentation.
   */
  private String trimCommonIndentation(String text) {
    String[] lines = text.split("\n", -1);

    // Find minimum indentation across all non-empty lines
    int minIndent = Integer.MAX_VALUE;
    for (String line : lines) {
      if (line.trim().isEmpty()) {
        continue;
      }
      int indent = countLeadingWhitespace(line);
      minIndent = Math.min(minIndent, indent);
    }

    if (minIndent == Integer.MAX_VALUE || minIndent == 0) {
      trimOffset = 0;
      return text;
    }
    trimOffset = minIndent;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      sb.append(line.length() >= minIndent ? line.substring(minIndent) : line);
      if (i < lines.length - 1) {
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  /**
   * Add "+ " prefix for pure insert.
   */
  private String addPureInsertPrefix(String text, boolean isPureInsert) {
    return isPureInsert ? "+ " + text : text;
  }

  /**
   * Count leading whitespace characters in a line.
   */
  private int countLeadingWhitespace(String line) {
    int count = 0;
    while (count < line.length() && Character.isWhitespace(line.charAt(count))) {
      count++;
    }
    return count;
  }

  /**
   * Position popup in shell coordinates.
   */
  private void positionPopup(StyledText text, ITextViewer editorViewer, Range range, int startModelLine,
      int endWidgetLine, Point size) {
    Shell shell = text.getShell();
    Rectangle shellBounds = shell.getClientArea();
    // Calculate Y: below end line
    int lineHeight = text.getLineHeight();
    int popupY = text.getLinePixel(endWidgetLine) + lineHeight + POPUP_VERTICAL_OFFSET;
    // Calculate X: align to editor text
    int popupX = calculateAlignmentX(text, editorViewer, range, startModelLine);
    // Convert to shell coordinates
    Point displayCoords = text.toDisplay(popupX, popupY);
    Point shellOrigin = shell.toDisplay(0, 0);
    int x = displayCoords.x - shellOrigin.x - POPUP_H_MARGIN;
    int y = displayCoords.y - shellOrigin.y;
    x = constrainToEditorBounds(text, shell, x, size.x);
    // If not enough space below, place above
    if (y + size.y > shellBounds.height) {
      y = Math.max(5, popupY - lineHeight - size.y - POPUP_VERTICAL_OFFSET);
      clearIndentation(text, editorViewer, null); // Clear indent when moving above
    }
    popupText.setLocation(x, y);
  }

  /**
   * Calculate X coordinate for popup alignment. - For mid-line start: align to range.start.character - For line start:
   * align to first non-whitespace character of first line
   */
  private int calculateAlignmentX(StyledText text, ITextViewer editorViewer, Range range, int startModelLine) {
    IDocument doc = editorViewer.getDocument();
    int startChar = range.getStart().getCharacter();

    // Mid-line start: align to exact start position
    if (startChar > 0) {
      return getTextCoordinate(text, editorViewer, doc, startModelLine, startChar);
    }
    // Line start: align to first non-whitespace character of first line
    try {
      String firstLineText = getLineText(doc, startModelLine);
      int firstNonSpace = countLeadingWhitespace(firstLineText);
      if (firstNonSpace < firstLineText.length()) {
        int x = getTextCoordinate(text, editorViewer, doc, startModelLine, firstNonSpace);
        // Adjust for pure insert prefix
        if (diffModel != null && diffModel.isPureInsert()) {
          x -= calculatePrefixWidth("+ ");
        }
        return Math.max(0, x);
      }
    } catch (BadLocationException ex) {
      CopilotCore.LOGGER.error(ex);
    }

    // Fallback to line start
    return getTextCoordinate(text, editorViewer, doc, startModelLine, 0);
  }

  /**
   * Get X coordinate of character at (line, character).
   */
  private int getTextCoordinate(StyledText text, ITextViewer editorViewer, IDocument doc, int modelLine,
      int character) {
    try {
      int lineOffset = doc.getLineOffset(modelLine);
      int modelOffset = lineOffset + character;
      int widgetOffset = UiUtils.modelOffset2WidgetOffset(editorViewer, modelOffset);

      if (widgetOffset >= 0 && widgetOffset < text.getCharCount()) {
        return text.getLocationAtOffset(widgetOffset).x;
      }
    } catch (BadLocationException ex) {
      CopilotCore.LOGGER.error(ex);
    }
    return 0;
  }

  /**
   * Calculate pixel width of text string.
   */
  private int calculatePrefixWidth(String prefix) {
    if (popupText == null || popupText.isDisposed()) {
      return prefix.length() * 7; // Fallback estimate
    }
    GC gc = new GC(popupText);
    int width = gc.stringExtent(prefix).x;
    gc.dispose();
    return width;
  }

  /**
   * Constrain X coordinate to stay within editor bounds.
   */
  private int constrainToEditorBounds(StyledText text, Shell shell, int x, int popupWidth) {
    Rectangle editorBounds = text.getClientArea();
    Point editorOrigin = text.toDisplay(0, 0);
    Point shellOrigin = shell.toDisplay(0, 0);

    int editorLeft = editorOrigin.x - shellOrigin.x;
    int editorRight = editorLeft + editorBounds.width;

    // Keep within right edge
    if (x + popupWidth + POPUP_H_MARGIN > editorRight) {
      x = Math.max(editorLeft, editorRight - popupWidth - POPUP_H_MARGIN);
    }

    // Keep minimum left margin
    return Math.max(editorLeft + 5, x);
  }

  /**
   * Apply vertical indentation in editor to make room for popup.
   */
  private void applyIndentation(StyledText text, ITextViewer viewer, Position indentPos, int height) {
    if (text == null || text.isDisposed() || viewer == null || indentPos == null || indentPos.isDeleted()) {
      return;
    }

    int targetLine = UiUtils.modelOffset2WidgetLine(viewer, indentPos.getOffset());
    if (targetLine < 0) {
      return;
    }

    // Calculate target line: current line for delete, next line for insert/replace
    if (diffModel != null && !diffModel.isPureDelete()) {
      targetLine++;
    }

    UiUtils.setLineVerticalIndent(text, targetLine, height);
    appliedIndentLine = targetLine;
    appliedIndentHeight = height;
  }

  /**
   * Clear vertical indentation in editor.
   */
  private void clearIndentation(StyledText text, ITextViewer viewer, Position indentPos) {
    if (text != null && !text.isDisposed() && viewer != null && indentPos != null && !indentPos.isDeleted()) {
      int targetLine = UiUtils.modelOffset2WidgetLine(viewer, indentPos.getOffset());
      if (targetLine >= 0) {
        if (diffModel != null && !diffModel.isPureDelete()) {
          targetLine++;
        }
        UiUtils.setLineVerticalIndent(text, targetLine, 0);
      }
    }

    // Also clear cached line, In case indentPos is invalid
    if (appliedIndentLine >= 0) {
      UiUtils.setLineVerticalIndent(text, appliedIndentLine, 0);
    }
    appliedIndentLine = -1;
    appliedIndentHeight = 0;
  }

  /**
   * Get current indentation info for RulerColumn to clear. Calculates based on current position.
   *
   * @return int array [widgetLine, height], or null if no indentation applied
   */
  public int[] getAppliedIndentInfo(StyledText text, ITextViewer viewer, Position indentPos) {
    if (appliedIndentHeight <= 0) {
      return null;
    }
    if (text == null || text.isDisposed() || viewer == null || indentPos == null || indentPos.isDeleted()) {
      return null;
    }

    int targetLine = UiUtils.modelOffset2WidgetLine(viewer, indentPos.getOffset());
    if (targetLine < 0) {
      return null;
    }

    // Calculate target line: current line for delete, next line for insert/replace
    if (diffModel != null && !diffModel.isPureDelete()) {
      targetLine++;
    }

    return new int[] { targetLine, appliedIndentHeight };
  }

  /**
   * Install presentation listener for diff highlighting.
   */
  private void installDiffHighlighting() {
    if (presentationListenerInstalled || viewer == null) {
      return;
    }
    viewer.addTextPresentationListener(this::applyDiffHighlighting);
    presentationListenerInstalled = true;
  }

  /**
   * Refresh diff highlighting by invalidating text presentation.
   */
  private void refreshDiffHighlighting() {
    if (viewer != null) {
      viewer.invalidateTextPresentation();
    }
  }

  /**
   * Apply diff highlighting to visible region. Maps original text coordinates to trimmed display coordinates.
   */
  private void applyDiffHighlighting(TextPresentation presentation) {
    if (diffModel == null || document == null) {
      return;
    }
    if (diffModel.isPureInsert() || diffModel.isPureDelete()) {
      return;
    }
    DualDiffResult diffResult = diffModel.diffResult;
    if (diffResult == null) {
      return;
    }
    String displayedText = document.get();
    String originalText = normalizeNewlines(diffModel.replacement);

    IRegion visible = viewer.getVisibleRegion();
    int visibleStart = visible.getOffset();
    int visibleEnd = visibleStart + visible.getLength();

    // Highlight INSERT and REPLACE spans
    for (DualDiffSpan span : diffResult.spans) {
      if (!shouldHighlightSpan(span)) {
        continue;
      }
      // Map original coordinates to display coordinates
      int displayStart = mapToDisplayCoordinate(originalText, span.newStart);
      int displayEnd = mapToDisplayCoordinate(originalText, span.newStart + span.newLength);
      displayStart = Math.max(0, displayStart);
      displayEnd = Math.min(displayEnd, displayedText.length());
      int start = Math.max(displayStart, visibleStart);
      int end = Math.min(displayEnd, visibleEnd);

      if (end > start) {
        StyleRange styleRange = new StyleRange();
        styleRange.start = start;
        styleRange.length = end - start;
        styleRange.background = insertHighlightColor;
        presentation.mergeStyleRange(styleRange);
      }
    }
  }

  /**
   * Check if span should be highlighted.
   */
  private boolean shouldHighlightSpan(DualDiffSpan span) {
    return span.newLength > 0 && (span.type == DiffSegment.Type.INSERT || span.type == DiffSegment.Type.REPLACE);
  }

  /**
   * Map original text position to display text position after trimming. Accounts for trimOffset characters removed from
   * each line start.
   */
  private int mapToDisplayCoordinate(String originalText, int originalPos) {
    if (trimOffset == 0 || originalPos == 0) {
      return originalPos;
    }
    int charsRemoved = 0;
    int posInLine = 0;
    for (int i = 0; i < originalPos && i < originalText.length(); i++) {
      if (originalText.charAt(i) == '\n') {
        posInLine = 0;
      } else {
        posInLine++;
        if (posInLine <= trimOffset) {
          charsRemoved++;
        }
      }
    }

    return originalPos - charsRemoved;
  }

  /**
   * Configure TextMate syntax highlighting for viewer.
   */
  private void configureSyntaxHighlighting(SourceViewer target, IFile file) {
    if (file == null) {
      return;
    }

    String extension = file.getFileExtension();
    if (extension == null) {
      return;
    }

    target.configure(TextMateUtils.getConfiguration(extension));
  }

  /**
   * Update popup layout and size.
   */
  private void updateLayout() {
    if (popupText == null || popupText.isDisposed()) {
      return;
    }

    Point raw = popupText.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    int adjustedHeight = adjustHeightForTrailingNewline(raw.y);
    popupText.setSize(raw.x, adjustedHeight);
  }

  /**
   * Adjust height to account for trailing newline.
   */
  private int adjustHeightForTrailingNewline(int rawHeight) {
    try {
      String text = document.get();
      if ((text.endsWith("\n") || text.endsWith("\r")) && popupText.getLineCount() > 1) {
        String lastLine = popupText.getLine(popupText.getLineCount() - 1);
        if (lastLine.isEmpty()) {
          int lineHeight = popupText.getLineHeight();
          return Math.max(rawHeight - lineHeight, rawHeight - lineHeight + 4);
        }
      }
    } catch (Exception ex) {
      CopilotCore.LOGGER.error(ex);
    }
    return rawHeight;
  }

  /**
   * Show the popup.
   */
  public void show() {
    if (popupText != null && !popupText.isDisposed() && !popupText.isVisible()) {
      popupText.setVisible(true);
    }
  }

  /**
   * Hide the popup.
   */
  public void hide() {
    if (popupText != null && !popupText.isDisposed() && popupText.isVisible()) {
      popupText.setVisible(false);
    }
  }

  /**
   * Get text of a document line.
   */
  private String getLineText(IDocument doc, int line) throws BadLocationException {
    int offset = doc.getLineOffset(line);
    int length = doc.getLineLength(line);
    return doc.get(offset, length);
  }

  /**
   * Normalize all line endings to \n.
   */
  private String normalizeNewlines(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    return text.replaceAll("\r\n?", "\n");
  }

  /**
   * Copy font from source to target StyledText.
   */
  private void copyFont(StyledText source, StyledText target) {
    if (source != null && !source.isDisposed()) {
      Font font = source.getFont();
      if (font != null && !font.isDisposed()) {
        target.setFont(font);
      }
    }
  }
}

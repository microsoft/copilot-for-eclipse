package com.microsoft.copilot.eclipse.ui.completion;

import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.microsoft.copilot.eclipse.core.completion.CompletionCollection;
import com.microsoft.copilot.eclipse.core.completion.CompletionListener;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyShownParams;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A class to control completion rendering.
 */
public class CompletionManager implements CompletionListener, PaintListener {

  private CopilotLanguageServerConnection lsConnection;
  private CompletionProvider provider;
  private IDocument document;
  private URI documentUri;
  private CompletionCollection completions;

  private ITextViewer textViewer;
  private Color ghostTextColor;
  private Position triggerPosition;

  /**
   * Creates a new CompletionManager.
   */
  public CompletionManager(CopilotLanguageServerConnection lsConnection, CompletionProvider provider,
      ITextViewer textViewer, IDocument document, URI documentUri) {
    this.lsConnection = lsConnection;
    this.provider = provider;
    this.provider.addCompletionListener(this);
    this.document = document;
    this.documentUri = documentUri;
    this.completions = null;

    this.triggerPosition = new org.eclipse.jface.text.Position(0);
    this.textViewer = textViewer;
    StyledText styledText = textViewer.getTextWidget();
    if (styledText != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        styledText.addPaintListener(this);
        this.ghostTextColor = new Color(styledText.getDisplay(), new RGB(UiConstants.DEFAULT_GHOST_TEXT_SCALE,
            UiConstants.DEFAULT_GHOST_TEXT_SCALE, UiConstants.DEFAULT_GHOST_TEXT_SCALE));
      }, styledText);
    }
  }

  /**
   * Triggers the completion.
   */
  public void triggerCompletion(Position position, int documentVersion) {
    this.triggerPosition = position;
    try {
      this.provider.triggerCompletion(documentUri.toASCIIString(),
          LSPEclipseUtils.toPosition(position.getOffset(), this.document), documentVersion);
    } catch (BadLocationException e) {
      CopilotUi.getPlugin().getLog().info("triggerCompletion BadLocationException e 77");
      // TODO log & send telemetry
    }
  }

  /**
   * Clear the completion.
   */
  public void clearGhostText(Position position) {
    this.triggerPosition = position;
    this.completions = null;
    StyledText styledText = textViewer.getTextWidget();
    if (styledText != null) {
      SwtUtils.invokeOnDisplayThread(styledText::redraw, styledText);
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
    StyledText styledText = textViewer.getTextWidget();
    if (styledText != null) {
      SwtUtils.invokeOnDisplayThread(styledText::redraw, styledText);
      this.notifyShown();
    }
  }

  @Override
  public void paintControl(PaintEvent e) {
    StyledText styledText = textViewer.getTextWidget();
    if (styledText == null) {
      return;
    }

    GC gc = e.gc;
    int widgetOffset = UiUtils.modelOffset2WidgetOffset(textViewer, this.triggerPosition.getOffset());
    // will get index out of bounds if the cursor is at the end.
    // Because there is no more text to get bounds at EOF.
    widgetOffset = Math.max(Math.min(widgetOffset, styledText.getCharCount() - 1), 0);
    setLineVerticalIndentation(styledText, gc, widgetOffset);

    if (this.completions == null) {
      return;
    }

    gc.setForeground(this.ghostTextColor);
    String firstLine = this.completions.getFirstLine();
    if (StringUtils.isNotBlank(firstLine)) {
      Rectangle bounds = styledText.getTextBounds(widgetOffset, widgetOffset);
      int y = bounds.y;
      y += bounds.height - styledText.getLineHeight();
      gc.drawString(firstLine, bounds.x + bounds.width, y, true);
    }
    String remainingLines = this.completions.getRemainingLines();
    if (StringUtils.isNotBlank(remainingLines)) {
      int lineHeight = styledText.getLineHeight();
      int fontHeight = gc.getFontMetrics().getHeight();
      int x = styledText.getLeftMargin();
      Point offsetLocation = styledText.getLocationAtOffset(widgetOffset);
      int y = offsetLocation.y + lineHeight * 2 - fontHeight;
      gc.drawText(remainingLines, x, y, true);
    }

  }

  private void setLineVerticalIndentation(StyledText styledText, GC gc, int widgetOffset) {
    int height = 0;
    if (this.completions != null) {
      // Change the height (line vertical indentation) to fit the line of
      // ghost text.
      Point ghostTextExtent = gc.textExtent(this.completions.getText());
      int numberOfLines = this.completions.getNumberOfLines();
      height = ghostTextExtent.y - ghostTextExtent.y / numberOfLines;
    }

    int lineIndex = styledText.getLineAtOffset(widgetOffset) + 1;
    lineIndex = Math.min(lineIndex, styledText.getLineCount() - 1);
    styledText.setLineVerticalIndent(lineIndex, height);
  }

  /**
   * Return if the completion manager has completion rendering.
   */
  public boolean hasCompletion() {
    return this.completions != null;
  }

  /**
   * Apply the completion suggestion to document.
   *
   * @throws BadLocationException if the offset is invalid.
   */
  public void acceptSuggestion() throws BadLocationException {
    int offset = this.triggerPosition.offset;
    if (offset < 0) {
      return;
    }

    String text = this.completions.getText();
    if (StringUtils.isEmpty(text)) {
      return;
    }
    this.document.replace(offset, 0, text);
  }

  public CompletionCollection getCompletions() {
    return completions;
  }

  /**
   * Dispose the resources used by the completion manager.
   */
  public void dispose() {
    this.provider.removeCompletionListener(this);
    this.completions = null;
    if (this.ghostTextColor != null) {
      this.ghostTextColor.dispose();
      this.ghostTextColor = null;
    }
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

}

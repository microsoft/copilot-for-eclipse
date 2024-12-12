package com.microsoft.copilot.eclipse.ui.completion;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Render the ghost text for a completion item.
 */
public class CompletionRendering implements PaintListener {

  private ITextViewer textViewer;
  private CompletionData completionData;
  private Color ghostTextColor;

  /**
   * Creates a new CompletionRendering.
   */
  public CompletionRendering(ITextViewer textViewer, CompletionData completionData) {
    this.completionData = completionData;
    this.textViewer = textViewer;
    StyledText styledText = textViewer.getTextWidget();
    if (styledText != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        styledText.addPaintListener(this);
        this.ghostTextColor = new Color(Display.getCurrent(), UiConstants.DEFAULT_GHOST_TEXT_COLOR);
      });
    }

  }

  public void setCompletionData(CompletionData completionData) {
    this.completionData = completionData;
  }

  @Override
  public void paintControl(PaintEvent e) {
    if (this.completionData == null || this.completionData.getTriggerOffset() < 0) {
      return;
    }

    StyledText styledText = textViewer.getTextWidget();
    if (styledText == null) {
      return;
    }

    GC gc = e.gc;
    gc.setForeground(this.ghostTextColor);
    // will get index out of bounds if the cursor is at the end.
    // Because there is no more text to get bounds at EOF.
    int caretOffset = Math.min(this.completionData.getTriggerOffset(), styledText.getCharCount() - 1);
    String displayText = this.completionData.getText();

    // set line vertical indentation
    setLineVerticalIndentation(styledText, gc, caretOffset, displayText);

    String firstLine = this.completionData.getFirstLine();
    if (StringUtils.isNotBlank(firstLine)) {
      Rectangle bounds = styledText.getTextBounds(caretOffset, caretOffset);
      int y = bounds.y;
      y += bounds.height - styledText.getLineHeight();
      gc.drawString(firstLine, bounds.x + bounds.width, y, true);
    }
    String remainingLines = this.completionData.getRemainingLines();
    if (StringUtils.isNotBlank(remainingLines)) {
      int lineHt = styledText.getLineHeight();
      int fontHt = gc.getFontMetrics().getHeight();
      int x = styledText.getLeftMargin();
      Point offsetLocation = styledText.getLocationAtOffset(caretOffset);
      int y = offsetLocation.y + lineHt * 2 - fontHt;
      gc.drawText(remainingLines, x, y, true);
    }
  }

  /**
   * Trigger a redraw event to update the ghost text.
   */
  public void redraw() {
    StyledText styledText = textViewer.getTextWidget();
    if (styledText != null) {
      // TODO: can we use redrawRange() to improve the perf?
      SwtUtils.invokeOnDisplayThread(() -> styledText.redraw());
    }
  }

  /**
   * Dispose the resources used by the rendering.
   */
  public void dispose() {
    if (this.ghostTextColor != null) {
      this.ghostTextColor.dispose();
      this.ghostTextColor = null;
    }
  }

  private void setLineVerticalIndentation(StyledText styledText, GC gc, int caretOffset, String displayText) {
    Point ghostTextExtent = gc.textExtent(displayText);
    int numberOfLines = this.completionData.getNumberOfLines();
    if (numberOfLines <= 1) {
      return;
    }
    int height = ghostTextExtent.y - ghostTextExtent.y / numberOfLines;
    int lineIndex = styledText.getLineAtOffset(caretOffset);
    styledText.setLineVerticalIndent(lineIndex + 1, height);
  }

}

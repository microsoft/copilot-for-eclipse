package com.microsoft.copilot.eclipse.ui.completion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;

import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A class to control completion rendering.
 */
public class RenderingManager implements PaintListener {

  private List<GhostText> ghostTexts;

  private ITextViewer textViewer;
  private Color ghostTextColor;

  /**
   * Creates a new CompletionManager.
   */
  public RenderingManager(ITextViewer textViewer) {
    this.ghostTexts = new ArrayList<>();
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
   * Redraw the canvas(editor).
   */
  public void redraw() {
    StyledText styledText = textViewer.getTextWidget();
    if (styledText != null) {
      SwtUtils.invokeOnDisplayThread(styledText::redraw, styledText);
    }
  }

  @Override
  public void paintControl(PaintEvent e) {
    StyledText styledText = textViewer.getTextWidget();
    if (styledText == null) {
      return;
    }

    if (this.ghostTexts == null || this.ghostTexts.isEmpty()) {
      return;
    }

    GC gc = e.gc;
    gc.setForeground(this.ghostTextColor);

    int widgetOffset = UiUtils.modelOffset2WidgetOffset(textViewer, this.ghostTexts.get(0).modelOffset);
    // will get index out of bounds if the cursor is at the end.
    // Because there is no more text to get bounds at EOF.
    widgetOffset = Math.max(Math.min(widgetOffset, styledText.getCharCount() - 1), 0);
    for (GhostText ghostText : this.ghostTexts) {
      ghostText.draw(styledText, widgetOffset, gc);
    }
  }

  /**
   * Clear the ghost texts.
   */
  public void clearGhostText() {
    this.ghostTexts.clear();
    StyledText styledText = textViewer.getTextWidget();
    if (styledText != null) {
      SwtUtils.invokeOnDisplayThread(styledText::redraw, styledText);
    }
  }

  /**
   * Dispose the resources used by the completion manager.
   */
  public void dispose() {
    if (this.ghostTextColor != null) {
      this.ghostTextColor.dispose();
      this.ghostTextColor = null;
    }
  }

  public void setGhostTexts(List<GhostText> ghostTexts) {
    this.ghostTexts = ghostTexts;
  }

}

package com.microsoft.copilot.eclipse.ui.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

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
   * Whether the color resource should be disposed. When the color is fetched from the jface registry, it should not be
   * disposed.
   */
  private boolean needDisposeColorResource;

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
        this.ghostTextColor = getRegisteredInlineAnnotationColor(styledText.getDisplay());
      }, styledText);
    }
  }

  @Nullable
  private Color getRegisteredInlineAnnotationColor(Display display) {
    Color color = SwtUtils.getRegisteredInlineAnnotationColor(display);
    if (color == null) {
      needDisposeColorResource = true;
      color = SwtUtils.getDefaultGhostTextColor(display);
    }
    return color;
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
    // Set line indentation for the remaining ghost texts if any.
    int firstBlockLineGhostTextIndex = ghostTexts.stream()
        .filter(ghostText -> ghostText.type == GhostTextType.BLOCK_LINE).findFirst().map(ghostTexts::indexOf)
        .orElse(-1);
    if (firstBlockLineGhostTextIndex > 0) {
      Point ghostTextExtent = gc.textExtent(ghostTexts.get(firstBlockLineGhostTextIndex).text);
      int height = ghostTextExtent.y;
      setLineVerticalIndentation(styledText, gc,
          UiUtils.modelOffset2WidgetOffset(textViewer, ghostTexts.get(0).modelOffset), height);
    }

    for (GhostText ghostText : this.ghostTexts) {
      int widgetOffset = UiUtils.modelOffset2WidgetOffset(textViewer, ghostText.modelOffset);
      // will get index out of bounds if the cursor is at the end.
      // Because there is no more text to get bounds at EOF.
      widgetOffset = Math.max(Math.min(widgetOffset, styledText.getCharCount() - 1), 0);
      // reset the color to default because the inline ghost text may change the color to the same
      // as the content text color.
      gc.setForeground(this.ghostTextColor);

      ghostText.draw(styledText, widgetOffset, gc);
    }
  }

  private void setLineVerticalIndentation(StyledText styledText, GC gc, int widgetOffset, int height) {
    if (styledText == null || widgetOffset < 0) {
      return;
    }
    int widgetLine = styledText.getLineAtOffset(widgetOffset) + 1;
    widgetLine = Math.min(widgetLine, styledText.getLineCount() - 1);
    UiUtils.setLineVerticalIndent(styledText, widgetLine, height);
  }

  /**
   * Reset the line vertical indentation at the given widget offset.
   *
   * @param widgetOffset the widget offset to reset the line vertical indentation.
   */
  public void resetLineVerticalIndentationAtWidgetOffset(int widgetOffset) {
    StyledText styledText = textViewer.getTextWidget();
    setLineVerticalIndentation(styledText, null, widgetOffset, 0);
  }

  /**
   * Clear the ghost texts.
   */
  public void clearGhostText() {
    StyledText styledText = textViewer.getTextWidget();
    if (styledText == null) {
      this.ghostTexts.clear();
    } else {
      for (GhostText ghostText : this.ghostTexts) {
        if (Objects.equals(ghostText.type, GhostTextType.IN_LINE)) {
          int widgetOffset = UiUtils.modelOffset2WidgetOffset(textViewer, ghostText.modelOffset);
          StyleRange style = styledText.getStyleRangeAtOffset(widgetOffset);
          // update metrics to null to remove extra spaces of the inline ghost text.
          if (style != null && style.metrics != null) {
            style.metrics = null;
            styledText.setStyleRange(style);
          }
        } else {
          // Clear vertical indentation for the position where the completion is triggered.
          resetLineVerticalIndentationAtWidgetOffset(
              UiUtils.modelOffset2WidgetOffset(textViewer, ghostText.modelOffset));
        }
      }
      this.ghostTexts.clear();
      SwtUtils.invokeOnDisplayThread(styledText::redraw, styledText);
    }
  }

  /**
   * Dispose the resources used by the completion manager.
   */
  public void dispose() {
    if (this.ghostTextColor != null && needDisposeColorResource) {
      this.ghostTextColor.dispose();
      this.ghostTextColor = null;
    }
  }

  public void setGhostTexts(List<GhostText> ghostTexts) {
    this.ghostTexts = ghostTexts;
  }

}

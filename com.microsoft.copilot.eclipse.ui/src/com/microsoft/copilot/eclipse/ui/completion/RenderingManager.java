package com.microsoft.copilot.eclipse.ui.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A class to control completion rendering.
 */
public class RenderingManager implements PaintListener {

  private static final String INLINE_ANNOTATION_COLOR_KEY = "org.eclipse.ui.editors.inlineAnnotationColor";
  private static final int DEFAULT_GHOST_TEXT_SCALE = 128;

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
    ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
    if (colorRegistry == null) {
      return null;
    }
    Color color = colorRegistry.get(INLINE_ANNOTATION_COLOR_KEY);
    if (color == null) {
      needDisposeColorResource = true;
      color = new Color(display, new RGB(DEFAULT_GHOST_TEXT_SCALE, DEFAULT_GHOST_TEXT_SCALE, DEFAULT_GHOST_TEXT_SCALE));
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

    int widgetOffset = UiUtils.modelOffset2WidgetOffset(textViewer, this.ghostTexts.get(0).modelOffset);
    // will get index out of bounds if the cursor is at the end.
    // Because there is no more text to get bounds at EOF.
    widgetOffset = Math.max(Math.min(widgetOffset, styledText.getCharCount() - 1), 0);
    for (GhostText ghostText : this.ghostTexts) {
      // reset the color to default because the inline ghost text may change the color to the same
      // as the content text color.
      gc.setForeground(this.ghostTextColor);
      ghostText.draw(styledText, widgetOffset, gc);
    }
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

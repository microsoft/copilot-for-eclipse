package com.microsoft.copilot.eclipse.ui.completion;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * A ghost text placed in the line (not end of line). For single line ghost text, we draw it by ourselves. Because the
 * code mining API will put the cursor at the end of the line, which is not what we want.
 */
public class InlineGhostText extends GhostText {

  /**
   * Creates a new InlineGhostText.
   */
  public InlineGhostText(String text, int modelOffset) {
    super(text, modelOffset, GhostTextType.IN_LINE);
  }

  /**
   * see {@link org.eclipse.jface.text.source.inlined.InlinedAnnotationDrawingStrategy#drawAsLeftOf1stCharacter}.
   */
  @Override
  public void draw(StyledText styledText, int widgetOffset, GC gc) {
    String hostCharacter = styledText.getText(widgetOffset, widgetOffset);
    // Compute the location of the ghost text
    Rectangle bounds = styledText.getTextBounds(widgetOffset, widgetOffset);

    int x = bounds.x;
    int y = bounds.y;
    // When line text has line header annotation, there is a space on the top, adjust the y by using char height
    y += bounds.height - styledText.getLineHeight();

    gc.drawString(text, x, y, true);

    StyleRange style = styledText.getStyleRangeAtOffset(widgetOffset);
    int redrawnCharacterWidth = hostCharacter.charAt(0) != '\t' ? gc.stringExtent(hostCharacter).x
        : styledText.getTabs() * gc.stringExtent(" ").x;
    int textWidth = gc.stringExtent(text).x;
    StyleRange newStyle = updateStyle(widgetOffset, text, style, gc.getFontMetrics(), redrawnCharacterWidth, textWidth);
    if (newStyle != null) {
      styledText.setStyleRange(newStyle);
      return;
    }

    // The inline annotation replaces one character by taking a place width
    // GlyphMetrics
    // Here we need to redraw this first character because GlyphMetrics clip this
    // character.
    gc.setForeground(styledText.getForeground());
    gc.setBackground(styledText.getBackground());
    gc.setFont(styledText.getFont());

    // Get size of the character where GlyphMetrics width is added
    Point charBounds = gc.stringExtent(hostCharacter);
    int charWidth = charBounds.x;
    int redrawnHostCharX = x + bounds.width - charWidth;
    int redrawnHostCharY = y;
    if (style != null) {
      if (style.background != null) {
        gc.setBackground(style.background);
        gc.fillRectangle(redrawnHostCharX, y, charWidth + 1, bounds.height);
      }

      if (style.foreground != null) {
        gc.setForeground(style.foreground);
      }

      if (style.font != null) {
        gc.setFont(style.font);
      }
    }

    if (styledText.getSelection().x <= widgetOffset && styledText.getSelection().y > widgetOffset) {
      gc.setForeground(styledText.getSelectionForeground());
      gc.setBackground(styledText.getSelectionBackground());
    }
    gc.drawString(hostCharacter, redrawnHostCharX, redrawnHostCharY, true);
  }

  private static StyleRange updateStyle(int widgetOffset, String text, StyleRange style, FontMetrics fontMetrics,
      int redrawnCharacterWidth, int textWidth) {
    int fullWidth = textWidth + redrawnCharacterWidth;
    if (style == null) {
      style = new StyleRange();
      style.start = widgetOffset;
      style.length = 1;
    }

    GlyphMetrics metrics = style.metrics;
    if (text != null) {
      if (metrics == null) {
        metrics = new GlyphMetrics(fontMetrics.getAscent(), fontMetrics.getDescent(), fullWidth);
      } else {
        if (metrics.width == fullWidth) {
          return null;
        }

        metrics = new GlyphMetrics(fontMetrics.getAscent(), fontMetrics.getDescent(), fullWidth);
      }
    } else {
      metrics = null;
    }

    style.metrics = metrics;
    return style;
  }

}

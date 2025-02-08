package com.microsoft.copilot.eclipse.ui.completion;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * A ghost text placed at the end of the line. For single line ghost text, we draw it by ourselves. Because the code
 * mining API will put the cursor at the end of the line, which is not what we want.
 */
public class EolGhostText extends GhostText {

  /**
   * Creates a new EolGhostText.
   */
  public EolGhostText(String text, int modelOffset) {
    super(text, modelOffset, GhostTextType.END_OF_LINE);
  }

  @Override
  public void draw(StyledText styledText, int widgetOffset, GC gc) {
    if (StringUtils.isBlank(this.text)) {
      return;
    }
    int totalLength = styledText.getCharCount();
    if (widgetOffset == totalLength - 1
        && Objects.equals(styledText.getContent().getTextRange(widgetOffset, 1), "\n")) {
      drawAtLastEmptyLine(styledText, widgetOffset, gc);
    } else {
      drawAtEndOfLine(styledText, widgetOffset, gc);
    }
  }

  private void drawAtEndOfLine(StyledText styledText, int widgetOffset, GC gc) {
    Rectangle bounds = styledText.getTextBounds(widgetOffset, widgetOffset);
    int y = bounds.y;
    y += Math.max(0, bounds.height - styledText.getLineHeight());
    gc.drawString(this.text, bounds.x + bounds.width, y, true);
  }

  private void drawAtLastEmptyLine(StyledText styledText, int widgetOffset, GC gc) {
    Rectangle bounds = styledText.getTextBounds(widgetOffset - 1, widgetOffset - 1);
    int y = bounds.y + styledText.getLineHeight();
    gc.drawString(this.text, 1, y, true);
  }
}

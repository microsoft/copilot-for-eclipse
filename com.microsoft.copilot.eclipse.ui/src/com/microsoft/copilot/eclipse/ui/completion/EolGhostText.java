package com.microsoft.copilot.eclipse.ui.completion;

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
    if (StringUtils.isNotBlank(this.text)) {
      Rectangle bounds = styledText.getTextBounds(widgetOffset, widgetOffset);
      int y = bounds.y;
      y += Math.max(0, bounds.height - styledText.getLineHeight());
      gc.drawString(this.text, bounds.x + bounds.width, y, true);
    }

  }

}

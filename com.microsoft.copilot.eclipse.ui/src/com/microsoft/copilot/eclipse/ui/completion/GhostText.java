package com.microsoft.copilot.eclipse.ui.completion;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;

abstract class GhostText {
  protected String text;
  protected int modelOffset;
  protected GhostTextType type;

  protected GhostText(String text, int modelOffset, GhostTextType type) {
    super();
    this.text = text;
    this.modelOffset = modelOffset;
    this.type = type;
  }

  /**
   * Draws the ghost text.
   */
  public abstract void draw(StyledText styledText, int widgetOffset, GC gc);

}

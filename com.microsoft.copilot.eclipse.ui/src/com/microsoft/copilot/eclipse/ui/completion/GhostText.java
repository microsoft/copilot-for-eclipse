package com.microsoft.copilot.eclipse.ui.completion;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;

/**
 * Represents a ghost text.
 */
public abstract class GhostText {
  protected String text;

  protected int modelOffset;
  protected GhostTextType type;

  /**
   * Creates a new instance of the {@link GhostText} class.
   *
   * @param text The text of the ghost text.
   * @param modelOffset The offset of the ghost text in the document.
   * @param type The type of the ghost text.
   */
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

  public String getText() {
    return text;
  }

  public int getModelOffset() {
    return modelOffset;
  }

}

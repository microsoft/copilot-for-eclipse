package com.microsoft.copilot.eclipse.ui;

import org.eclipse.swt.graphics.RGB;

/**
 * A class to hold all the public constants used in the GitHub Copilot UI.
 */
public class UiConstants {

  private UiConstants() {
    // prevent instantiation
  }

  public static final int TOOLBAR_ICON_WIDTH_IN_PIEXL = 16;
  public static final int TOOLBAR_ICON_HEIGHT_IN_PIEXL = 16;

  /**
   * Default color for ghost text.
   */
  public static final RGB DEFAULT_GHOST_TEXT_COLOR = new RGB(112, 112, 112);
}

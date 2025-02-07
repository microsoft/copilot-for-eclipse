package com.microsoft.copilot.eclipse.ui.completion;

/**
 * Type of ghost text.
 */
public enum GhostTextType {
  /**
   * Single line of ghost text placed in the line (not at the end).
   */
  IN_LINE,

  /**
   * Single line of ghost text placed at the end of the line.
   */
  END_OF_LINE,

  /**
   * Block of ghost text with multiple lines placed below the last line of the document.
   */
  BELOW_LAST_LINE
}

package com.microsoft.copilot.eclipse.ui;

/**
 * A class to hold all the public constants used in the GitHub Copilot UI.
 */
public class UiConstants {

  public static final String HOVER_BACKGROUND = "org.eclipse.ui.workbench.HOVER_BACKGROUND";
  public static final String EDITOR_BACKGROUND = "org.eclipse.ui.editors.backgroundColor";
  public static final String WORKBENCH_TEXTEDITOR = "org.eclipse.ui.workbench.texteditor";
  public static final String INSERT_ICON = "icons/full/elcl16/insert_template.png";
  public static final String USE_PARENT_BACKGROUND = "useParentBackground";

  private UiConstants() {
    // prevent instantiation
  }

  public static final int TOOLBAR_ICON_WIDTH_IN_PIEXL = 16;
  public static final int TOOLBAR_ICON_HEIGHT_IN_PIEXL = 16;

  public static final int BTN_PADDING = 3;

  /**
   * The URL constants for the Copilot menu.
   */
  public static final String COPILOT_FEEDBACK_FORUM_URL = "https://github.com/orgs/community/discussions/categories/copilot";
}

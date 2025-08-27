package com.microsoft.copilot.eclipse.ui;

/**
 * A class to hold all the public constants used in the GitHub Copilot UI.
 */
public class UiConstants {

  public static final String WORKBENCH_TEXTEDITOR = "org.eclipse.ui.workbench.texteditor";
  public static final String INSERT_ICON = "icons/full/elcl16/insert_template.png";

  private UiConstants() {
    // prevent instantiation
  }

  public static final int TOOLBAR_ICON_WIDTH_IN_PIEXL = 16;
  public static final int TOOLBAR_ICON_HEIGHT_IN_PIEXL = 16;

  public static final int BTN_PADDING = 3;

  /**
   * The URL constants for the Copilot menu.
   */
  public static final String OPEN_URL_COMMAND_ID = "com.microsoft.copilot.eclipse.commands.openUrl";
  public static final String OPEN_URL_PARAMETER_NAME = "com.microsoft.copilot.eclipse.commands.openUrl.url";
  public static final String OPEN_CHAT_VIEW_COMMAND_ID = "com.microsoft.copilot.eclipse.commands.openChatView";
  public static final String OPEN_QUICK_START_COMMAND_ID = "com.microsoft.copilot.eclipse.commands.openQuickStart";
  public static final String COPILOT_FEEDBACK_FORUM_URL = "https://github.com/orgs/community/discussions/categories/copilot";
  public static final String COPILOT_UPGRADE_PLAN_URL = "https://aka.ms/github-copilot-upgrade-plan";
  public static final String MANAGE_COPILOT_URL = "https://aka.ms/github-copilot-settings";
  public static final String MANAGE_COPILOT_OVERAGE_URL = "https://aka.ms/github-copilot-manage-overage";
  public static final String OPEN_CHAT_VIEW_INPUT_VALUE = 
      "com.microsoft.copilot.eclipse.commands.openChatView.inputValue";
  public static final String OPEN_CHAT_VIEW_AUTO_SEND = "com.microsoft.copilot.eclipse.commands.openChatView.autoSend";
}

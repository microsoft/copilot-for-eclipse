package com.microsoft.copilot.eclipse.ui.dialogs;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for the i18n.
 */
public final class Messages extends NLS {
  public static String githubCodingAgentDialog_title;
  public static String githubCodingAgentDialog_info_description;
  public static String githubCodingAgentDialog_checkbox_dontAskAgain;
  public static String githubCodingAgentDialog_button_cancel;
  public static String githubCodingAgentDialog_button_continue;
  public static String projectSelectionDialog_title;
  public static String projectSelectionDialog_info_description;
  public static String projectSelectionDialog_label_selectProject;
  public static String projectSelectionDialog_button_cancel;
  public static String projectSelectionDialog_button_continue;
  public static String githubCodingAgent_link_learnMore;

  // Terminal dependency dialog
  public static String terminalDependencyDialog_shellTitle;
  public static String terminalDependencyDialog_title;
  public static String terminalDependencyDialog_message;
  public static String terminalDependencyDialog_missingDependencies;
  public static String terminalDependencyDialog_instruction;
  public static String terminalDependencyDialog_helpLink;
  public static String terminalDependencyDialog_dontShowAgain;

  static {
    // initialize resource bundle
    NLS.initializeMessages("com.microsoft.copilot.eclipse.ui.dialogs.messages", Messages.class);
  }

  private Messages() {
    // prevent instantiation
  }
}

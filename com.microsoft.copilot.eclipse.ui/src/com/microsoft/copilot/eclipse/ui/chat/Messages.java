package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for the i18n.
 */
public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.chat.messages"; //$NON-NLS-1$

  public static String chat_chatContentView_errorTemplate;
  public static String newChat_confirmationTitle;
  public static String newChat_confirmationMessage;
  public static String switchChat_confirmationTitle;
  public static String switchChat_confirmationMessage;
  public static String confirmDialog_keepChangesButton;
  public static String confirmDialog_undoChangesButton;
  public static String chat_warnWidget_defaultErrorMsg;
  public static String configureModes;
  public static String agentMessageWidget_openInBrowserButton;
  public static String agentMessageWidget_openInBrowserTooltip;
  public static String agentMessageWidget_openJobListButton;
  public static String agentMessageWidget_openJobListTooltip;
  public static String agentMessageWidget_openJobListError;
  public static String handoffContainer_proceedFrom;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}

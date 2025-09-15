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

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}

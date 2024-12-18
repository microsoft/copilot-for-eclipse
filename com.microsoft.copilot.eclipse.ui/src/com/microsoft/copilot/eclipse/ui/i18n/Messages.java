package com.microsoft.copilot.eclipse.ui.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for the i18n.
 */
public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.i18n.messages"; //$NON-NLS-1$
  public static String menu_signInStatus;
  public static String menu_signInStatus_ready;
  public static String menu_signInStatus_loading;
  public static String menu_signInStatus_notSignedInToGitHub;
  public static String menu_signInStatus_unknownError;
  public static String menu_signInStatus_notAuthorized;
  public static String menu_signInStatus_agentWarning;
  public static String menu_signToGitHub;
  public static String menu_signOutFromGitHub;
  public static String signInDialog_title;
  public static String signInDialog_button_cancel;
  public static String signInDialog_button_copyOpen;
  public static String signInDialog_info_instructions;
  public static String signInDialog_info_deviceCodePrefix;
  public static String signInDialog_info_gitHubWebSitePrefix;
  public static String signInConfirmDialog_progress;
  public static String signInConfirmDialog_progressSuffix;
  public static String signInConfirmDialog_progressTimeout;
  public static String signInConfirmDialog_progressCanceled;
  public static String signInConfirmDialog_authResult_notSignedIn;
  public static String signInConfirmDialog_authResult_notAuthed;
  public static String signInHandler_msgDialog_gitHubCopilot;
  public static String signInHandler_msgDialog_title;
  public static String signInHandler_msgDialog_alreadySignedIn;
  public static String signInHandler_msgDialog_signInSuccess;
  public static String signInHandler_msgDialog_signInFailed;
  public static String signInHandler_msgDialog_signInFailedTryAgain;
  public static String signInHandler_msgDialog_signInFailedFailure;
  public static String signOutHandler_msgDialog_gitHubCopilot;
  public static String signOutHandler_msgDialog_signOutSuccess;
  public static String signOutHandler_msgDialog_signOutFailed;
  public static String signOutHandler_msgDialog_signOutFailedFailure;
  
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
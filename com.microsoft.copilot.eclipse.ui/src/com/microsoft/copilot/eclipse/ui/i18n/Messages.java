package com.microsoft.copilot.eclipse.ui.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for the i18n.
 */
public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.i18n.messages"; //$NON-NLS-1$
  public static String menu_copilotStatus;
  public static String menu_copilotStatus_ready;
  public static String menu_copilotStatus_loading;
  public static String menu_copilotStatus_completionInProgress;
  public static String menu_copilotStatus_notSignedInToGitHub;
  public static String menu_copilotStatus_unknownError;
  public static String menu_copilotStatus_notAuthorized;
  public static String menu_copilotStatus_agentWarning;
  public static String menu_signToGitHub;
  public static String menu_signOutFromGitHub;
  public static String menu_viewFeedbackForum;
  public static String menu_editPreferences;
  public static String menu_editKeyboardShortcuts;
  public static String signInDialog_title;
  public static String signInDialog_button_cancel;
  public static String signInDialog_button_copyOpen;
  public static String signInDialog_info_instructions;
  public static String signInDialog_info_deviceCodePrefix;
  public static String signInDialog_info_githubWebSitePrefix;
  public static String signInConfirmDialog_progress;
  public static String signInConfirmDialog_progressTimeout;
  public static String signInConfirmDialog_progressCanceled;
  public static String signInConfirmDialog_authResult_notSignedIn;
  public static String signInConfirmDialog_authResult_notAuthed;
  public static String signInHandler_msgDialog_githubCopilot;
  public static String signInHandler_msgDialog_title;
  public static String signInHandler_msgDialog_alreadySignedIn;
  public static String signInHandler_msgDialog_signInSuccess;
  public static String signInHandler_msgDialog_signInFailed;
  public static String signInHandler_msgDialog_signInFailedTryAgain;
  public static String signInHandler_msgDialog_signInFailedFailure;
  public static String signOutHandler_msgDialog_githubCopilot;
  public static String signOutHandler_msgDialog_signOutSuccess;
  public static String signOutHandler_msgDialog_signOutFailed;
  public static String signOutHandler_msgDialog_signOutFailedFailure;
  public static String preferencesPage_description;
  public static String preferencesPage_autoShowCompletion;
  public static String preferences_page_enable_strict_ssl;
  public static String preferences_page_proxy_kerberos_sp;
  public static String preferences_page_github_enterprise;
  public static String preferences_page_proxy_config_link;
  public static String preferences_page_proxy_settings;
  public static String preferences_page_editor_settings;
  public static String preferences_page_auth_settings;
  public static String preferences_page_enable_strict_ssl_tooltip;
  public static String preferences_page_proxy_config_link_tooltip;
  public static String preferences_page_github_enterprise_tooltip;
  public static String preferences_page_note_text;
  public static String preferences_page_note_content;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
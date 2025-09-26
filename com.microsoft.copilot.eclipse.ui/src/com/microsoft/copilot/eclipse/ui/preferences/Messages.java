package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.osgi.util.NLS;

/**
 * Messages used in preference page.
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.preferences.messages"; //$NON-NLS-1$

  public static String preferences_page_completions_autoShowCompletion;
  public static String preferences_page_byok_title;
  public static String preferences_page_byok_description;
  public static String preferences_page_byok_signin_description;
  public static String preferences_page_loading;
  public static String preferences_page_byok_provider_title;
  public static String preferences_page_byok_provider_description;
  public static String preferences_page_byok_table_status_column;
  public static String preferences_page_byok_addModel_button;
  public static String preferences_page_byok_removeModel;
  public static String preferences_page_byok_changeApi_button;
  public static String preferences_page_byok_deleteApi_button;
  public static String preferences_page_byok_enableModel_button;
  public static String preferences_page_byok_disableModel_button;
  public static String preferences_page_byok_reload_button;
  public static String preferences_page_byok_removeModel_confirmDialog_message;
  public static String preferences_page_byok_addModel_dialog_title;
  public static String preferences_page_byok_addModel_modelId;
  public static String preferences_page_byok_addModel_deploymentUrl;
  public static String preferences_page_byok_addModel_apiKey;
  public static String preferences_page_byok_addModel_displayName;
  public static String preferences_page_byok_addModel_supportVision;
  public static String preferences_page_byok_addModel_supportToolCalling;
  public static String preferences_page_byok_changeApi_dialog_title;
  public static String preferences_page_byok_changeApi_dialog_description;
  public static String preferences_page_byok_deleteApi_dialog_title;
  public static String preferences_page_byok_deleteApi_dialog_description;
  public static String preferences_page_byok_dialog_add;
  public static String preferences_page_byok_dialog_delete;
  public static String preferences_page_byok_dialog_yes;
  public static String preferences_page_byok_default;
  public static String preferences_page_byok_dialog_cancel;
  public static String preferences_page_byok_dialog_remove;
  public static String preferences_page_byok_customModels;
  public static String preferences_page_byok_enabledCount;
  public static String preferences_page_completions_codeMiningNote;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}

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
  public static String preferences_page_byok_preview_disabled_tip;
  public static String preferences_page_completions_codeMiningNote;
  public static String preferences_page_customModes_defaultDescription;
  public static String preferences_page_customModes_defaultInstructions;
  public static String preferences_page_completions_enableNes;

  // CustomModesPreferencePage
  public static String customModes_page_description;
  public static String customModes_table_column_modeName;
  public static String customModes_table_column_workspace;
  public static String customModes_table_column_description;
  public static String customModes_button_add;
  public static String customModes_button_edit;
  public static String customModes_button_delete;
  public static String customModes_info_label;
  public static String customModes_error_noWorkspaceFolder_title;
  public static String customModes_error_noWorkspaceFolder_message;
  public static String customModes_dialog_createMode_title;
  public static String customModes_dialog_modeName_label;
  public static String customModes_dialog_workspaceFolder_label;
  public static String customModes_dialog_error_emptyName_title;
  public static String customModes_dialog_error_emptyName_message;
  public static String customModes_dialog_error_invalidName_title;
  public static String customModes_dialog_error_invalidName_message;
  public static String customModes_dialog_error_noFolder_title;
  public static String customModes_dialog_error_noFolder_message;
  public static String customModes_error_createFailed_title;
  public static String customModes_error_createFailed_message;
  public static String customModes_error_fileNotFound_title;
  public static String customModes_error_fileNotFound_message;
  public static String customModes_error_openFailed_title;
  public static String customModes_error_openFailed_message;
  public static String customModes_delete_confirm_title;
  public static String customModes_delete_confirm_message;
  public static String customModes_error_deleteFailed_title;
  public static String customModes_error_deleteFailed_message;
  public static String customModes_disabled_by_policy;

  //Agent Max Requests
  public static String preferences_page_agent_max_requests;
  public static String preferences_page_agent_max_requests_desc;
  public static String preferences_page_agent_max_requests_validation_error;

  public static String setting_managed_by_organization;
  public static String setting_disabled_by_organization;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}

package com.microsoft.copilot.eclipse.ui.dialogs;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for the i18n.
 */
public final class Messages extends NLS {
  public static String mcpRegistryDialog_title;
  public static String mcpRegistryDialog_searchPlaceholder;
  public static String mcpRegistryDialog_col_name;
  public static String mcpRegistryDialog_col_desc;
  public static String mcpRegistryDialog_col_actions;
  public static String mcpRegistryDialog_details;
  public static String mcpRegistryDialog_install;
  public static String mcpRegistryDialog_loading;
  public static String mcpRegistryDialog_empty;
  public static String mcpRegistryDialog_errorLoading;
  public static String mcpRegistryDialog_button_refresh;
  public static String mcpRegistryDialog_button_refresh_tooltip;
  public static String mcpRegistryDialog_button_changeUrl;
  public static String mcpRegistryDialog_button_changeUrl_tooltip;
  public static String mcpRegistryDialog_close;
  public static String mcpRegistryDialog_error_empty_url;
  public static String mcpRegistryDialog_emptyUrlForRegistryOnly_title;
  public static String mcpRegistryDialog_emptyUrlForRegistryOnly_msg;
  public static String mcpServerDetailDialog_title;
  public static String mcpServerDetailDialog_description;
  public static String mcpServerDetailDialog_category;
  public static String mcpServerDetailDialog_version;
  public static String mcpServerDetailDialog_repository;
  public static String mcpServerDetailDialog_repository_tooltip;
  public static String mcpServerDetailDialog_close;
  public static String mcpServerDetailDialog_noDetailsAvailable;
  public static String mcpServerDetailDialog_noDescription;
  public static String mcpServerDetailDialog_noVersion;
  public static String mcpServerDetailDialog_categoryDeveloperTools;
  public static String mcpServerDetailDialog_categoryDataScience;
  public static String mcpServerDetailDialog_published;
  public static String mcpServerDetailDialog_updated;
  public static String mcpServerDetailDialog_install;
  public static String mcpServerDetailDialog_noPublishedDate;
  public static String mcpServerDetailDialog_noUpdatedDate;
  public static String mcpServerDetailDialog_installation_options;
  public static String mcpServerDetailDialog_configuration_preview;
  public static String mcpServerDetailDialog_remote_prefix;

  public static String mcpApprovalDialog_title;
  public static String mcpApprovalDialog_description;
  public static String mcpApprovalDialog_column_status;
  public static String mcpApprovalDialog_column_pluginId;
  public static String mcpApprovalDialog_column_displayName;
  public static String mcpApprovalDialog_column_signedStatus;
  public static String mcpApprovalDialog_button_approve;
  public static String mcpApprovalDialog_button_deny;
  public static String mcpApprovalDialog_button_approveAll;
  public static String mcpApprovalDialog_button_denyAll;
  public static String mcpApprovalDialog_note_prefix;
  public static String mcpApprovalDialog_note_content;
  public static String mcpApprovalDialog_preview_label;
  public static String mcpApprovalDialog_preview_empty;
  public static String mcpApprovalDialog_status_approved;
  public static String mcpApprovalDialog_status_denied;
  public static String mcpApprovalDialog_signed_signed;
  public static String mcpApprovalDialog_signed_unsigned;
  public static String mcpApprovalDialog_warning_title;
  public static String mcpApprovalDialog_warning_message;
  public static String mcpApprovalDialog_warning_button_approve;
  public static String mcpApprovalDialog_warning_button_cancel;
  public static String mcpApprovalDialog_close_button;

  public static String mcpServerInstallManager_overrideServer_title;
  public static String mcpServerInstallManager_overrideServer_message;

  static {
    // initialize resource bundle
    NLS.initializeMessages("com.microsoft.copilot.eclipse.ui.dialogs.messages", Messages.class);
  }

  private Messages() {
    // prevent instantiation
  }
}

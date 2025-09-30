package com.microsoft.copilot.eclipse.ui.dialogs;

import org.eclipse.osgi.util.NLS;

/**
 * Dialog message class for the i18n.
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

  static {
    // initialize resource bundle
    NLS.initializeMessages("com.microsoft.copilot.eclipse.ui.dialogs.messages", Messages.class);
  }

  private Messages() {
    // prevent instantiation
  }
}
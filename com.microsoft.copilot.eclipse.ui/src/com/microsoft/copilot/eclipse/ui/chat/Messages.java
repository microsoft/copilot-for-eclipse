// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for the i18n.
 */
public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.chat.messages"; //$NON-NLS-1$

  public static String chat_chatContentView_errorTemplate;
  public static String chat_toolCall_genericError;
  public static String chat_toolCall_errorTemplate;
  public static String endChat_confirmationTitle;
  public static String endChat_confirmationMessage;
  public static String confirmDialog_keepChangesButton;
  public static String confirmDialog_undoChangesButton;
  public static String chat_warnWidget_defaultErrorMsg;
  public static String chat_warnWidget_byokQuotaUsageMessage;
  public static String configureModes;
  public static String agentMessageWidget_openInBrowserButton;
  public static String agentMessageWidget_openInBrowserTooltip;
  public static String agentMessageWidget_openJobListButton;
  public static String agentMessageWidget_openJobListTooltip;
  public static String handoffContainer_proceedFrom;
  public static String fileChangeSummary_filesChanged;
  public static String fileChangeSummary_fileChanged;
  public static String fileChangeSummary_keepButton;
  public static String fileChangeSummary_undoButton;
  public static String fileChangeSummary_collapseTooltip;
  public static String fileChangeSummary_expandTooltip;
  public static String todoList_titleWithCount;
  public static String todoList_clearButton;
  public static String todoList_clearButtonDisabled;
  public static String todoList_expandTooltip;
  public static String todoList_collapseTooltip;
  public static String thinking_title;
  public static String thinking_expandTooltip;
  public static String thinking_collapseTooltip;

  // Confirmation dialog action labels
  public static String confirmation_action_allowOnce;
  public static String confirmation_action_skip;
  public static String confirmation_action_allowAllCommands;
  public static String confirmation_action_allowNamesSession;
  public static String confirmation_action_alwaysAllowNames;
  public static String confirmation_action_allowExactSession;
  public static String confirmation_action_alwaysAllowExact;
  public static String confirmation_action_alwaysAllow;
  public static String confirmation_action_allowFileSession;
  public static String confirmation_action_allowFolderSession;

  // MCP confirmation dialog action labels
  public static String confirmation_title_mcpTool;
  public static String confirmation_title_mcpToolDefault;
  public static String confirmation_action_allowServerSession;
  public static String confirmation_action_alwaysAllowServer;

  // Confirmation dialog titles
  public static String confirmation_title_terminal;
  public static String confirmation_title_fallback;
  public static String confirmation_title_fileRead;
  public static String confirmation_title_fileWrite;
  public static String confirmation_title_fileOperation;

  // Confirmation dialog messages
  public static String confirmation_message_fileRead;
  public static String confirmation_message_fileWrite;
  public static String confirmation_message_fileOperation;

  // Misc
  public static String confirmation_autoApprovedDescription;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}

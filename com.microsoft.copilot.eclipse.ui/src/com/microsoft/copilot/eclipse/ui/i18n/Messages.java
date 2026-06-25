// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for the i18n.
 */
public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.i18n.messages"; //$NON-NLS-1$
  public static String menu_signToGitHub;
  public static String menu_signOutOfGitHub;
  public static String menu_configureGitHubCopilotSettings;
  public static String menu_giveFeedback;
  public static String menu_whatIsNew;
  public static String menu_editPreferences;
  public static String menu_editKeyboardShortcuts;
  public static String menu_turnOnCompletions;
  public static String menu_turnOffCompletions;
  public static String menu_openChatView;
  public static String menu_quota_copilotUsage;
  public static String menu_quota_codeCompletions;
  public static String menu_quota_chatMessages;
  public static String menu_quota_monthlyLimit;
  public static String menu_quota_includedCredits;
  public static String menu_quota_includedCreditsTooltip;
  public static String menu_quota_monthlyLimitTooltip;
  public static String menu_quota_percentUsedFormat;
  public static String menu_quota_aiCreditsUsedFormat;
  public static String menu_quota_included;
  public static String menu_userPlanFormat;
  public static String menu_quota_noUsageYet;
  public static String menu_quota_allowanceReset_today;
  public static String menu_quota_allowanceReset_singular;
  public static String menu_quota_allowanceReset_plural;
  public static String menu_quota_manageCopilotTooltip;
  public static String menu_quota_enableAdditionalUsage;
  public static String menu_quota_increaseBudget;
  public static String menu_quota_upgradePlan;
  public static String menu_quota_unlimitedOrgMessage;
  public static String menu_quota_plan_free;
  public static String menu_quota_plan_individual;
  public static String menu_quota_plan_individualPro;
  public static String menu_quota_plan_individualMax;
  public static String menu_quota_plan_business;
  public static String menu_quota_plan_enterprise;
  // TODO: Remove these legacy keys after TBB is officially released.
  // Legacy quota keys retained for installations where token-based billing is not yet enabled by the
  // language server. When tokenBasedBillingEnabled is false on the CheckQuotaResult the menu
  // handlers fall back to these strings to preserve the original main-branch UI.
  public static String menu_quota_premiumRequests;
  public static String menu_quota_additionalPremiumRequests;
  public static String menu_quota_enabled;
  public static String menu_quota_disabled;
  public static String menu_quota_additionalUsageOrgEnabledTooltip;
  public static String menu_quota_additionalUsageOrgNotConfiguredTooltip;
  public static String menu_quota_allowanceReset;
  public static String menu_quota_updateCopilotToPro;
  public static String menu_quota_updateCopilotToProPlus;
  public static String menu_quota_managePaidPremiumRequests;
  public static String signInDialog_title;
  public static String signInDialog_button_cancel;
  public static String signInDialog_button_copyOpen;
  public static String signInDialog_info_instructions;
  public static String signInDialog_info_deviceCodePrefix;
  public static String signInDialog_info_githubWebSitePrefix;
  public static String signInConfirmDialog_progress;
  public static String signInConfirmDialog_progressTimeout;
  public static String signInConfirmDialog_progressCanceled;
  public static String signInConfirmDialog_deviceCodeFormatString;
  public static String signInConfirmDialog_authResult_notSignedIn;
  public static String signInConfirmDialog_authResult_notAuthed;
  public static String signInHandler_msgDialog_githubCopilot;
  public static String signInHandler_msgDialog_title;
  public static String signInHandler_msgDialog_alreadySignedIn;
  public static String signInHandler_msgDialog_signInSuccess;
  public static String signInHandler_msgDialog_signInFailed;
  public static String signInHandler_msgDialog_signInFailedTryAgain;
  public static String signOutHandler_msgDialog_githubCopilot;
  public static String signOutHandler_msgDialog_signOutSuccess;
  public static String signOutHandler_msgDialog_signOutFailed;
  public static String signOutHandler_msgDialog_signOutFailedFailure;
  public static String chat_topBanner_defaultChatTitle;
  public static String chat_topBanner_chatHistoryItem_newChat;
  public static String chat_topBanner_chatHistoryItem_newChatTime_Now;
  public static String chat_topBanner_chatHistoryItem_untitledConversation_placeholder;
  public static String chat_topBanner_chatHistoryItem_currentConversation_label;
  public static String chat_actionBar_initialContent;
  public static String chat_actionBar_initialContentForAgent;
  public static String chat_actionBar_sendButton_Tooltip;
  public static String chat_actionBar_sendToJobButton_Tooltip;
  public static String chat_actionBar_sendToJob_noProject;
  public static String chat_actionBar_cancelButton_Tooltip;
  public static String chat_actionBar_toolButton_toolTip;
  public static String chat_actionBar_toolButton_disabled_toolTip;
  public static String chat_actionBar_modelPicker_manageModels;
  public static String chat_actionBar_toolButton_detected_toolTip;
  public static String chat_actionBar_autoBreakpointButton_enabled_Tooltip;
  public static String chat_actionBar_autoBreakpointButton_disabled_Tooltip;
  public static String chat_actionBar_autoBreakpointButton_accessibilityName;
  public static String chat_welcomeView_title;
  public static String chat_welcomeView_description;
  public static String chat_welcomeView_agentSuffix;
  public static String chat_welcomeView_mcpSuffix;
  public static String chat_welcomeView_completionSuffix;
  public static String chat_welcomeView_chatSuffix;
  public static String chat_welcomeView_freeCopilotLink;
  public static String chat_welcomeView_freeCopilotIntroPrefix;
  public static String chat_welcomeView_freeCopilotIntroSuffix;
  public static String chat_welcomeView_signInButton;
  public static String chat_welcomeView_signInButton_Tooltip;
  public static String chat_welcomeView_termsPrefix;
  public static String chat_welcomeView_termsLink;
  public static String chat_welcomeView_termsSuffix;
  public static String chat_welcomeView_privacyPolicyPrefix;
  public static String chat_welcomeView_privacyPolicyLink;
  public static String chat_welcomeView_privacyPolicySuffix;
  public static String chat_welcomeView_footerPublicCodePrefix;
  public static String chat_welcomeView_footerPublicCodeLink;
  public static String chat_welcomeView_footerPublicCodeSuffix;
  public static String chat_welcomeView_footerSettingsPrefix;
  public static String chat_welcomeView_footerSettingsLink;
  public static String chat_welcomeView_footerSettingsSuffix;
  public static String chat_aiWarn_description;
  public static String chat_initialChatView_title;
  public static String chat_initialChatView_attachContextSuffix;
  public static String chat_initialChatView_useCommandsIntro;
  public static String chat_agentModeView_title;
  public static String chat_agentModeView_agentModeIntro;
  public static String chat_agentModeView_configureMcpSuffix;
  public static String chat_agentModeView_attachContextSuffix;
  public static String chat_loadingView_title;
  public static String chat_loadingView_description;
  public static String chat_noAuthView_title;
  public static String chat_noAuthView_description;
  public static String chat_noAuthView_checkSubButton;
  public static String chat_noAuthView_checkSubButton_Tooltip;
  public static String chat_noAuthView_checkSubLink;
  public static String chat_addContext_tooltip;
  public static String chat_filePicker_title;
  public static String chat_filePicker_message;
  public static String chat_noQuotaView_updatePlanButton_Tooltip;
  public static String chat_noQuotaView_enableAdditionalUsageButton_tooltip;

  // TODO: Remove these legacy keys after TBB is officially released.
  // Legacy quota-warning keys used when tokenBasedBillingEnabled is false on the CheckQuotaResult,
  // in which case the chat warning falls back to the original main-branch behavior.
  public static String chat_noQuotaView_fallbackModel;
  public static String chat_noQuotaView_updatePlanButton;
  public static String chat_noQuotaView_updatePlanLink;
  public static String chat_noQuotaView_proProplusWarnMsg;
  public static String chat_noQuotaView_cbCeWarnMsg;

  public static String chat_currentReferencedFile_description;
  public static String chat_turnWidget_copilot;
  public static String chat_turnWidget_user;
  public static String chat_model_unsupported_message;
  public static String chat_copilotModels;
  public static String chat_standardModels;
  public static String chat_premiumModels;
  public static String chat_customModels;
  public static String chat_addPremiumModels;
  public static String chat_referencedFile_noVision_tooltip;
  public static String agent_tool_compareEditor_titlePrefix;
  public static String agent_tool_compareEditor_proposedChangesTitle;
  public static String agentFileEditor_contentAssist_statusMessage;
  public static String agentFileEditor_contentAssist_emptyMessage;
  public static String quickStart_title;
  public static String quickStart_description;
  public static String quickStart_closeButton_tooltip;
  public static String quickStart_continueButton;
  public static String quickStart_agent_title;
  public static String quickStart_agent_description;
  public static String quickStart_ask_title;
  public static String quickStart_ask_description;
  public static String quickStart_completion_title;
  public static String quickStart_completion_description;
  public static String generateCommitMessage_requiredBundlesMissing_title;
  public static String generateCommitMessage_requiredBundlesMissing_message;
  public static String generateCommitMessage_noRepo_title;
  public static String generateCommitMessage_noRepo_message;
  public static String generateCommitMessage_noStagedFiles_title;
  public static String generateCommitMessage_noStagedFiles_message;
  public static String addToReference_addFile_title;
  public static String addToReference_addFolder_title;
  public static String chat_historyView_backButton;
  public static String relative_dateFormat_today;
  public static String relative_dateFormat_yesterday;
  public static String relative_dateFormat_daysAgo;
  public static String relative_dateFormat_oneWeekAgo;
  public static String relative_dateFormat_weeksAgo;
  public static String relative_dateFormat_oneMonthAgo;
  public static String relative_dateFormat_monthsAgo;
  public static String chat_historyView_enterIcon_tooltip;
  public static String chat_historyView_editIcon_tooltip;
  public static String chat_historyView_deleteIcon_tooltip;
  public static String model_billing_multiplier_suffix;
  public static String model_billing_multiplier_variable;
  public static String model_preview_suffix;
  public static String model_hover_contextWindow;
  public static String model_hover_cost;
  public static String model_hover_thinkingEffort;
  public static String model_hover_thinkingEffort_default_suffix;
  public static String model_reasoningEffort_none;
  public static String model_reasoningEffort_low;
  public static String model_reasoningEffort_medium;
  public static String model_reasoningEffort_high;
  public static String model_reasoningEffort_xhigh;
  public static String model_reasoningEffort_none_description;
  public static String model_reasoningEffort_low_description;
  public static String model_reasoningEffort_medium_description;
  public static String model_reasoningEffort_high_description;
  public static String model_reasoningEffort_xhigh_description;
  public static String chat_actionBar_modePicker_Tooltip;
  public static String chat_actionBar_modelPicker_Tooltip;
  public static String context_window_title;
  public static String context_window_tokens;
  public static String context_window_system;
  public static String context_window_system_instructions;
  public static String context_window_tool_definitions;
  public static String context_window_user_context;
  public static String context_window_messages;
  public static String context_window_files;
  public static String context_window_tool_results;
  public static String chat_compacting_conversation;
  public static String chat_rateLimitBanner_getMoreInfo;
  public static String chat_rateLimitBanner_closeTooltip;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
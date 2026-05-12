// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.GC;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.Quota;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.MenuUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for showing GitHub Copilot menu bar menu.
 */
public class ShowMenuBarMenuHandler extends CompoundContributionItem implements IWorkbenchContribution {
  private IServiceLocator serviceLocator;
  private CommandContributionItem chatUsageItem;
  private CommandContributionItem completionsUsageItem;
  private CommandContributionItem premiumRequestsUsageItem;
  private CommandContributionItem allowanceResetItem;

  @Override
  public void initialize(IServiceLocator serviceLocator) {
    this.serviceLocator = serviceLocator;
    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    eventBroker.subscribe(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, event -> {
      Object data = event.getProperty(IEventBroker.DATA);
      if (data instanceof CopilotStatusResult statusResult && statusResult != null && statusResult.isNotSignedIn()) {
        if (chatUsageItem != null) {
          chatUsageItem.dispose();
          chatUsageItem = null;
        }
        if (completionsUsageItem != null) {
          completionsUsageItem.dispose();
          completionsUsageItem = null;
        }
        if (premiumRequestsUsageItem != null) {
          premiumRequestsUsageItem.dispose();
          premiumRequestsUsageItem = null;
        }
        if (allowanceResetItem != null) {
          allowanceResetItem.dispose();
          allowanceResetItem = null;
        }
      }
    });
  }

  @Override
  protected IContributionItem[] getContributionItems() {
    List<IContributionItem> items = new ArrayList<>();

    AuthStatusManager authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
    String status = authStatusManager != null ? authStatusManager.getCopilotStatus() : CopilotStatusResult.LOADING;

    // menu: username/Sign In
    if (CopilotStatusResult.NOT_SIGNED_IN.equals(status)) {
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.signIn", Messages.menu_signToGitHub,
          UiUtils.buildImageDescriptorFromPngPath("/icons/signin.png")));
    } else if (CopilotStatusResult.OK.equals(status)) {
      String userName = authStatusManager.getUserName();
      String planLabel = MenuUtils.getPlanLabel(authStatusManager.getQuotaStatus().copilotPlan());
      String userLabel = planLabel != null ? NLS.bind(Messages.menu_userPlanFormat, userName, planLabel) : userName;
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.disabledDoNothing",
          userLabel, userName, null));
    }

    // menu: copilot Usage
    addCopilotUsageItems(authStatusManager, items);

    // menu: openChatView
    items.add(new Separator());
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.openChatView", Messages.menu_openChatView,
        UiUtils.buildImageDescriptorFromPngPath("/icons/github_copilot.png")));

    // menu:(label options) Turn off Completions or Turn on Completions
    LanguageServerSettingManager languageServerSettingManager = CopilotUi.getPlugin().getLanguageServerSettingManager();
    if (languageServerSettingManager != null) {
      items.add(new Separator());
      String label = languageServerSettingManager.isAutoShowCompletionEnabled() ? Messages.menu_turnOffCompletions
          : Messages.menu_turnOnCompletions;
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.autoShowCompletions", label,
          UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png")));
    }

    // menu: editKeyboardShortcuts
    items.add(new Separator());
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.openEditKeyboardShortcuts",
        Messages.menu_editKeyboardShortcuts,
        UiUtils.buildImageDescriptorFromPngPath("/icons/edit_keyboard_shortcuts.png")));

    // menu: editPreferences
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.openPreferences", Messages.menu_editPreferences,
        UiUtils.buildImageDescriptorFromPngPath("/icons/edit_preferences.png")));

    // menu: giveFeedback
    items.add(new Separator());
    Map<String, String> parameters = Map.of(UiConstants.OPEN_URL_PARAMETER_NAME,
        UiConstants.COPILOT_FEEDBACK_FORUM_URL);
    items.add(createCommandItem(UiConstants.OPEN_URL_COMMAND_ID, Messages.menu_giveFeedback, parameters,
        UiUtils.buildImageDescriptorFromPngPath("/icons/feedback_forum.png")));

    // menu: whatIsNew
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.showWhatIsNew", Messages.menu_whatIsNew,
        UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png")));

    // menu: Copilot settings and Sign Out
    addAuthenticationActions(items, status);

    return items.toArray(new IContributionItem[0]);
  }

  private void addCopilotUsageItems(AuthStatusManager authStatusManager, List<IContributionItem> items) {
    // menu: Copilot useage
    CheckQuotaResult quotaStatus = authStatusManager.getQuotaStatus();
    if (authStatusManager.isNotSignedInOrNotAuthorized() || quotaStatus.completions() == null
        || quotaStatus.chat() == null || StringUtils.isEmpty(quotaStatus.resetDate())) {
      return;
    }
    // TODO: remove reset date null check when the CLS is ready for all IDEs.
    items.add(new Separator());

    if (quotaStatus.tokenBasedBillingEnabled()) {
      addCopilotUsageItemsTbb(items, quotaStatus);
    } else {
      // TODO: Remove this legacy branch after TBB is officially released.
      addCopilotUsageItemsLegacy(items, quotaStatus);
    }

    // Create a CompletableFuture to update quota information
    CopilotCore.getPlugin().getAuthStatusManager().checkQuota().thenAccept(this::updateQuotaItems);
  }

  /**
   * Renders the Copilot usage rows using the token-based-billing layout (Monthly Limit / Included
   * Credits, Enable Additional Usage / Increase Budget, Upgrade Plan, dynamic allowance reset).
   */
  private void addCopilotUsageItemsTbb(List<IContributionItem> items, CheckQuotaResult quotaStatus) {
    ImageDescriptor usageIcon = MenuUtils.getUsageIcon(MenuUtils.calculatePercentRemaining(quotaStatus));
    ImageDescriptor blankIcon = MenuUtils.getBlankIcon();
    CopilotPlan plan = quotaStatus.copilotPlan();
    Quota premiumQuota = quotaStatus.premiumInteractions();
    boolean isOrgUnlimited = MenuUtils.isOrgUnlimited(quotaStatus);
    boolean hasNonOrgPremiumQuota = MenuUtils.hasNonOrgPremiumQuota(quotaStatus);
    // For non-free plans with a Monthly limit row, the usage icon belongs on that row instead of the header.
    ImageDescriptor headerIcon = hasNonOrgPremiumQuota ? blankIcon : usageIcon;

    Map<String, String> parameters = Map.of(UiConstants.OPEN_URL_PARAMETER_NAME, UiConstants.MANAGE_COPILOT_URL);
    items.add(createCommandItem(UiConstants.OPEN_URL_COMMAND_ID, Messages.menu_quota_copilotUsage, parameters,
        Messages.menu_quota_manageCopilotTooltip, headerIcon));

    GC gc = new GC(PlatformUI.getWorkbench().getDisplay());
    QuotaTextCalculator calculator = new QuotaTextCalculator(gc, quotaStatus);
    try {
      if (plan == CopilotPlan.free) {
        // Free plan: only show Code Completions and Chat Messages rows
        this.completionsUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
            calculator.getCompletionText(), blankIcon);
        items.add(this.completionsUsageItem);

        this.chatUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
            calculator.getChatText(), blankIcon);
        items.add(this.chatUsageItem);
      } else if (isOrgUnlimited) {
        // Business / Enterprise with unlimited premium interactions: show informational message
        items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.disabledDoNothing",
            Messages.menu_quota_unlimitedOrgMessage, null));
      } else if (premiumQuota != null) {
        // Other paid plans: show only the Monthly limit row sourced from premium interactions
        this.premiumRequestsUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
            calculator.getPremiumRequestsText(), calculator.getPremiumRequestsTooltip(), usageIcon);
        items.add(this.premiumRequestsUsageItem);
      }
    } finally {
      gc.dispose();
    }

    // Allowance reset date
    if (MenuUtils.shouldShowAllowanceResetRow(quotaStatus)) {
      this.allowanceResetItem = createCommandItem("com.microsoft.copilot.eclipse.commands.disabledDoNothing",
          MenuUtils.formatAllowanceReset(quotaStatus), null);
      items.add(this.allowanceResetItem);
    }

    // Upsell actions based on the user's plan
    ImageDescriptor upgradeIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/quota/upgrade.png");

    // For non-free users (excluding org-unlimited business/enterprise):
    // show "Enable Additional Usage" or "Increase Budget" depending on overage state.
    // The overage row uses the same predicate as the Monthly limit row.
    if (hasNonOrgPremiumQuota) {
      items.add(createCommandItem(UiConstants.OPEN_URL_COMMAND_ID, MenuUtils.getOverageRowLabel(premiumQuota),
          Map.of(UiConstants.OPEN_URL_PARAMETER_NAME, UiConstants.MANAGE_COPILOT_OVERAGE_URL), upgradeIcon));
    }

    // For free / individual / individual_pro users, show an Upgrade Plan row. When the overage row is
    // already showing the upgrade icon directly above, this row uses the blank icon to avoid duplication.
    if (MenuUtils.shouldShowUpgradePlanRow(plan)) {
      ImageDescriptor upgradePlanIcon = hasNonOrgPremiumQuota ? blankIcon : upgradeIcon;
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.upgradeCopilotPlan",
          Messages.menu_quota_upgradePlan, upgradePlanIcon));
    }
  }

  // TODO: Remove this legacy fallback after TBB is officially released.
  /**
   * Renders the original main-branch Copilot usage rows. Used when the language server has not
   * enabled token-based billing yet, in which case the new TBB-only APIs (premium interactions
   * entitlement, overage budget UI, dynamic allowance-reset wording) are not relied on.
   */
  private void addCopilotUsageItemsLegacy(List<IContributionItem> items, CheckQuotaResult quotaStatus) {
    Quota completionsQuota = quotaStatus.completions();
    Quota chatQuota = quotaStatus.chat();
    Quota premiumQuota = quotaStatus.premiumInteractions();
    CopilotPlan plan = quotaStatus.copilotPlan();

    // Calculate percentRemaining based on plan
    double percentRemaining;
    if (plan == CopilotPlan.free) {
      percentRemaining = Math.min(completionsQuota.percentRemaining(), chatQuota.percentRemaining());
    } else if (premiumQuota != null) {
      percentRemaining = Math.min(completionsQuota.percentRemaining(),
          Math.min(chatQuota.percentRemaining(), premiumQuota.percentRemaining()));
    } else {
      percentRemaining = Math.min(completionsQuota.percentRemaining(), chatQuota.percentRemaining());
    }

    ImageDescriptor icon = MenuUtils.getUsageIcon(percentRemaining);
    ImageDescriptor blankIcon = MenuUtils.getBlankIcon();

    Map<String, String> parameters = Map.of(UiConstants.OPEN_URL_PARAMETER_NAME, UiConstants.MANAGE_COPILOT_URL);
    items.add(createCommandItem(UiConstants.OPEN_URL_COMMAND_ID, Messages.menu_quota_copilotUsage, parameters,
        Messages.menu_quota_manageCopilotTooltip, icon));

    GC gc = new GC(PlatformUI.getWorkbench().getDisplay());
    QuotaTextCalculator calculator = new QuotaTextCalculator(gc, quotaStatus);
    try {
      // Premium requests row first when both completion/chat quotas are unlimited.
      if (plan != CopilotPlan.free && premiumQuota != null && completionsQuota.unlimited() && chatQuota.unlimited()) {
        this.premiumRequestsUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
            calculator.getPremiumText(), blankIcon);
        items.add(this.premiumRequestsUsageItem);
      }

      // Code completions usage
      this.completionsUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
          calculator.getCompletionText(), blankIcon);
      items.add(this.completionsUsageItem);

      // Chat messages usage
      this.chatUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
          calculator.getChatText(), blankIcon);
      items.add(this.chatUsageItem);

      // Premium requests usage / additional-paid status for non-free plans.
      if (plan != CopilotPlan.free && premiumQuota != null) {
        if (!completionsQuota.unlimited() || !chatQuota.unlimited()) {
          this.premiumRequestsUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
              calculator.getPremiumText(), blankIcon);
          items.add(this.premiumRequestsUsageItem);
        }

        CommandContributionItem additionalPremiumRequestsDesc = createCommandItem(
            "com.microsoft.copilot.eclipse.commands.disabledDoNothing",
            Messages.menu_quota_additionalPremiumRequests
                + (premiumQuota.overagePermitted() ? Messages.menu_quota_enabled : Messages.menu_quota_disabled),
            null);
        items.add(additionalPremiumRequestsDesc);
      }
    } finally {
      gc.dispose();
    }

    // Allowance reset date (legacy: simple "Allowance resets <date>" string).
    if (!StringUtils.isEmpty(quotaStatus.resetDate())) {
      LocalDate resetDate = LocalDate.parse(quotaStatus.resetDate());
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.disabledDoNothing",
          Messages.menu_quota_allowanceReset + resetDate.format(formatter), null));
    }

    // Upsell actions based on the user's plan (legacy wording).
    ImageDescriptor upgradeIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/quota/upgrade.png");
    if (plan == CopilotPlan.free) {
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.upgradeCopilotPlan",
          Messages.menu_quota_updateCopilotToPro, Messages.menu_quota_updateCopilotToProPlus, upgradeIcon));
    } else if (plan != CopilotPlan.business && plan != CopilotPlan.enterprise) {
      items.add(createCommandItem(UiConstants.OPEN_URL_COMMAND_ID, Messages.menu_quota_managePaidPremiumRequests,
          Map.of(UiConstants.OPEN_URL_PARAMETER_NAME, UiConstants.MANAGE_COPILOT_OVERAGE_URL), upgradeIcon));
    }
  }

  private void addAuthenticationActions(List<IContributionItem> items, String status) {
    if (CopilotStatusResult.LOADING.equals(status) || CopilotStatusResult.NOT_SIGNED_IN.equals(status)) {
      return;
    }
    items.add(new Separator());
    if (CopilotStatusResult.NOT_AUTHORIZED.equals(status)) {
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.configureCopilotSettings",
          Messages.menu_configureGitHubCopilotSettings, null));
    }
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.signOut", Messages.menu_signOutOfGitHub,
        UiUtils.buildImageDescriptorFromPngPath("/icons/signout.png")));
  }

  /**
   * Updates the quota items with the latest quota information.
   *
   * @param quotaResult The latest quota information.
   */
  private void updateQuotaItems(CheckQuotaResult quotaResult) {
    if (quotaResult == null) {
      return;
    }

    SwtUtils.invokeOnDisplayThread(() -> {
      GC gc = new GC(PlatformUI.getWorkbench().getDisplay());
      try {
        updateQuotaActionTexts(quotaResult, gc);
      } finally {
        gc.dispose();
      }
    });
  }

  private void updateQuotaActionTexts(CheckQuotaResult quotaResult, GC gc) {
    QuotaTextCalculator calculator = new QuotaTextCalculator(gc, quotaResult);
    boolean tbbEnabled = quotaResult.tokenBasedBillingEnabled();

    if (this.chatUsageItem != null && quotaResult.chat() != null) {
      String chatMessagesText = calculator.getChatText();
      setCommandItemField(this.chatUsageItem, "label", chatMessagesText);
    }

    if (this.completionsUsageItem != null && quotaResult.completions() != null) {
      String codeCompletionsText = calculator.getCompletionText();
      setCommandItemField(this.completionsUsageItem, "label", codeCompletionsText);
    }

    if (this.premiumRequestsUsageItem != null && quotaResult.premiumInteractions() != null) {
      if (tbbEnabled) {
        String monthlyLimitText = calculator.getPremiumRequestsText();
        setCommandItemField(this.premiumRequestsUsageItem, "label", monthlyLimitText);
        // Refresh the usage icon (red/yellow/blue) to reflect the latest percent remaining.
        setCommandItemField(this.premiumRequestsUsageItem, "icon",
            MenuUtils.getUsageIcon(MenuUtils.calculatePercentRemaining(quotaResult)));
      } else {
        // TODO: Remove this legacy fallback after TBB is officially released.
        setCommandItemField(this.premiumRequestsUsageItem, "label", calculator.getPremiumText());
      }
    }

    // Refresh the allowance-reset row label, which switches between "Reset in N days..." and
    // "No usage yet" depending on whether any of the tracked quotas have been consumed. When the
    // predicate flips off (e.g. plan changed to unlimited mid-session), skip the update and leave
    // the stale label until the menu is rebuilt rather than rendering an empty disabled row.
    if (tbbEnabled && this.allowanceResetItem != null && MenuUtils.shouldShowAllowanceResetRow(quotaResult)) {
      setCommandItemField(this.allowanceResetItem, "label", MenuUtils.formatAllowanceReset(quotaResult));
      this.allowanceResetItem.update();
    }

    if (this.chatUsageItem != null) {
      this.chatUsageItem.update();
    }
    if (this.completionsUsageItem != null) {
      this.completionsUsageItem.update();
    }
    if (this.premiumRequestsUsageItem != null) {
      this.premiumRequestsUsageItem.update();
    }
  }

  /**
   * Reflectively assigns a private field on {@link CommandContributionItem}. The platform does not
   * expose mutators for {@code label}/{@code icon}, so this is used to refresh the menu entries in
   * place. A failure here means a future Eclipse version renamed the field and the menu will stop
   * refreshing - log it so the regression is visible.
   */
  private void setCommandItemField(CommandContributionItem item, String fieldName, Object value) {
    try {
      Field field = CommandContributionItem.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(item, value);
    } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
      CopilotCore.LOGGER.error("Failed to update CommandContributionItem field '" + fieldName + "'", e);
    }
  }

  private CommandContributionItem createCommandItem(String commandId, String label, Map<String, String> parameters,
      ImageDescriptor icon) {
    return createCommandItem(commandId, label, parameters, null, icon);
  }

  private CommandContributionItem createCommandItem(String commandId, String label, ImageDescriptor icon) {
    return createCommandItem(commandId, label, null, null, icon);
  }

  private CommandContributionItem createCommandItem(String commandId, String label, String tooltip,
      ImageDescriptor icon) {
    return createCommandItem(commandId, label, null, tooltip, icon);
  }

  private CommandContributionItem createCommandItem(String commandId, String label, Map<String, String> parameters,
      String tooltip, ImageDescriptor icon) {
    CommandContributionItemParameter parameter = createCommandContributionItemParameter(commandId, label, parameters,
        tooltip, icon);

    return new CommandContributionItem(parameter);
  }

  private CommandContributionItemParameter createCommandContributionItemParameter(String commandId, String label,
      Map<String, String> parameters, String tooltip, ImageDescriptor icon) {
    CommandContributionItemParameter parameter = new CommandContributionItemParameter(serviceLocator, null, commandId,
        CommandContributionItem.STYLE_PUSH);
    if (icon != null) {
      parameter.icon = icon;
    } else {
      setDefaultBlankIcon(parameter);
    }

    if (label != null) {
      parameter.label = label;
    }

    if (tooltip != null) {
      parameter.tooltip = tooltip;
    }

    if (parameters != null && !parameters.isEmpty()) {
      parameter.parameters = parameters;
    }

    return parameter;
  }

  private void setDefaultBlankIcon(CommandContributionItemParameter parameter) {
    if (PlatformUtils.isMac()) {
      parameter.icon = MenuUtils.getBlankIcon();
    }
  }
}

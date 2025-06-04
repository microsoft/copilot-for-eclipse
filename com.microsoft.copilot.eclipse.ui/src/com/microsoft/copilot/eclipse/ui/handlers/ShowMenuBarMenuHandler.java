package com.microsoft.copilot.eclipse.ui.handlers;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.Quota;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
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

  @Override
  public void initialize(IServiceLocator serviceLocator) {
    this.serviceLocator = serviceLocator;
  }

  @Override
  protected IContributionItem[] getContributionItems() {
    List<IContributionItem> items = new ArrayList<>();

    // menu: openChatView
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.openChatView", Messages.menu_openChatView,
        UiUtils.buildImageDescriptorFromPngPath("/icons/chat/github_copilot_chat.png")));
    items.add(new Separator());

    // menu:(label options) enableCompletions or disableCompletions
    LanguageServerSettingManager languageServerSettingManager = CopilotUi.getPlugin().getLanguageServerSettingManager();
    if (languageServerSettingManager != null) {
      String label = languageServerSettingManager.isAutoShowCompletionEnabled() ? Messages.menu_disableCompletions
          : Messages.menu_enableCompletions;
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.autoShowCompletions", label,
          UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png")));
      items.add(new Separator());
    }

    // menu: editPreferences
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.openPreferences", Messages.menu_editPreferences,
        UiUtils.buildImageDescriptorFromPngPath("/icons/edit_preferences.png")));

    // menu: editKeyboardShortcuts
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.openEditKeyboardShortcuts",
        Messages.menu_editKeyboardShortcuts,
        UiUtils.buildImageDescriptorFromPngPath("/icons/edit_keyboard_shortcuts.png")));
    items.add(new Separator());

    // menu: viewFeedbackForum
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.viewFeedbackForum",
        Messages.menu_viewFeedbackForum, UiUtils.buildImageDescriptorFromPngPath("/icons/feedback_forum.png")));

    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.showWhatIsNew", Messages.menu_whatIsNew,
        UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png")));
    items.add(new Separator());

    AuthStatusManager authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
    String status = authStatusManager != null ? authStatusManager.getCopilotStatus() : CopilotStatusResult.LOADING;

    // menu: configureGitHubCopilotSettings
    if (CopilotStatusResult.NOT_AUTHORIZED.equals(status)) {
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.configureCopilotSettings",
          Messages.menu_configureGitHubCopilotSettings, null));
    }

    // menu:(command options) signToGitHub or signOutFromGitHub
    if (CopilotStatusResult.NOT_SIGNED_IN.equals(status)) {
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.signIn", Messages.menu_signToGitHub,
          UiUtils.buildImageDescriptorFromPngPath("/icons/signin.png")));
    } else if (!CopilotStatusResult.LOADING.equals(status)) {
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.signOut", Messages.menu_signOutFromGitHub,
          UiUtils.buildImageDescriptorFromPngPath("/icons/signout.png")));
    }
    addCopilotUsageItems(authStatusManager, items);
    return items.toArray(new IContributionItem[0]);
  }

  private void addCopilotUsageItems(AuthStatusManager authStatusManager, List<IContributionItem> items) {
    // menu: Copilot useage
    CheckQuotaResult quotaStatus = authStatusManager.getQuotaStatus();
    if (authStatusManager.isNotSignedInOrNotAuthorized() || quotaStatus.getCompletionsQuota() == null
        || quotaStatus.getChatQuota() == null || StringUtils.isEmpty(quotaStatus.getResetDate())) {
      return;
    }
    // TODO: remove reset date null check when the CLS is ready for all IDEs.
    items.add(new Separator());
    // Calculate percentRemaining based on plan
    double percentRemaining;
    if (quotaStatus.getCopilotPlan() == CopilotPlan.free) {
      // For free plan, consider completions and chat quotas
      percentRemaining = Math.min(quotaStatus.getCompletionsQuota().getPercentRemaining(),
          quotaStatus.getChatQuota().getPercentRemaining());
    } else {
      // For paid plans, also consider premium interactions quota
      if (quotaStatus.getCompletionsQuota() == null) {
        // If completions quota is not available, set percentRemaining to 0
        percentRemaining = 0;
      } else {
        percentRemaining = Math.min(quotaStatus.getCompletionsQuota().getPercentRemaining(),
            Math.min(quotaStatus.getChatQuota().getPercentRemaining(),
                quotaStatus.getPremiumInteractionsQuota().getPercentRemaining()));
      }
    }

    ImageDescriptor icon;
    // Set icon based on percentRemaining
    if (percentRemaining >= 90) {
      icon = UiUtils.buildImageDescriptorFromPngPath("/icons/quota/usage_blue.png");
    } else if (percentRemaining >= 75) {
      icon = UiUtils.buildImageDescriptorFromPngPath("/icons/quota/usage_yellow.png");
    } else {
      icon = UiUtils.buildImageDescriptorFromPngPath("/icons/quota/usage_red.png");
    }

    items.add(createCommandItemWithTooltip("com.microsoft.copilot.eclipse.commands.manageCopilot",
        Messages.menu_quota_copilotUsage, Messages.menu_quota_manageCopilotTooltip, icon));

    // Premium requests usage when rest plans are unlimited
    if (quotaStatus.getCopilotPlan() != CopilotPlan.free && quotaStatus.getCompletionsQuota().isUnlimited()
        && quotaStatus.getChatQuota().isUnlimited()) {
      String premiumRequestsText = Messages.menu_quota_premiumRequests
          + getPercentRemaining(quotaStatus.getPremiumInteractionsQuota());
      this.premiumRequestsUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
          premiumRequestsText, UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png"));
      items.add(this.premiumRequestsUsageItem);
    }

    // Code completions useage
    String codeCompletionsText = Messages.menu_quota_codeCompletions
        + getPercentRemaining(quotaStatus.getCompletionsQuota());
    this.completionsUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
        codeCompletionsText, UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png"));
    items.add(this.completionsUsageItem);

    // Chat messages usage
    String chatMessagesText = Messages.menu_quota_chatMessages + getPercentRemaining(quotaStatus.getChatQuota());
    this.chatUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing", chatMessagesText,
        UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png"));
    items.add(this.chatUsageItem);

    // Premium requests usage
    if (quotaStatus.getCopilotPlan() != CopilotPlan.free) {
      // Premium requests usage when either of the rest plans is not unlimited
      if (!quotaStatus.getCompletionsQuota().isUnlimited() || !quotaStatus.getChatQuota().isUnlimited()) {
        String premiumRequestsText = Messages.menu_quota_premiumRequests
            + getPercentRemaining(quotaStatus.getPremiumInteractionsQuota());
        this.premiumRequestsUsageItem = createCommandItem("com.microsoft.copilot.eclipse.commands.enabledDoNothing",
            premiumRequestsText, UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png"));
        items.add(this.premiumRequestsUsageItem);
      }

      CommandContributionItem additionalPremiumRequestsDesc = createCommandItem(
          "com.microsoft.copilot.eclipse.commands.disabledDoNothing",
          Messages.menu_quota_additionalPremiumRequests
              + (quotaStatus.getPremiumInteractionsQuota().isOveragePermitted() ? Messages.menu_quota_enabled
                  : Messages.menu_quota_disabled),
          null);
      items.add(additionalPremiumRequestsDesc);
    }

    // Allowance reset date
    if (!StringUtils.isEmpty(quotaStatus.getResetDate())) {
      LocalDate resetDate = LocalDate.parse(quotaStatus.getResetDate());
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
      items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.disabledDoNothing",
          Messages.menu_quota_allowanceReset + resetDate.format(formatter), null));
    }

    // Upsell actions based on the user's plan
    ImageDescriptor upgradeIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/quota/upgrade.png");
    if (quotaStatus.getCopilotPlan() == CopilotPlan.free) {
      // If the user is on a free plan, show a link to upgrade.
      items.add(createCommandItemWithTooltip("com.microsoft.copilot.eclipse.commands.upgradeCopilotPlan",
          Messages.menu_quota_updateCopilotToPro, Messages.menu_quota_updateCopilotToProPlus, upgradeIcon));
    } else if (quotaStatus.getCopilotPlan() != CopilotPlan.business
        && quotaStatus.getCopilotPlan() != CopilotPlan.enterprise) {
      // If the user is not on a free plan / business plan / enterprise plan, show a link to manage subscription.
      items.add(createCommandItemWithTooltip("com.microsoft.copilot.eclipse.commands.upgradeCopilotPlan",
          Messages.menu_quota_updateCopilotToProPlus, Messages.menu_quota_updateCopilotToProPlus, upgradeIcon));
    }
    // Create a CompletableFuture to update quota information
    CopilotCore.getPlugin().getAuthStatusManager().checkQuota().thenAccept(this::updateQuotaItems);
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
    if (this.chatUsageItem != null && quotaResult.getChatQuota() != null) {
      String chatMessagesText = Messages.menu_quota_chatMessages + getPercentRemaining(quotaResult.getChatQuota());
      updateCommandItemLabel(this.chatUsageItem, chatMessagesText);
    }

    if (this.completionsUsageItem != null && quotaResult.getCompletionsQuota() != null) {
      String codeCompletionsText = Messages.menu_quota_codeCompletions
          + getPercentRemaining(quotaResult.getCompletionsQuota());
      updateCommandItemLabel(this.completionsUsageItem, codeCompletionsText);
    }

    if (this.premiumRequestsUsageItem != null && quotaResult.getPremiumInteractionsQuota() != null) {
      String premiumRequestsText = Messages.menu_quota_premiumRequests
          + getPercentRemaining(quotaResult.getPremiumInteractionsQuota());
      updateCommandItemLabel(this.premiumRequestsUsageItem, premiumRequestsText);
    }

    if (this.chatUsageItem != null || this.completionsUsageItem != null || this.premiumRequestsUsageItem != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        if (this.chatUsageItem != null) {
          this.chatUsageItem.update();
        }
        if (this.completionsUsageItem != null) {
          this.completionsUsageItem.update();
        }
        if (this.premiumRequestsUsageItem != null) {
          this.premiumRequestsUsageItem.update();
        }
      });
    }
  }

  /**
   * Updates the label of a CommandContributionItem.
   *
   * @param item The CommandContributionItem to update
   * @param newLabel The new label to set
   */
  private void updateCommandItemLabel(CommandContributionItem item, String newLabel) {
    try {
      Field labelField = CommandContributionItem.class.getDeclaredField("label");
      labelField.setAccessible(true);
      labelField.set(item, newLabel);
    } catch (Exception e) {
      // Skip updating the label if reflection fails
    }
  }

  private String getPercentRemaining(Quota quota) {
    if (quota.isUnlimited()) {
      return "Included";
    }
    double percent = Math.max(0, 100 - quota.getPercentRemaining());
    if (percent == 0.0) {
      return "0%";
    }
    return String.format("%.1f", percent) + "%";
  }

  private CommandContributionItem createCommandItem(String commandId, String label, ImageDescriptor icon) {
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

    return new CommandContributionItem(parameter);
  }

  private CommandContributionItem createCommandItemWithTooltip(String commandId, String label, String tooltip,
      ImageDescriptor icon) {
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

    return new CommandContributionItem(parameter);
  }

  private void setDefaultBlankIcon(CommandContributionItemParameter parameter) {
    ImageDescriptor icon = UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png");
    if (PlatformUtils.isMac()) {
      parameter.icon = icon;
    }
  }
}
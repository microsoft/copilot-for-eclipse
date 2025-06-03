package com.microsoft.copilot.eclipse.ui.handlers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.UIElement;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.Quota;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotStatusManager;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for showing GitHub Copilot status bar menu.
 */
public class ShowStatusBarMenuHandler extends CopilotHandler implements IElementUpdater {
  private IHandlerService handlerService;
  private AuthStatusManager authStatusManager;
  private LanguageServerSettingManager languageServerSettingManager;
  private SpinnerJob spinnerJob;

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    handlerService = HandlerUtil.getActiveWorkbenchWindow(event).getService(IHandlerService.class);
    authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
    languageServerSettingManager = CopilotUi.getPlugin().getLanguageServerSettingManager();

    MenuManager menuManager = new MenuManager();
    // Sign in status section
    addStatusAction(menuManager);

    // Copilot usage section
    if (!authStatusManager.isNotSignedInOrNotAuthorized()) {
      menuManager.add(new Separator("copilotUsageGroup"));
      addCopilotUsageAction(menuManager);
    }

    // Sign in & sign out section
    menuManager.add(new Separator());
    if (!Objects.equals(authStatusManager.getCopilotStatus(), CopilotStatusResult.LOADING)) {
      addAuthenticationActions(menuManager);
    }

    // Provide feedback section
    menuManager.add(new Separator());
    addLinkToFeedbackForumAction(menuManager);
    addShowWhatIsNewAction(menuManager);

    // Preferences section
    menuManager.add(new Separator());
    addEditKeyboardShortcutsAction(menuManager);
    addPreferencesAction(menuManager);

    // Completion settings section
    menuManager.add(new Separator());
    addCompletionSettingsAction(menuManager);

    // Open Copilot chat view section
    menuManager.add(new Separator());
    addOpenChatViewAction(menuManager);

    Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
    Menu menu = menuManager.createContextMenu(shell);
    menu.setVisible(true);
    return null;
  }

  @Override
  public void updateElement(UIElement element, Map parameters) {
    CopilotStatusManager copilotStatusManager = getCopilotStatusManager();

    if (copilotStatusManager == null || copilotStatusManager.isCompletionInProgress()) {
      scheduleSpinnerJob(element);
      return;
    } else {
      // Since spinner job has 100ms delay, cancel the spinner job if it is running to avoid flickering.
      if (spinnerJob != null) {
        spinnerJob.cancel();
      }

      AuthStatusManager authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
      if (authStatusManager == null) {
        scheduleSpinnerJob(element);
        return;
      } else {
        String copilotStatus = authStatusManager.getCopilotStatus();
        String iconPath = null;

        switch (copilotStatus) {
          case CopilotStatusResult.OK:
            iconPath = "/icons/github_copilot_signed_in.png";
            break;
          case CopilotStatusResult.LOADING:
            scheduleSpinnerJob(element);
            return;
          case CopilotStatusResult.ERROR, CopilotStatusResult.WARNING:
            iconPath = "/icons/github_copilot_error.png";
            break;
          case CopilotStatusResult.NOT_AUTHORIZED:
            iconPath = "/icons/github_copilot_not_authorized.png";
            break;
          case CopilotStatusResult.NOT_SIGNED_IN:
          default:
            iconPath = "/icons/github_copilot_not_signed_in.png";
        }
        setIconOnDisplayThread(element, iconPath);
      }
    }
  }

  private void setIconOnDisplayThread(UIElement element, String iconPath) {
    if (iconPath != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        ImageDescriptor newIcon = UiUtils.buildImageDescriptorFromPngPath(iconPath);
        element.setIcon(newIcon);
      });
    }
  }

  private void addStatusAction(MenuManager menuManager) {
    String copilotStatus = getCopilotStatusBasedOnAuthAndCompletionResult(authStatusManager.getCopilotStatus());
    if (Objects.equals(CopilotStatusResult.OK, authStatusManager.getCopilotStatus())
        && !authStatusManager.getUserName().isBlank()) {
      copilotStatus += " (" + authStatusManager.getUserName() + ")";
    }
    String copilotStatusTitle = Messages.menu_copilotStatus + ": " + copilotStatus;

    MenuActionFactory.createMenuAction(menuManager, copilotStatusTitle, handlerService, copilotStatus, false);
  }

  private void addCopilotUsageAction(MenuManager menuManager) {
    CheckQuotaResult quotaStatus = CopilotCore.getPlugin().getAuthStatusManager().getQuotaStatus();
    if (quotaStatus.getCompletionsQuota() == null || quotaStatus.getChatQuota() == null
        || StringUtils.isEmpty(quotaStatus.getResetDate())) {
      // skip quota status menu if quotas are not available
      // TODO: remove reset date null check when the CLS is ready for all IDEs.
      return;
    }

    // Calculate percentRemaining based on plan
    double percentRemaining;
    if (quotaStatus.getCopilotPlan() == CopilotPlan.free) {
      // For free plan, consider completions and chat quotas
      percentRemaining = Math.min(quotaStatus.getCompletionsQuota().getPercentRemaining(),
          quotaStatus.getChatQuota().getPercentRemaining());
    } else {
      // For paid plans, also consider premium interactions quota
      percentRemaining = Math.min(quotaStatus.getCompletionsQuota().getPercentRemaining(),
          Math.min(quotaStatus.getChatQuota().getPercentRemaining(),
              quotaStatus.getPremiumInteractionsQuota().getPercentRemaining()));
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

    MenuActionFactory.createMenuActionWithTooltipText(menuManager, Messages.menu_quota_copilotUsage,
        Messages.menu_quota_manageCopilotTooltip, icon, handlerService,
        "com.microsoft.copilot.eclipse.commands.manageCopilot", true);

    // Premium requests usage when rest plans are unlimited
    if (quotaStatus.getCopilotPlan() != CopilotPlan.free && quotaStatus.getCompletionsQuota().isUnlimited()
        && quotaStatus.getChatQuota().isUnlimited()) {
      String premiumRequestsText = Messages.menu_quota_premiumRequests
          + getPercentRemaining(quotaStatus.getPremiumInteractionsQuota());
      MenuActionFactory.createMenuAction(menuManager, premiumRequestsText,
          UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png"), handlerService,
          "com.microsoft.copilot.eclipse.commands.enabledDoNothing", true);
    }

    // Code completions usage
    String codeCompletionsText = Messages.menu_quota_codeCompletions
        + getPercentRemaining(quotaStatus.getCompletionsQuota());
    MenuActionFactory.createMenuAction(menuManager, codeCompletionsText,
        UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png"), handlerService,
        "com.microsoft.copilot.eclipse.commands.enabledDoNothing", true);

    // Chat messages usage
    String chatMessagesText = Messages.menu_quota_chatMessages + getPercentRemaining(quotaStatus.getChatQuota());
    MenuActionFactory.createMenuAction(menuManager, chatMessagesText,
        UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png"), handlerService,
        "com.microsoft.copilot.eclipse.commands.enabledDoNothing", true);

    // Premium requests usage
    if (quotaStatus.getCopilotPlan() != CopilotPlan.free) {
      // Premium requests usage when either of the rest plans is not unlimited
      if (!quotaStatus.getCompletionsQuota().isUnlimited() || !quotaStatus.getChatQuota().isUnlimited()) {
        String premiumRequestsText = Messages.menu_quota_premiumRequests
            + getPercentRemaining(quotaStatus.getPremiumInteractionsQuota());
        MenuActionFactory.createMenuAction(menuManager, premiumRequestsText,
            UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png"), handlerService,
            "com.microsoft.copilot.eclipse.commands.enabledDoNothing", true);
      }

      MenuActionFactory.createMenuAction(menuManager,
          Messages.menu_quota_additionalPremiumRequests
              + (quotaStatus.getPremiumInteractionsQuota().isOveragePermitted() ? Messages.menu_quota_enabled
                  : Messages.menu_quota_disabled),
          handlerService, "com.microsoft.copilot.eclipse.commands.disabledDoNothing", false);
    }

    // Allowance reset date
    if (!StringUtils.isEmpty(quotaStatus.getResetDate())) {
      LocalDate resetDate = LocalDate.parse(quotaStatus.getResetDate());
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_quota_allowanceReset + resetDate.format(formatter),
          handlerService, "com.microsoft.copilot.eclipse.commands.disabledDoNothing", false);
    }

    // Upsell actions based on the user's plan
    ImageDescriptor upgradeIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/quota/upgrade.png");
    if (quotaStatus.getCopilotPlan() == CopilotPlan.free) {
      // If the user is on a free plan, show a link to upgrade.
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_quota_updateCopilotToPro, upgradeIcon,
          handlerService, "com.microsoft.copilot.eclipse.commands.upgradeCopilotPlan", true);
    } else if (quotaStatus.getCopilotPlan() != CopilotPlan.business
        && quotaStatus.getCopilotPlan() != CopilotPlan.enterprise) {
      // If the user is not on a free plan / business plan / enterprise plan, show a link to manage subscription.
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_quota_managePaidPremiumRequests, upgradeIcon,
          handlerService, "com.microsoft.copilot.eclipse.commands.manageCopilotOverage", true);
    }
  }

  private void addOpenChatViewAction(MenuManager menuManager) {
    ImageDescriptor icon = UiUtils.buildImageDescriptorFromPngPath("/icons/chat/github_copilot_chat.png");
    MenuActionFactory.createMenuAction(menuManager, Messages.menu_openChatView, icon, handlerService,
        "com.microsoft.copilot.eclipse.commands.openChatView", true);
  }

  private void addLinkToFeedbackForumAction(MenuManager menuManager) {
    ImageDescriptor feedbackIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/feedback_forum.png");
    MenuActionFactory.createMenuAction(menuManager, Messages.menu_viewFeedbackForum, feedbackIcon, handlerService,
        "com.microsoft.copilot.eclipse.commands.viewFeedbackForum", true);
  }

  private void addPreferencesAction(MenuManager menuManager) {
    ImageDescriptor editPreferencesIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/edit_preferences.png");
    MenuActionFactory.createMenuAction(menuManager, Messages.menu_editPreferences, editPreferencesIcon, handlerService,
        "com.microsoft.copilot.eclipse.commands.openPreferences", true);
  }

  private void addEditKeyboardShortcutsAction(MenuManager menuManager) {
    ImageDescriptor editKeyboardShortcutsIcon = UiUtils
        .buildImageDescriptorFromPngPath("/icons/edit_keyboard_shortcuts.png");
    MenuActionFactory.createMenuAction(menuManager, Messages.menu_editKeyboardShortcuts, editKeyboardShortcutsIcon,
        handlerService, "com.microsoft.copilot.eclipse.commands.openEditKeyboardShortcuts", true);
  }

  private void addCompletionSettingsAction(MenuManager menuManager) {
    ImageDescriptor placeHolder = UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png");
    if (languageServerSettingManager.isAutoShowCompletionEnabled()) {
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_disableCompletions, placeHolder, handlerService,
          "com.microsoft.copilot.eclipse.commands.autoShowCompletions", true);
    } else {
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_enableCompletions, placeHolder, handlerService,
          "com.microsoft.copilot.eclipse.commands.autoShowCompletions", true);
    }
  }

  private void addShowWhatIsNewAction(MenuManager menuManager) {
    ImageDescriptor placeHolder = UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png");
    MenuActionFactory.createMenuAction(menuManager, Messages.menu_whatIsNew, placeHolder, handlerService,
        "com.microsoft.copilot.eclipse.commands.showWhatIsNew", true);
  }

  private String getCopilotStatusBasedOnAuthAndCompletionResult(String copilotStatus) {
    CopilotStatusManager copilotStatusManager = getCopilotStatusManager();
    switch (copilotStatus) {
      case CopilotStatusResult.OK:
        return copilotStatusManager.isCompletionInProgress() ? Messages.menu_copilotStatus_completionInProgress
            : Messages.menu_copilotStatus_ready;
      case CopilotStatusResult.ERROR:
        return Messages.menu_copilotStatus_unknownError;
      case CopilotStatusResult.LOADING:
        return Messages.menu_copilotStatus_loading;
      case CopilotStatusResult.NOT_SIGNED_IN:
        return Messages.menu_copilotStatus_notSignedInToGitHub;
      case CopilotStatusResult.WARNING:
        return Messages.menu_copilotStatus_agentWarning;
      case CopilotStatusResult.NOT_AUTHORIZED:
        return Messages.menu_copilotStatus_notAuthorized;
      default:
        return Messages.menu_copilotStatus_loading;
    }
  }

  private void scheduleSpinnerJob(UIElement uiElement) {
    if (spinnerJob != null) {
      spinnerJob.cancel();
    } else {
      spinnerJob = new SpinnerJob();
    }
    spinnerJob.setTargetUiElement(uiElement);
    spinnerJob.schedule();
  }

  private void addAuthenticationActions(MenuManager menuManager) {
    if (Objects.equals(authStatusManager.getCopilotStatus(), CopilotStatusResult.NOT_AUTHORIZED)) {
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_configureGitHubCopilotSettings, null,
          handlerService, "com.microsoft.copilot.eclipse.commands.configureCopilotSettings", true);
    }

    if (Objects.equals(authStatusManager.getCopilotStatus(), CopilotStatusResult.NOT_SIGNED_IN)) {
      ImageDescriptor signInIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/signin.png");
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_signToGitHub, signInIcon, handlerService,
          "com.microsoft.copilot.eclipse.commands.signIn", true);
    } else if (!Objects.equals(authStatusManager.getCopilotStatus(), CopilotStatusResult.LOADING)) {
      // Only show sign out action when the user is in OK, NOT_AUTHORIZED, WARNING, or ERROR state.
      ImageDescriptor signOutIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/signout.png");
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_signOutFromGitHub, signOutIcon, handlerService,
          "com.microsoft.copilot.eclipse.commands.signOut", true);
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

  private static class MenuActionFactory {
    public static void createMenuAction(MenuManager menuManager, String actionName, ImageDescriptor icon,
        IHandlerService handlerService, String commandId, boolean enabled) {
      Action action = new Action(actionName, icon) {
        @Override
        public void run() {
          try {
            handlerService.executeCommand(commandId, null);
          } catch (Exception e) {
            CopilotCore.LOGGER.error(e);
          }
        }
      };
      action.setEnabled(enabled);
      if (icon == null) {
        setDefaultBlankIcon(action);
      }
      menuManager.add(action);
    }

    public static void createMenuAction(MenuManager menuManager, String text, IHandlerService handlerService,
        String commandId, boolean enabled) {
      createMenuAction(menuManager, text, null, handlerService, commandId, enabled);
    }

    public static void createMenuActionWithTooltipText(MenuManager menuManager, String text, String tooltipText,
        ImageDescriptor icon, IHandlerService handlerService, String commandId, boolean enabled) {
      Action action = new Action(text, icon) {
        @Override
        public void run() {
          try {
            handlerService.executeCommand(commandId, null);
          } catch (Exception e) {
            CopilotCore.LOGGER.error(e);
          }
        }
      };
      action.setEnabled(enabled);
      action.setToolTipText(tooltipText);
      if (icon == null) {
        setDefaultBlankIcon(action);
      }
      menuManager.add(action);
    }
  }

  private static void setDefaultBlankIcon(Action action) {
    ImageDescriptor blankIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png");
    if (PlatformUtils.isMac()) {
      action.setImageDescriptor(blankIcon);
    }
  }

  private class SpinnerJob extends Job {
    private static final int INITIAL_ICON_INDEX = 1;
    private static final int TOTAL_SPINNER_ICONS = 8;
    private static final long COMPLETION_IN_PROGRESS_SPINNER_ROTATE_RATE_MILLIS = 100L;

    private int currentIconIndex = INITIAL_ICON_INDEX;
    private UIElement uiElement;

    public SpinnerJob() {
      super("Spinner Job");
      this.setSystem(true);
    }

    public void setTargetUiElement(UIElement uiElement) {
      this.uiElement = uiElement;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        if (this.uiElement == null) {
          throw new IllegalStateException("UI element is not set. Spinner cannot be set.");
        }
        setIconOnDisplayThread(this.uiElement, String.format("/icons/spinner/%d.png", currentIconIndex));
        currentIconIndex = (currentIconIndex % TOTAL_SPINNER_ICONS) + 1;
        if (CopilotUi.getPlugin().getCopilotStatusManager() != null
            && CopilotUi.getPlugin().getCopilotStatusManager().isCompletionInProgress()
            || CopilotCore.getPlugin().getAuthStatusManager() != null
                && CopilotCore.getPlugin().getAuthStatusManager().isLoading()) {
          schedule(COMPLETION_IN_PROGRESS_SPINNER_ROTATE_RATE_MILLIS);
        } else {
          cancel();
        }
      } catch (Exception e) {
        CopilotCore.LOGGER.error(e);
        return Status.CANCEL_STATUS;
      }
      return Status.OK_STATUS;
    }
  }
}

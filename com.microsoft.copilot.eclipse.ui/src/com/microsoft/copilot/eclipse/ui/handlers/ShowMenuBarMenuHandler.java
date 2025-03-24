package com.microsoft.copilot.eclipse.ui.handlers;

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
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for showing GitHub Copilot menu bar menu.
 */
public class ShowMenuBarMenuHandler extends CompoundContributionItem implements IWorkbenchContribution {
  private IServiceLocator serviceLocator;

  @Override
  public void initialize(IServiceLocator serviceLocator) {
    this.serviceLocator = serviceLocator;
  }

  @Override
  protected IContributionItem[] getContributionItems() {
    java.util.List<IContributionItem> items = new java.util.ArrayList<>();

    // menu: openChatView
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.openChatView", Messages.menu_openChatView,
        UiUtils.buildImageDescriptorFromPngPath("/icons/chat/github_copilot_chat.png")));
    items.add(new Separator());

    // menu:(label options) enableCompletions or disableCompletions
    String label = CopilotUi.getPlugin().getLanguageServerSettingManager().isAutoShowCompletionEnabled()
        ? Messages.menu_disableCompletions
        : Messages.menu_enableCompletions;
    items.add(createCommandItem("com.microsoft.copilot.eclipse.commands.autoShowCompletions", label,
        UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png")));
    items.add(new Separator());

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

    return items.toArray(new IContributionItem[0]);
  }

  private CommandContributionItem createCommandItem(String commandId, String label, ImageDescriptor icon) {
    CommandContributionItemParameter parameter = new CommandContributionItemParameter(serviceLocator, null, commandId,
        CommandContributionItem.STYLE_PUSH);
    if (icon != null) {
      parameter.icon = icon;
    }

    if (label != null) {
      parameter.label = label;
    }

    return new CommandContributionItem(parameter);
  }
}
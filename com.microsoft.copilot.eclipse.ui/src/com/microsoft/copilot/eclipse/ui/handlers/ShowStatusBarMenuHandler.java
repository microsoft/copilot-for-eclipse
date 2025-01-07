package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.Map;
import java.util.Objects;

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
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.CopilotStatusManager;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for showing GitHub Copilot status bar menu.
 */
public class ShowStatusBarMenuHandler extends CopilotHandler implements IElementUpdater {
  private IHandlerService handlerService;
  private AuthStatusManager authStatusManager;
  private SpinnerJob spinnerJob;

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    handlerService = HandlerUtil.getActiveWorkbenchWindow(event).getService(IHandlerService.class);
    authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();

    MenuManager menuManager = new MenuManager();
    // Sign in status section
    addStatusAction(menuManager);

    // References to the usage of GitHub Copilot section
    // TODO: Uncomment to enable the feedback forum link
    // menuManager.add(new Separator());
    // addLinkToFeedbackForumAction(menuManager);

    // Sign in & sign out section
    menuManager.add(new Separator());
    if (!Objects.equals(authStatusManager.getCopilotStatus(), CopilotStatusResult.LOADING)) {
      addSignInOrSignOutAction(menuManager);
    }

    // Preferences section
    menuManager.add(new Separator());
    addEditKeyboardShortcutsAction(menuManager);
    addPreferencesAction(menuManager);

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
      // Since spinner job has 200ms delay, cancel the spinner job if it is running to avoid flickering.
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
            iconPath = "/icons/github_copilot_signed_in_blue.png";
            break;
          case CopilotStatusResult.LOADING:
            scheduleSpinnerJob(element);
            return;
          case CopilotStatusResult.ERROR, CopilotStatusResult.WARNING:
            iconPath = "/icons/github_copilot_error_blue.png";
            break;
          case CopilotStatusResult.NOT_SIGNED_IN, CopilotStatusResult.NOT_AUTHORIZED:
          default:
            iconPath = "/icons/github_copilot_not_signed_in_blue.png";
        }

        if (iconPath != null) {
          ImageDescriptor newIcon = UiUtils.buildImageDescriptorFromPngPath(iconPath);
          element.setIcon(newIcon);
        }
      }
    }
  }

  private void addStatusAction(MenuManager menuManager) {
    String copilotStatus = getCopilotStatusBasedOnAuthAndCompletionResult(authStatusManager.getCopilotStatus());
    String copilotStatusTitle = Messages.menu_copilotStatus + ": " + copilotStatus;

    MenuActionFactory.createMenuAction(menuManager, copilotStatusTitle, handlerService, copilotStatus, false);
  }

  private void addLinkToFeedbackForumAction(MenuManager menuManager) {
    MenuActionFactory.createMenuAction(menuManager, Messages.menu_viewFeedbackForum, handlerService,
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

  private void addSignInOrSignOutAction(MenuManager menuManager) {
    if (Objects.equals(authStatusManager.getCopilotStatus(), CopilotStatusResult.OK)) {
      ImageDescriptor signOutIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/signout.png");
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_signOutFromGitHub, signOutIcon, handlerService,
          "com.microsoft.copilot.eclipse.commands.signOut", true);
    } else {
      ImageDescriptor signInIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/signin.png");
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_signToGitHub, signInIcon, handlerService,
          "com.microsoft.copilot.eclipse.commands.signIn", true);
    }
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
            CopilotCore.LOGGER.log(LogLevel.ERROR, e);
          }
        }
      };
      action.setEnabled(enabled);
      menuManager.add(action);
    }

    public static void createMenuAction(MenuManager menuManager, String text, IHandlerService handlerService,
        String commandId, boolean enabled) {
      createMenuAction(menuManager, text, null, handlerService, commandId, enabled);
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
        String iconPath = String.format("/icons/spinner/%d.png", currentIconIndex);
        ImageDescriptor newIcon = UiUtils.buildImageDescriptorFromPngPath(iconPath);
        this.uiElement.setIcon(newIcon);
        currentIconIndex = (currentIconIndex % TOTAL_SPINNER_ICONS) + 1;
        if (CopilotUi.getPlugin().getCopilotStatusManager().isCompletionInProgress()) {
          schedule(COMPLETION_IN_PROGRESS_SPINNER_ROTATE_RATE_MILLIS);
        } else {
          cancel();
        }
      } catch (Exception e) {
        CopilotCore.LOGGER.log(LogLevel.ERROR, e);
        return Status.CANCEL_STATUS;
      }
      return Status.OK_STATUS;
    }
  }
}

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AuthStatusResult;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for showing GitHub Copilot status bar menu.
 */
public class ShowStatusBarMenuHandler extends AbstractHandler {

  private IHandlerService handlerService;
  private AuthStatusManager authStatusManager;

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    handlerService = HandlerUtil.getActiveWorkbenchWindow(event).getService(IHandlerService.class);
    authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();

    MenuManager menuManager = new MenuManager();
    addStatusAction(menuManager);
    
    if (!authStatusManager.getAuthStatusResult().isLoading()) {
      menuManager.add(new Separator());
      addSignInOrSignOutAction(menuManager);
    }

    Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
    Menu menu = menuManager.createContextMenu(shell);
    menu.setVisible(true);
    return null;
  }

  private void addStatusAction(MenuManager menuManager) {
    String signInStatus = getSignInStatusBasedOnAuthResult(authStatusManager.getAuthStatusResult());
    String signInStatusTitle = Messages.menu_signInStatus + ": " + signInStatus;

    MenuActionFactory.createMenuAction(menuManager, signInStatusTitle, handlerService, signInStatus, false);
  }

  private String getSignInStatusBasedOnAuthResult(AuthStatusResult authStatusResult) {
    switch (authStatusResult.getStatus()) {
      case AuthStatusResult.OK:
        return Messages.menu_signInStatus_ready;
      case AuthStatusResult.ERROR:
        return Messages.menu_signInStatus_unknownError;
      case AuthStatusResult.LOADING:
        return Messages.menu_signInStatus_loading;
      case AuthStatusResult.NOT_SIGNED_IN:
        return Messages.menu_signInStatus_notSignedInToGitHub;
      case AuthStatusResult.WARNING:
        return Messages.menu_signInStatus_agentWarning;
      case AuthStatusResult.NOT_AUTHORIZED:
        return Messages.menu_signInStatus_notAuthorized;
      default:
        return Messages.menu_signInStatus_loading;
    }
  }

  private void addSignInOrSignOutAction(MenuManager menuManager) {
    if (authStatusManager.getAuthStatusResult().isSignedIn()) {
      ImageDescriptor signInIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/signin.png");
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_signOutFromGitHub, signInIcon, handlerService,
          "com.microsoft.copilot.eclipse.commands.signOut", true);
    } else {
      ImageDescriptor signOutIcon = UiUtils.buildImageDescriptorFromPngPath("/icons/signout.png");
      MenuActionFactory.createMenuAction(menuManager, Messages.menu_signToGitHub, signOutIcon, handlerService,
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
            // TODO: log & send telemetry
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
}

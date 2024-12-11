package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for showing GitHub Copilot status bar menu.
 */
public class ShowStatusBarMenuHandler extends AbstractHandler {

  /**
   * Render the status bar menu based on the logged-in state.
   */
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {

    Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
    MenuManager menuManager = new MenuManager();
    ImageDescriptor icon = UiUtils.resizeIcon("/icons/copilot.png", UiConstants.TOOLBAR_ICON_WIDTH_IN_PIEXL,
        UiConstants.TOOLBAR_ICON_HEIGHT_IN_PIEXL);

    // TODO: Add GitHub sign-in states to the menu
    Action signInAction = new Action("Sign In to GitHub", icon) {
      @Override
      public void run() {
        // Handle sign-in action
      }
    };

    // TODO: Add GitHub sign-out states to the menu
    Action signOutAction = new Action("Sign Out from GitHub", icon) {
      @Override
      public void run() {
        // Handle sign-out action
      }
    };

    menuManager.add(signInAction);
    menuManager.add(signOutAction);

    Menu menu = menuManager.createContextMenu(shell);
    menu.setVisible(true);
    return null;
  }
}

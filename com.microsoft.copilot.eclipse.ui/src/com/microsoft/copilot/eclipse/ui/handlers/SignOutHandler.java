package com.microsoft.copilot.eclipse.ui.handlers;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Handler for signing out from GitHub Copilot.
 */
public class SignOutHandler extends AbstractHandler {

  private AuthStatusManager authStatusManager;

  /**
   * Initialize the Copilot Language Server and Auth Status Manager for the SignOutHandler.
   */
  public SignOutHandler() {
    this.authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = SwtUtils.getShellFromEvent(event);
    try {
      CopilotStatusResult result = authStatusManager.signOut();
      if (!result.isSignedIn()) {
        showSignOutMessage(shell);
        authStatusManager.checkStatus();
      }
    } catch (Exception e) {
      handleSignOutException(shell, e);
    }

    return null;
  }

  private void handleSignOutException(Shell shell, Exception e) {
    String msg = Messages.signOutHandler_msgDialog_signOutFailed;
    if (StringUtils.isNotBlank(e.getMessage())) {
      msg += " " + e.getMessage();
      CopilotCore.LOGGER.log(LogLevel.ERROR, e);
    }
    MessageDialog.openError(shell, Messages.signOutHandler_msgDialog_signOutFailedFailure, msg);
  }

  private void showSignOutMessage(Shell shell) {
    MessageDialog.openInformation(shell, Messages.signOutHandler_msgDialog_githubCopilot,
        Messages.signOutHandler_msgDialog_signOutSuccess);
  }

}

package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.CopilotStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;
import com.microsoft.copilot.eclipse.ui.dialogs.SignInConfirmDialog;
import com.microsoft.copilot.eclipse.ui.dialogs.SignInDialog;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for signing to GitHub Copilot.
 */
public class SignInHandler extends AbstractHandler {

  private static final long SIGNIN_TIMEOUT_MILLIS = 180000L;

  private CopilotStatusManager copilotStatusManager;

  /**
   * Initialize the Copilot Language Server for the SignInHandler.
   */
  public SignInHandler() {
    this.copilotStatusManager = CopilotCore.getPlugin().getCopilotStatusManager();
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = SwtUtils.getShellFromEvent(event);

    try {
      SignInInitiateResult result = initiateSignIn();
      if (result.isAlreadySignedIn()) {
        showAlreadySignedInMessage(shell);
      } else {
        handleSignIn(shell, result);
      }
    } catch (Exception e) {
      handleSignInException(shell, e);
      // TODO log & send telemetry
    }

    return null;
  }

  private SignInInitiateResult initiateSignIn() throws Exception {
    return this.copilotStatusManager.signInInitiate();
  }

  private void showAlreadySignedInMessage(Shell shell) {
    MessageDialog.openInformation(shell, Messages.signInHandler_msgDialog_title,
        Messages.signInHandler_msgDialog_alreadySignedIn);
  }

  private void handleSignIn(Shell shell, SignInInitiateResult result) {
    AtomicReference<SignInInitiateResult> signInInitiateResultHolder = new AtomicReference<>(result);
    SwtUtils.invokeOnDisplayThread(() -> {
      SignInDialog signInDialog = new SignInDialog(shell, signInInitiateResultHolder.get());
      int btnId = signInDialog.open();
      if (btnId > 0) {
        UiUtils.openLink(signInInitiateResultHolder.get().getVerificationUri());
        SignInConfirmDialog signInConfirmDialog = new SignInConfirmDialog(shell,
            signInInitiateResultHolder.get().getUserCode(), SIGNIN_TIMEOUT_MILLIS);
        signInConfirmDialog.run();
        handleSignInConfirmation(shell, signInConfirmDialog);
      }
    });
  }

  private void handleSignInConfirmation(Shell shell, SignInConfirmDialog signInConfirmDialog) {
    IStatus status = signInConfirmDialog.getStatus();
    if (status != null && status.isOK()) {
      showSignInSuccessMessage(shell);
    } else {
      showSignInFailMessage(shell, status);
    }
  }

  private void showSignInSuccessMessage(Shell shell) {
    MessageDialog.openInformation(shell, Messages.signInHandler_msgDialog_gitHubCopilot,
        Messages.signInHandler_msgDialog_signInSuccess);
  }

  private void showSignInFailMessage(Shell shell, IStatus status) {
    String msg = Messages.signInHandler_msgDialog_signInFailed;
    if (status != null && StringUtils.isNotBlank(status.getMessage())) {
      msg += ": " + status.getMessage();
    }
    msg += ". ";
    MessageDialog.openInformation(shell, Messages.signInHandler_msgDialog_gitHubCopilot,
        msg + Messages.signInHandler_msgDialog_signInFailedTryAgain);
  }

  private void handleSignInException(Shell shell, Exception e) {
    String msg = Messages.signInHandler_msgDialog_signInFailed;
    if (StringUtils.isNotBlank(e.getMessage())) {
      msg += " " + e.getMessage();
      // TODO log & send telemetry
    }
    MessageDialog.openError(shell, Messages.signInHandler_msgDialog_signInFailedFailure, msg);
  }
}

package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
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

  private AuthStatusManager authStatusManager;

  /**
   * Initialize the Copilot Language Server for the SignInHandler.
   */
  public SignInHandler() {
    this.authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    SignInJob signInJob = new SignInJob(event);
    signInJob.schedule();

    return null;
  }

  private class SignInJob extends Job {

    private static final long SIGNIN_TIMEOUT_MILLIS = 180000L;

    private final ExecutionEvent event;

    /**
     * Creates a new completion job.
     */
    public SignInJob(ExecutionEvent event) {
      super("Initializing GitHub Copilot sign-in process...");
      this.event = event;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        Shell shell = SwtUtils.getShellFromEvent(event);
        IStatus status = runInitiateSignIn(shell);
        return status;
      } catch (Exception e) {
        String msg = Messages.signInHandler_msgDialog_signInFailed;
        if (StringUtils.isNotBlank(e.getMessage())) {
          msg += " " + e.getMessage();
          CopilotCore.LOGGER.error(msg, e);
        }

        String errorMsg = "Sign in failed: " + e.getMessage();
        return new Status(IStatus.ERROR, Constants.PLUGIN_ID, errorMsg);
      }
    }

    private IStatus runInitiateSignIn(Shell shell)
        throws InterruptedException, java.util.concurrent.ExecutionException {
      SignInInitiateResult result = initiateSignIn();
      if (result.isAlreadySignedIn()) {
        showAlreadySignedInMessage(shell);
        return Status.OK_STATUS;
      } else {
        handleSignIn(shell, result);
        return Status.OK_STATUS;
      }
    }

    private SignInInitiateResult initiateSignIn() throws InterruptedException, java.util.concurrent.ExecutionException {
      return authStatusManager.signInInitiate();
    }

    private void showAlreadySignedInMessage(Shell shell) {
      SwtUtils.invokeOnDisplayThread(() -> {
        MessageDialog.openInformation(shell, Messages.signInHandler_msgDialog_title,
            Messages.signInHandler_msgDialog_alreadySignedIn);
      }, shell);
    }

    private void handleSignIn(Shell shell, SignInInitiateResult result) {
      AtomicReference<SignInInitiateResult> signInInitiateResultHolder = new AtomicReference<>(result);
      SwtUtils.invokeOnDisplayThread(() -> {
        SignInDialog signInDialog = new SignInDialog(shell, signInInitiateResultHolder.get());
        int openResult = signInDialog.open();
        if (openResult > 0) {
          UiUtils.openLink(signInInitiateResultHolder.get().getVerificationUri());
          SignInConfirmDialog signInConfirmDialog = new SignInConfirmDialog(shell,
              signInInitiateResultHolder.get().getUserCode(), SIGNIN_TIMEOUT_MILLIS);
          signInConfirmDialog.run();
          handleSignInConfirmation(shell, signInConfirmDialog);
        }
      }, shell);
    }

    private void handleSignInConfirmation(Shell shell, SignInConfirmDialog signInConfirmDialog) {
      IStatus status = signInConfirmDialog.getStatus();
      if (status != null && status.isOK()) {
        showSignInSuccessMessage(shell);
        authStatusManager.setCopilotStatus(CopilotStatusResult.OK);
      } else {
        showSignInFailMessage(shell, status);
        authStatusManager.setCopilotStatus(CopilotStatusResult.NOT_SIGNED_IN);
      }
    }

    private void showSignInSuccessMessage(Shell shell) {
      MessageDialog.openInformation(shell, Messages.signInHandler_msgDialog_githubCopilot,
          Messages.signInHandler_msgDialog_signInSuccess);
    }

    private void showSignInFailMessage(Shell shell, IStatus status) {
      String msg = Messages.signInHandler_msgDialog_signInFailed;
      if (status != null && StringUtils.isNotBlank(status.getMessage())) {
        msg += ": " + status.getMessage();
      }
      msg += ". ";
      MessageDialog.openInformation(shell, Messages.signInHandler_msgDialog_githubCopilot,
          msg + Messages.signInHandler_msgDialog_signInFailedTryAgain);
    }
  }
}

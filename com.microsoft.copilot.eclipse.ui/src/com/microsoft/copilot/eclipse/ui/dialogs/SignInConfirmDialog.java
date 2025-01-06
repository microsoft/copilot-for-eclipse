package com.microsoft.copilot.eclipse.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Dialog for confirming sign-in to GitHub Copilot.
 */
public class SignInConfirmDialog extends ProgressMonitorDialog {

  private final String userCode;
  private final long timeout;
  private CompletableFuture<CopilotStatusResult> future;
  private IStatus status;

  /**
   * Constructs a new SignInConfirmDialog.
   *
   * @param parent the parent shell
   * @param userCode the user code for sign-in confirmation
   * @param timeout the timeout duration in milliseconds
   */
  public SignInConfirmDialog(Shell parent, String userCode, long timeout) {
    super(parent);
    this.userCode = userCode;
    this.timeout = timeout;
    this.future = null;
    this.setCancelable(true);
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(Messages.signInDialog_title);
  }

  /**
   * Runs the sign-in confirmation process.
   */
  public void run() {
    IRunnableWithProgress task = new SignInConfirmationTask();
    try {
      this.run(true, true, task);
    } catch (Exception e) {
      CopilotCore.LOGGER.log(LogLevel.ERROR, e);
    }
  }

  /**
   * Gets the status of the sign-in confirmation.
   *
   * @return the status of the sign-in confirmation
   */
  public IStatus getStatus() {
    return this.status;
  }

  private class SignInConfirmationTask implements IRunnableWithProgress {
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
      try {
        future = CompletableFuture.supplyAsync(() -> {
          try {
            return CopilotCore.getPlugin().getAuthStatusManager().signInConfirm(userCode);
          } catch (Exception e) {
            CopilotCore.LOGGER.log(LogLevel.ERROR, e);
            return null;
          }
        });

        monitor.beginTask(Messages.signInConfirmDialog_progress, (int) timeout / 250);

        waitForAuthorization(monitor);

        if (future.isDone()) {
          handleAuthorizationResult();
        } else {
          future.cancel(true);
          status = Status.error(Messages.signInConfirmDialog_progressTimeout);
        }
      } catch (ExecutionException | InterruptedException e) {
        status = Status.error(Messages.signInConfirmDialog_progressCanceled);
      }
    }

    private void waitForAuthorization(IProgressMonitor monitor) throws InterruptedException {
      int step = 250;
      int steps = (int) timeout / step;

      for (int i = 0; i < steps; i++) {
        Thread.sleep(step);

        if (monitor.isCanceled()) {
          future.cancel(true);
          return;
        }

        if (future.isDone()) {
          break;
        }

        monitor.worked(1);
      }

      monitor.done();
    }

    private void handleAuthorizationResult() throws ExecutionException, InterruptedException {
      CopilotStatusResult result = future.get();
      String errorMsg = null;

      if (result == null || !result.isSignedIn()) {
        errorMsg = Messages.signInConfirmDialog_authResult_notSignedIn;
      } else if (result.isNotAuthorized()) {
        errorMsg = Messages.signInConfirmDialog_authResult_notAuthed;
      }

      status = errorMsg != null ? Status.error(errorMsg) : Status.OK_STATUS;
    }
  }
}

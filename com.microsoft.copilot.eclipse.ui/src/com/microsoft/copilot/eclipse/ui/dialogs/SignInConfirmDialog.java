package com.microsoft.copilot.eclipse.ui.dialogs;

import static com.microsoft.copilot.eclipse.core.AuthStatusManager.SIGNIN_TIMEOUT_MILLIS;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Dialog for confirming sign-in to GitHub Copilot.
 */
public class SignInConfirmDialog extends ProgressMonitorDialog {

  private final String userCode;
  private CompletableFuture<CopilotStatusResult> future;
  private IStatus status;

  /**
   * Constructs a new SignInConfirmDialog.
   *
   * @param parent the parent shell
   * @param userCode the user code for sign-in confirmation
   */
  public SignInConfirmDialog(Shell parent, String userCode) {
    super(parent);
    this.userCode = userCode;
    this.future = null;
    this.setCancelable(true);
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(Messages.signInDialog_title);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    super.createDialogArea(parent);
    GridData gdSubTaskLabel = new GridData(GridData.FILL_HORIZONTAL);
    gdSubTaskLabel.heightHint = 1;
    subTaskLabel.setLayoutData(gdSubTaskLabel);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    Label label = new Label(parent, SWT.NONE);
    label.setLayoutData(gd);
    label.setText(Messages.signInConfirmDialog_deviceCodeFormatString.formatted(userCode));
    parent.layout();
    return parent;
  }

  /**
   * Runs the sign-in confirmation process.
   */
  public void run() {
    IRunnableWithProgress task = new SignInConfirmationTask();
    try {
      this.run(true, true, task);
    } catch (Exception e) {
      CopilotCore.LOGGER.error(e);
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
    private static final int STEP_INTERVAL = 250;

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
      try {
        future = CompletableFuture.supplyAsync(() -> {
          try {
            return CopilotCore.getPlugin().getAuthStatusManager().signInConfirm(userCode);
          } catch (Exception e) {
            CopilotCore.LOGGER.error(e);
            return null;
          }
        });

        monitor.beginTask(Messages.signInConfirmDialog_progress, (int) SIGNIN_TIMEOUT_MILLIS / STEP_INTERVAL);

        waitForAuthorization(monitor);

        if (future.isDone()) {
          handleAuthorizationResult();
        } else {
          future.cancel(true);
          status = Status.error(Messages.signInConfirmDialog_progressTimeout);
        }
      } catch (ExecutionException | InterruptedException | CancellationException e) {
        status = Status.error(Messages.signInConfirmDialog_progressCanceled);
      }
    }

    private void waitForAuthorization(IProgressMonitor monitor) throws InterruptedException {
      int steps = (int) SIGNIN_TIMEOUT_MILLIS / STEP_INTERVAL;

      for (int i = 0; i < steps; i++) {
        Thread.sleep(STEP_INTERVAL);

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

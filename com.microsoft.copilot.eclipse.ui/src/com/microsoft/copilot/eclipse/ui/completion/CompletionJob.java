package com.microsoft.copilot.eclipse.ui.completion;

import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;

/**
 * Job to trigger an inline completion.
 */
public class CompletionJob extends Job {

  private CompletionResult result;

  /**
   * Creates a new completion job.
   */
  public CompletionJob(CopilotLanguageServerConnection lsConnection) {
    super("Generating completion...");
    this.lsConnection = lsConnection;
    this.setUser(true);
  }

  private CopilotLanguageServerConnection lsConnection;
  private CompletionParams params;

  public void setCompletionParams(CompletionParams params) {
    this.params = params;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    if (params == null) {
      return new Status(IStatus.ERROR, Constants.PLUGIN_ID, "Invalid completion parameters");
    }
    if (monitor.isCanceled()) {
      System.out.println("Completion job is canceled");
      return Status.CANCEL_STATUS;
    }
    try {
      this.result = this.lsConnection.getCompletions(params).get();
    } catch (InterruptedException e) {
      return Status.CANCEL_STATUS;
    } catch (ExecutionException e) {
      // TODO: log & send telemetry
      return new Status(IStatus.ERROR, Constants.PLUGIN_ID, e.getMessage(), e);
    }
    if (monitor.isCanceled()) {
      System.out.println("Completion job is canceled");
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  public CompletionResult getCompletionResult() {
    return result;
  }

}

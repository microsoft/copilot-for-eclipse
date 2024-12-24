package com.microsoft.copilot.eclipse.core.completion;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;

/**
 * Job to trigger an inline completion.
 */
public class CompletionJob extends Job {

  /**
   * The job family for completion jobs, can be used to find out this completion job.
   */
  public static final String COMPLETION_JOB_FAMILY = "com.microsoft.copilot.eclipse.completionJobFamily";

  private static final int COMPLETION_TIMEOUT_MILLIS = 5000;

  private CompletionResult result;

  private CopilotLanguageServerConnection lsConnection;
  private CompletionParams params;

  /**
   * Creates a new completion job.
   */
  public CompletionJob(CopilotLanguageServerConnection lsConnection) {
    super("Generating completion...");
    this.lsConnection = lsConnection;
    this.setSystem(true);
    this.setPriority(Job.INTERACTIVE);
  }

  public void setCompletionParams(CompletionParams params) {
    this.params = params;
  }

  public CompletionParams getCompletionParams() {
    return params;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    if (params == null) {
      return new Status(IStatus.ERROR, Constants.PLUGIN_ID, "Invalid completion parameters");
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    try {
      this.result = this.lsConnection.getCompletions(params).get(COMPLETION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      return Status.CANCEL_STATUS;
    } catch (ExecutionException e) {
      CopilotCore.LOGGER.log(LogLevel.ERROR, e);
      return new Status(IStatus.ERROR, Constants.PLUGIN_ID, e.getMessage(), e);
    } catch (TimeoutException e) {
      CopilotCore.LOGGER.log(LogLevel.WARNING,
          "Completion request timed out after " + COMPLETION_TIMEOUT_MILLIS + " milliseconds");
      return Status.CANCEL_STATUS;
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  public CompletionResult getCompletionResult() {
    return result;
  }

  public String getUriString() {
    return params.getDoc().getUri();
  }

  @Override
  public boolean belongsTo(Object family) {
    return Objects.equals(family, COMPLETION_JOB_FAMILY);
  }

}

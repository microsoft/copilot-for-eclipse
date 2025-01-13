package com.microsoft.copilot.eclipse.core.completion;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4j.Position;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionDocument;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

/**
 * Provider for inline completion.
 */
public class CompletionProvider {

  /**
   * The job family for completion jobs, can be used to find out this completion job.
   */
  public static final String COMPLETION_JOB_FAMILY = "com.microsoft.copilot.eclipse.completionJobFamily";

  private CompletionJob completionJob;
  private Set<CompletionListener> completionListeners;
  private Set<CompletionStatusListener> completionStatusListeners;
  private AuthStatusManager statusManager;

  /**
   * Creates a new completion provider.
   */
  public CompletionProvider(CopilotLanguageServerConnection lsConnection, AuthStatusManager statusManager) {
    this.statusManager = statusManager;
    this.completionJob = new CompletionJob(lsConnection);
    this.completionListeners = new LinkedHashSet<>();
    this.completionStatusListeners = new LinkedHashSet<>();
  }

  /**
   * Trigger an inline completion.
   *
   * @param uriString the URI string of the document.
   * @param position the position of the cursor.
   * @param documentVersion the version of the document.
   */
  public void triggerCompletion(String uriString, Position position, int documentVersion) {
    if (!Objects.equals(statusManager.getCopilotStatus(), CopilotStatusResult.OK)) {
      return;
    }
    this.completionJob.cancel();
    CompletionDocument completionDoc = new CompletionDocument(uriString, position);
    completionDoc.setVersion(documentVersion);
    // following format options are hard-coded, because eclipse support applying the format options
    // automatically when drawing text into the editor, so don't need to set the actual values here.
    completionDoc.setInsertSpaces(true);
    completionDoc.setTabSize(4);
    CompletionParams params = new CompletionParams(completionDoc);

    this.completionJob.setCompletionParams(params);
    this.completionJob.schedule();
  }

  /**
   * Add a completion listener.
   */
  public void addCompletionListener(CompletionListener listener) {
    this.completionListeners.add(listener);
  }

  /**
   * Register a completion status listener.
   */
  public void addCompletionStatusListener(CompletionStatusListener listener) {
    this.completionStatusListeners.add(listener);
  }

  /**
   * Remove a completion listener.
   */
  public void removeCompletionListener(CompletionListener listener) {
    this.completionListeners.remove(listener);
  }

  /**
   * Unregister a completion status listener.
   */
  public void removeCompletionStatusListener(CompletionStatusListener listener) {
    listener.onCompletionDone();
    this.completionStatusListeners.remove(listener);
  }

  /**
   * TODO: public for testing.
   */
  public class CompletionJob extends Job {

    private static final int COMPLETION_TIMEOUT_MILLIS = 5000;

    private CopilotLanguageServerConnection lsConnection;
    private CompletionParams params;
    private List<CompletionItem> completions;

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

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      notifyCompletionAboutToRun();
      this.completions = null;
      try {
        IStatus status = runCompletion(monitor);
        if (status.isOK() && this.completions != null) {
          notifyCompletionResolved();
        }
        return status;
      } finally {
        notifyCompletionDone();
      }
    }

    private IStatus runCompletion(IProgressMonitor monitor) {
      if (params == null) {
        CopilotCore.LOGGER.log(LogLevel.ERROR, "Invalid completion parameters");
        return new Status(IStatus.ERROR, Constants.PLUGIN_ID, "Invalid completion parameters");
      }
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      try {
        CompletionResult result = this.lsConnection.getCompletions(params).get(COMPLETION_TIMEOUT_MILLIS,
            TimeUnit.MILLISECONDS);
        if (result == null || result.getCompletions() == null || result.getCompletions().isEmpty()) {
          return Status.OK_STATUS;
        }

        this.completions = result.getCompletions();
      } catch (InterruptedException e) {
        return Status.CANCEL_STATUS;
      } catch (ExecutionException e) {
        statusManager.setCopilotStatus(CopilotStatusResult.ERROR);
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

    @Override
    public boolean belongsTo(Object family) {
      return Objects.equals(family, COMPLETION_JOB_FAMILY);
    }

    private void notifyCompletionAboutToRun() {
      for (CompletionStatusListener listener : CompletionProvider.this.completionStatusListeners) {
        listener.onCompletionAboutToRun();
      }
    }

    private void notifyCompletionDone() {
      for (CompletionStatusListener listener : CompletionProvider.this.completionStatusListeners) {
        listener.onCompletionDone();
      }
    }

    private void notifyCompletionResolved() {
      for (CompletionListener listener : CompletionProvider.this.completionListeners) {
        // TODO: notify the listener according to the listen uri?
        listener.onCompletionResolved(this.params.getDoc().getUri(), this.completions);
      }
    }
  }
}

package com.microsoft.copilot.eclipse.core.completion;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.lsp4j.Position;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.CopilotStatusManager;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionDocument;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

/**
 * Provider for inline completion.
 */
public class CompletionProvider implements IJobChangeListener {

  private CompletionJob completionJob;
  private Set<CompletionListener> completionListeners;
  private CopilotStatusManager statusManager;

  /**
   * Creates a new completion provider.
   */
  public CompletionProvider(CopilotLanguageServerConnection lsConnection, CopilotStatusManager statusManager) {
    this.statusManager = statusManager;
    this.completionJob = new CompletionJob(lsConnection);
    this.completionJob.addJobChangeListener(this);
    this.completionListeners = new LinkedHashSet<>();
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
    this.completionJob.setCompletionParams(null);
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
   * Remove a completion listener.
   */
  public void removeCompletionListener(CompletionListener listener) {
    this.completionListeners.remove(listener);
  }

  @Override
  public void aboutToRun(IJobChangeEvent event) {
    // do nothing

  }

  @Override
  public void awake(IJobChangeEvent event) {
    // do nothing

  }

  @Override
  public void done(IJobChangeEvent event) {
    IStatus jobStatus = this.completionJob.getResult();
    if (jobStatus != null && !jobStatus.isOK()) {
      return;
    }
    CompletionResult result = this.completionJob.getCompletionResult();
    if (result == null || result.getCompletions() == null || result.getCompletions().isEmpty()) {
      return;
    }

    CompletionParams params = this.completionJob.getCompletionParams();
    if (params == null) {
      return;
    }

    CompletionCollection completions = new CompletionCollection(result.getCompletions(), params.getDoc().getUri());
    for (CompletionListener listener : this.completionListeners) {
      listener.onCompletionResolved(completions);
    }
  }

  @Override
  public void running(IJobChangeEvent event) {
    // do nothing

  }

  @Override
  public void scheduled(IJobChangeEvent event) {
    // do nothing

  }

  @Override
  public void sleeping(IJobChangeEvent event) {
    // do nothing

  }

}

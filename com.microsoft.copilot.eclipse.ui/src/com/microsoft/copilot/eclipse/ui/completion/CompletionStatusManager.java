package com.microsoft.copilot.eclipse.ui.completion;

import com.microsoft.copilot.eclipse.core.completion.CompletionStatusListener;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Listener for tracking copilot completion status.
 */
public class CompletionStatusManager implements CompletionStatusListener {

  private boolean completionInProgress;

  /**
   * Constructor for the CompletionStatusManager.
   */
  public CompletionStatusManager() {
  }

  @Override
  public void onCompletionAboutToRun() {
    this.completionInProgress = true;
    UiUtils.refreshCopilotMenu();
  }

  @Override
  public void onCompletionDone() {
    this.completionInProgress = false;
    UiUtils.refreshCopilotMenu();
  }

  public boolean isCompletionInProgress() {
    return completionInProgress;
  }
}
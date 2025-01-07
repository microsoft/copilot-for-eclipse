package com.microsoft.copilot.eclipse.ui;

import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.completion.CompletionStatusListener;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Listener for tracking copilot completion status.
 */
public class CopilotStatusManager implements CompletionStatusListener, CopilotAuthStatusListener {

  private boolean completionInProgress;

  /**
   * Constructor for the CopilotStatusManager.
   */
  public CopilotStatusManager() {
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

  @Override
  public void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult) {
    UiUtils.refreshCopilotMenu();
  }
}
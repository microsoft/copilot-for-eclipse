package com.microsoft.copilot.eclipse.ui;

import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Listener for tracking copilot completion status.
 */
public class CopilotStatusManager implements CopilotAuthStatusListener {
  /**
   * Constructor for the CopilotStatusManager.
   */
  public CopilotStatusManager() {
  }

  @Override
  public void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult) {
    UiUtils.refreshCopilotMenu();
  }
}
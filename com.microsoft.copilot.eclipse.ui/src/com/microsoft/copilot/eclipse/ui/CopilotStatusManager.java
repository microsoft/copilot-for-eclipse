package com.microsoft.copilot.eclipse.ui;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Manager dealing with the copilot status in the Eclipse status bar.
 */
public class CopilotStatusManager {

  private IEventBroker eventBroker;

  /**
   * Constructor for the CopilotStatusManager.
   */
  public CopilotStatusManager() {
    eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      eventBroker.subscribe(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, event -> {
        onDidCopilotStatusChange();
      });
    }
  }

  private void onDidCopilotStatusChange() {
    UiUtils.refreshCopilotMenu();
  }
}
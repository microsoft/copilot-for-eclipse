package com.microsoft.copilot.eclipse.ui.notifications;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.QuotaWarningNotification;

/**
 * Handles quota notifications from the language server and displays
 * them as JFace notification popups.
 */
public class QuotaNotificationManager {

  private static final int NOTIFICATION_DELAY_MS = 10000;

  private final IEventBroker eventBroker;
  private EventHandler quotaWarningEventHandler;

  /**
   * Creates a new QuotaNotificationManager and subscribes to quota events.
   */
  public QuotaNotificationManager() {
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      this.quotaWarningEventHandler = event -> {
        Object data = event.getProperty(IEventBroker.DATA);
        if (data instanceof QuotaWarningNotification notification) {
          showQuotaWarningNotification(notification);
        }
      };
      eventBroker.subscribe(CopilotEventConstants.TOPIC_QUOTA_WARNING, quotaWarningEventHandler);
    }
  }

  /**
   * Unsubscribes the quota event handler.
   */
  public void dispose() {
    if (eventBroker != null && quotaWarningEventHandler != null) {
      eventBroker.unsubscribe(quotaWarningEventHandler);
      quotaWarningEventHandler = null;
    }
  }

  private void showQuotaWarningNotification(QuotaWarningNotification notification) {
    Display display = PlatformUI.getWorkbench().getDisplay();
    if (display == null || display.isDisposed()) {
      return;
    }
    display.asyncExec(() -> {
      try {
        new QuotaWarningNotificationPopup(display, notification.message(), NOTIFICATION_DELAY_MS).open();
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to show quota warning notification", e);
      }
    });
  }
}

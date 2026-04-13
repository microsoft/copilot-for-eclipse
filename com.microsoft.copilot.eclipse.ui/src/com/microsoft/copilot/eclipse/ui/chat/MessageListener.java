package com.microsoft.copilot.eclipse.ui.chat;

/**
 * Listener for sending and canceling messages.
 */
public interface MessageListener {
  /**
   * Called when a message is cancelled.
   */
  public void onCancel();
}

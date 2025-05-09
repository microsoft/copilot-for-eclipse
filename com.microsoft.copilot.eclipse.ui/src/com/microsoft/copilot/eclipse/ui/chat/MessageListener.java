package com.microsoft.copilot.eclipse.ui.chat;

/**
 * Listener for sending and canceling messages.
 */
public interface MessageListener {
  /**
   * Called when a message is sent.
   *
   * @param workDoneToken the work done token
   * @param message the message
   */
  public void onSend(String workDoneToken, String message);

  /**
   * Called when a message is cancelled.
   */
  public void onCancel();
}

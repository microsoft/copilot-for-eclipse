package com.microsoft.copilot.eclipse.ui.chat;

import java.util.List;

import org.eclipse.core.resources.IFile;

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
  public void onSend(String workDoneToken, String message, List<IFile> files);

  /**
   * Called when a message is cancelled.
   */
  public void onCancel();
}

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.List;

import org.eclipse.core.resources.IFile;

/**
 * Listener for sending messages.
 */
public interface SendMessageListener {
  /**
   * Called when a message is sent.
   *
   * @param workDoneToken the work done token
   * @param message the message
   */
  public void onSendMessage(String workDoneToken, String message, List<IFile> files);
}

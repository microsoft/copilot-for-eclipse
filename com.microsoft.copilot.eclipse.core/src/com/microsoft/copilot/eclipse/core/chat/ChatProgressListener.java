package com.microsoft.copilot.eclipse.core.chat;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;

/**
 * Listener for chat resolution.
 */
public interface ChatProgressListener {
  /**
   * Notifies to the listeners when the chat is resolved.
   */
  public void onChatProgress(ChatProgressValue progress);

}

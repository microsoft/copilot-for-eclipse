package com.microsoft.copilot.eclipse.core.chat;

import java.util.LinkedHashSet;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;

/**
 * Provider for chat progress.
 */
public class ChatProvider {

  /**
   * List of chat progress listeners.
   */
  public LinkedHashSet<ChatProgressListener> chatProgressListeners;

  /**
   * Creates a new chat progress provider.
   */
  public ChatProvider() {
    this.chatProgressListeners = new LinkedHashSet<>();
  }

  /**
   * Add a listener to the chat progress provider.
   */
  public void addChatProgressListener(ChatProgressListener listener) {
    this.chatProgressListeners.add(listener);
  }

  /**
   * Remove a listener from the chat progress provider.
   */
  public void removeChatProgressListener(ChatProgressListener listener) {
    this.chatProgressListeners.remove(listener);
  }

  /**
   * Notify the progress to the listeners.
   */
  public void notifyProgress(ChatProgressValue message) {
    for (ChatProgressListener listener : this.chatProgressListeners) {
      listener.onChatProgress(message);
    }
  }
}

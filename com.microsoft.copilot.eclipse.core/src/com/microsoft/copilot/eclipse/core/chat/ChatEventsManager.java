// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.LinkedHashSet;
import java.util.concurrent.CompletableFuture;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;

/**
 * Provider for chat progress.
 */
public class ChatEventsManager {

  /**
   * List of chat progress listeners.
   */
  public LinkedHashSet<ChatProgressListener> chatProgressListeners;

  /**
   * List of agent tool listeners.
   */
  public ToolInvocationListener agentToolListener;

  /**
   * Creates a new chat progress provider.
   */
  public ChatEventsManager() {
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

  /**
   * Add a listener to the agent tool manager.
   *
   * @param listener the listener to add
   */
  public void registerAgentToolListener(ToolInvocationListener listener) {
    this.agentToolListener = listener;
  }

  /**
   * Remove a listener from the agent tool manager.
   *
   * @param listener the listener to remove
   */
  public void unregisterAgentToolListener(ToolInvocationListener listener) {
    this.agentToolListener = null;
  }

  /**
   * Notify the listeners when the agent tool should be confirmed.
   *
   * @param params the parameters for the tool confirmation
   */
  public CompletableFuture<LanguageModelToolConfirmationResult> confirmAgentToolInvocation(
      InvokeClientToolConfirmationParams params) {
    if (this.agentToolListener == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("No agent tool listener registered"));
    }
    return this.agentToolListener.onToolConfirmation(params);
  }

  /**
   * Notify the listeners when the agent tool should be invoked.
   *
   * @param params the parameters for the tool invocation
   */
  public CompletableFuture<LanguageModelToolResult[]> invokeAgentTool(InvokeClientToolParams params) {
    if (this.agentToolListener == null) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("No agent tool listener registered"));
    }
    return this.agentToolListener.onToolInvocation(params);
  }
}

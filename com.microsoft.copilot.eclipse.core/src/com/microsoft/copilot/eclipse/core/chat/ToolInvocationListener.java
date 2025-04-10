package com.microsoft.copilot.eclipse.core.chat;

import java.util.concurrent.CompletableFuture;

import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;

/**
 * Listener for tool invocation.
 */
public interface ToolInvocationListener {
  /**
   * Notifies to the listeners when a tool is invoked.
   *
   * @param params The parameters for invoking the tool.
   * @return A CompletableFuture that will be completed with the result of the tool invocation.
   */
  public CompletableFuture<LanguageModelToolResult[]> onToolInvocation(InvokeClientToolParams params);
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ConfirmationMessages;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Base class for tools.
 */
public abstract class BaseTool {
  protected String name;

  /**
   * Invoke the tool.
   *
   * @param input the input arguments for the tool invocation.
   * @param chatView the chat view requesting the tool invocation.
   * @return a future completed with the language model tool results.
   */
  public abstract CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView);

  /**
   * Get the registration information of the tool.
   *
   * @return the language model tool registration information.
   */
  public LanguageModelToolInformation getToolInformation() {
    LanguageModelToolInformation toolInfo = new LanguageModelToolInformation();
    if (needConfirmation()) {
      toolInfo.setConfirmationMessages(getConfirmationMessages());
    }
    return toolInfo;
  }

  /**
   * Needs user's confirmation to continue.
   *
   * @return {@code true} if the tool requires user confirmation; {@code false} otherwise.
   */
  public boolean needConfirmation() {
    return false;
  }

  /**
   * Get confirmed messages.
   *
   * @return the confirmation messages for this tool.
   */
  public ConfirmationMessages getConfirmationMessages() {
    return new ConfirmationMessages();
  }

  /**
   * Get the user input.
   *
   * @return the user input for this tool, or {@code null} if none is available.
   */
  @Nullable
  public Map<String, Object> getInput() {
    return null;
  }

  /**
   * Get the name of the tool.
   *
   * @return the tool name.
   */
  public String getToolName() {
    return name;
  }
}
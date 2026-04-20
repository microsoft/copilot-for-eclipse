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
   */
  public abstract CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView);

  /**
   * Get the registration information of the tool.
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
   */
  public boolean needConfirmation() {
    return false;
  }

  /**
   * Get confirmed messages.
   */
  public ConfirmationMessages getConfirmationMessages() {
    return new ConfirmationMessages();
  }

  /**
   * Get the user input.
   */
  @Nullable
  public Map<String, Object> getInput() {
    return null;
  }

  /**
   * Get the name of the tool.
   */
  public String getToolName() {
    return name;
  }
}
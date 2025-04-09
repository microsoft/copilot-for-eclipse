package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;

import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Base class for tools.
 */
public abstract class BaseTool {
  protected String name;

  /**
   * Invoke the tool.
   */
  public abstract CompletableFuture<String> invoke(InputSchema inputSchema, ChatView chatView);

  /**
   * Needs user's confirmation to continue.
   */
  public boolean needConfirmation() {
    return false;
  }

  /**
   * Get confirmed message.
   */
  public String getConfirmedMessage() {
    return "";
  }

  /**
   * Get the user input.
   */
  @Nullable
  public InputSchema getInput() {
    return null;
  }

  /**
   * Get the name of the tool.
   */
  public String getToolName() {
    return name;
  }
}
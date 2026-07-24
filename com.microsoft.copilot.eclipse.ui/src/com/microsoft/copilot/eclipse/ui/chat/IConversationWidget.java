// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent.CodingAgentMessageRequestParams;
import com.microsoft.copilot.eclipse.core.persistence.AbstractTurnData;
import com.microsoft.copilot.eclipse.core.persistence.ConversationDataFactory;

/**
 * Abstraction for the conversation content area in the chat view. Implementations render chat turns
 * either via ({@link StyledTextConversationWidget}) using {@link StyledText}
 * or via ({@link BrowserConversationWidget}) using an Eclipse-internal web browser rendering HTML code.
 */
public interface IConversationWidget {

  /**
   * Returns the underlying SWT control for layout purposes.
   */
  Control getControl();

  /**
   * Returns whether this widget has been disposed.
   */
  boolean isDisposed();

  /**
   * Disposes this widget and releases all resources.
   */
  void dispose();

  /**
   * Requests a layout pass on the underlying control.
   */
  void requestLayout();

  /**
   * Sets the conversation ID for this widget.
   */
  void setConversationId(String conversationId);

  /**
   * Begins a new user / copilot turn for the given turn ID.
   *
   * @param turnId the unique ID of the turn
   * @param isCopilot true if this is a Copilot turn, false for user turns
   * @param isHistory true if the turn is being restored from history
   */
  void beginTurn(String turnId, boolean isCopilot, boolean isHistory);

  /**
   * Processes a chat progress event (report or end phase). The widget updates the turn content
   * accordingly.
   */
  void processTurnEvent(ChatProgressValue value);

  /**
   * Creates a new user turn entry in the conversation.
   *
   * @param turnId the turn ID
   * @param message the user's message text
   */
  void startNewUserTurn(String turnId, String message);

  /**
   * Scrolls the conversation content to the bottom.
   */
  void scrollToBottom();

  /**
   * Refreshes the internal scroller layout (e.g., after content changes).
   */
  void refreshScrollerLayout();

  /**
   * Renders an error message in the conversation area.
   */
  void renderErrorMessage(String content);

  /**
   * Shows a "compacting" status indicator on the latest Copilot turn.
   */
  void showCompactingStatusOnLatestCopilotTurn();

  /**
   * Hides the "compacting" status indicator on the latest Copilot turn.
   */
  void hideCompactingStatusOnLatestCopilotTurn();

  /**
   * Returns the active thinking block ID for the given turn, or null if none.
   */
  String getActiveThinkingBlockId(String turnId);

  /**
   * Restores a single turn from persisted conversation data. Implementations render user turns,
   * copilot turns (with thinking blocks, tool calls, errors, agent messages), and model info
   * footers.
   *
   * @param turn the turn data to restore (either {@code UserTurnData} or {@code CopilotTurnData})
   * @param dataFactory factory for converting persisted data to runtime objects
   */
  void restoreTurn(AbstractTurnData turn, ConversationDataFactory dataFactory);

  /**
   * Renders model info footer below a copilot turn (model name, billing multiplier, reasoning
   * effort).
   *
   * @param turnId the turn ID to attach the footer to
   * @param modelName the model name to display
   * @param billingMultiplier the billing multiplier (0 means not shown)
   * @param reasoningEffort the reasoning effort level (may be null)
   */
  void renderModelInfo(String turnId, String modelName, double billingMultiplier,
      String reasoningEffort);

  /**
   * Renders a coding agent message (e.g., PR link) in the specified turn.
   *
   * @param params the agent message parameters containing turn ID, title, description, and link
   */
  void renderAgentMessage(CodingAgentMessageRequestParams params);

  /**
   * Requests tool execution confirmation from the user. Implementations render a confirmation UI
   * (inline HTML in browser view, or SWT dialog in SWT view) and return a future that completes
   * when the user accepts or dismisses.
   *
   * @param turnId the turn ID where the confirmation should appear
   * @param content confirmation content with title, message, and action buttons
   * @param input tool input (may contain "command", "explanation", "action" keys)
   * @return future that completes with the user's confirmation result
   */
  CompletableFuture<LanguageModelToolConfirmationResult> requestToolConfirmation(
      String turnId, ConfirmationContent content, Object input);

  /**
   * Cancels any pending tool confirmation for the given turn. Completes the pending future with
   * DISMISS and removes the confirmation UI.
   *
   * @param turnId the turn ID whose confirmation should be cancelled
   */
  void cancelToolConfirmation(String turnId);

  /**
   * Returns the selected {@link ConfirmationAction} from the last completed confirmation, or null
   * if the user dismissed or no confirmation was shown. Used by the caller to cache decisions.
   */
  ConfirmationAction getLastSelectedConfirmationAction();
}

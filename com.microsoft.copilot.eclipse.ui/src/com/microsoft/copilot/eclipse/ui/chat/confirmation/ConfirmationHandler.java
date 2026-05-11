// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;

/**
 * Evaluates whether a tool confirmation request can be auto-approved.
 * Each implementation handles a specific category of tool (terminal, file operations, MCP, etc.).
 */
public interface ConfirmationHandler {

  /**
   * Evaluates whether the given confirmation request should be auto-approved.
   * Implementations should check both global rules and session memory.
   *
   * @param params the confirmation request parameters from CLS
   * @param sessionConversationId the conversation ID to use for session-scoped
   *     lookups (may differ from params.getConversationId() when called from a
   *     subagent context)
   * @return the confirmation result
   */
  ConfirmationResult evaluate(InvokeClientToolConfirmationParams params,
      String sessionConversationId);

  /**
   * Persists a user's decision based on the action scope.
   * SESSION actions are stored in-memory per conversation;
   * GLOBAL actions are written to preferences.
   *
   * @param action the user's selected action with metadata
   * @param params the original confirmation params (for command data etc.)
   * @param sessionConversationId the conversation ID to use for session storage
   */
  default void persistDecision(ConfirmationAction action,
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    // no-op by default
  }

  /** Clears session-scoped approvals for the given conversation. */
  default void clearSession(String conversationId) {
    // no-op by default
  }
}

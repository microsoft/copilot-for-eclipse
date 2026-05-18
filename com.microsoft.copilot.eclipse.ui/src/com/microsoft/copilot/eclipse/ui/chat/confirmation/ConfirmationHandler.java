// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import java.util.Locale;
import java.util.Map;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;

/**
 * Evaluates whether a tool confirmation request can be auto-approved.
 * Each implementation handles a specific category of tool (terminal, file operations, MCP, etc.).
 */
public interface ConfirmationHandler {

  /** Maximum number of conversations tracked in session memory. */
  int MAX_SESSION_CONVERSATIONS = 50;

  /**
   * Normalizes a file path for case-insensitive, separator-agnostic comparison.
   * Converts backslashes to forward slashes and lowercases the result.
   *
   * @param path the file path to normalize
   * @return the normalized path
   */
  static String normalizePath(String path) {
    return path.replace('\\', '/').toLowerCase(Locale.ROOT);
  }

  /**
   * Extracts the {@code toolType} string from the input map of a confirmation request.
   * Returns {@code null} if the field is absent or not a string.
   *
   * @param params the confirmation request parameters from CLS
   * @return the toolType value, or {@code null}
   */
  static String extractToolType(InvokeClientToolConfirmationParams params) {
    Object input = params.getInput();
    if (input instanceof Map<?, ?> inputMap) {
      Object toolType = inputMap.get("toolType");
      if (toolType instanceof String) {
        return (String) toolType;
      }
    }
    return null;
  }

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
   * Caches a user's decision based on the action scope.
   * SESSION actions are stored in-memory per conversation;
   * GLOBAL actions are written to preferences.
   *
   * @param action the user's selected action with metadata
   * @param params the original confirmation params (for command data etc.)
   * @param sessionConversationId the conversation ID to use for session storage
   */
  default void cacheDecision(ConfirmationAction action,
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    // no-op by default
  }

  /** Clears session-scoped approvals for the given conversation. */
  default void clearSession(String conversationId) {
    // no-op by default
  }
}

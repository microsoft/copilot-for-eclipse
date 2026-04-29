// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;

/**
 * Evaluates whether a tool confirmation request can be auto-approved.
 * Each implementation handles a specific category of tool (terminal, file operations, MCP, etc.).
 */
public interface ConfirmationHandler {

  /**
   * Evaluates whether the given confirmation request should be auto-approved.
   *
   * @param params the confirmation request parameters from CLS
   * @return ConfirmationResult.AUTO_APPROVED if the tool call can proceed without user
   *     confirmation, or ConfirmationResult.NEEDS_CONFIRMATION if the user must approve
   */
  ConfirmationResult evaluate(InvokeClientToolConfirmationParams params);
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;

/**
 * Central entry point for auto-approve evaluation. Decides whether a tool confirmation
 * request should be auto-approved or shown to the user. Currently a no-op skeleton that
 * always requires confirmation; concrete logic will be added in subsequent sub-tasks.
 */
public class ConfirmationService {

  /**
   * Evaluates whether a tool confirmation request should be auto-approved.
   *
   * @param params the confirmation request parameters from CLS
   * @return the confirmation result (currently always {@link ConfirmationResult#NEEDS_CONFIRMATION})
   */
  public ConfirmationResult evaluate(InvokeClientToolConfirmationParams params) {
    return ConfirmationResult.NEEDS_CONFIRMATION;
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

/**
 * Result of evaluating an auto-approve confirmation request.
 */
public enum ConfirmationResult {
  /**
   * The tool invocation is automatically approved and no user confirmation is needed.
   */
  AUTO_APPROVED,

  /**
   * The tool invocation requires user confirmation before proceeding.
   */
  NEEDS_CONFIRMATION
}

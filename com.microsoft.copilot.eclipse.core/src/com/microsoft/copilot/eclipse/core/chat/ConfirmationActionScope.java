// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

/**
 * Scope of how a confirmation decision should be persisted.
 */
public enum ConfirmationActionScope {
  /** One-time acceptance for this single invocation. */
  ONCE,
  /** Remember for the current conversation session (in-memory). */
  SESSION,
  /** Remember globally (application-level persistent, synced to CLS). */
  GLOBAL
}

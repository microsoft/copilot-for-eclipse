// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

/**
 * Listener for the authentication status.
 */
public interface CopilotAuthStatusListener {

  /**
   * Notifies to the listeners when the authentication status is changed.
   */
  void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult);
}

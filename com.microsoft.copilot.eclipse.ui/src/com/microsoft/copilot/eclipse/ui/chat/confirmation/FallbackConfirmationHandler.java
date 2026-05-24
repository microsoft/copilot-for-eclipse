// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import java.util.List;

import org.eclipse.osgi.util.NLS;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.ui.chat.Messages;

/**
 * Catch-all handler for tool types that have no dedicated handler registered.
 * Always shows a simple confirmation dialog with Allow Once / Skip.
 */
public class FallbackConfirmationHandler implements ConfirmationHandler {

  @Override
  public ConfirmationResult evaluate(
      InvokeClientToolConfirmationParams params,
      String sessionConversationId, boolean isAutoApprovalEnabled) {
    String title = params.getTitle() != null
        ? params.getTitle()
        : NLS.bind(Messages.confirmation_title_fallback, params.getName());
    return ConfirmationResult.needsConfirmation(
        new ConfirmationContent(title, params.getMessage(),
            List.of(
                ConfirmationAction.allowOnce(
                    Messages.confirmation_action_allowOnce),
                ConfirmationAction.skip(
                    Messages.confirmation_action_skip))));
  }
}

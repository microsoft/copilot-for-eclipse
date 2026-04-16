// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.completion.SuggestionUpdateManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyRejectedParams;
import com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager;
import com.microsoft.copilot.eclipse.ui.nes.RenderManager;

/**
 * Handler for clearing the completion ghost text.
 */
public class DiscardSuggestionHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    BaseCompletionManager handler = getActiveCompletionManager();
    if (handler != null) {
      SuggestionUpdateManager manager = handler.getSuggestionUpdateManager();
      if (manager != null) {
        List<String> uuids = manager.getUuids();
        if (uuids != null && !uuids.isEmpty()) {
          notifyRejected(manager);
          handler.clearGhostTexts();
          return null;
        }
      }
    }

    // Fallback: no active completion suggestion -> dismiss Next Edit Suggestion (NES) via EditorsManager
    RenderManager nesManager = getActiveNesRenderManager();
    if (nesManager != null && nesManager.hasActiveSuggestion()) {
      nesManager.clearSuggestion();
    }
    return null;
  }

  private void notifyRejected(SuggestionUpdateManager manager) {
    if (manager == null) {
      return;
    }
    List<String> uuids = manager.getUuids();
    if (uuids == null || uuids.isEmpty()) {
      return;
    }

    NotifyRejectedParams params = new NotifyRejectedParams(uuids);
    getLanguageServerConnection().notifyRejected(params);
  }
}

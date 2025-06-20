package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.completion.SuggestionUpdateManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyRejectedParams;
import com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager;

/**
 * Handler for clearing the completion ghost text.
 */
public class DiscardSuggestionHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    BaseCompletionManager handler = getActiveCompletionManager();
    if (handler != null) {
      notifyRejected(handler.getSuggestionUpdateManager());
      handler.clearGhostTexts();
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

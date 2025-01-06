package com.microsoft.copilot.eclipse.ui.handlers;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.completion.CompletionCollection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyRejectedParams;
import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;

/**
 * Handler for clearing the completion ghost text.
 */
public class DiscardSuggestionHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    CompletionManager handler = getActiveCompletionManager();
    if (handler != null) {
      notifyRejected(handler.getCompletions());
      handler.clearCompletionRendering();
    }
    return null;
  }

  @Override
  public boolean isEnabled() {
    CompletionManager manager = getActiveCompletionManager();
    if (manager != null) {
      return manager.hasCompletion();
    }
    return false;
  }

  private void notifyRejected(CompletionCollection completions) {
    if (completions == null) {
      return;
    }
    List<String> uuids = completions.getUuids();
    if (uuids == null || uuids.isEmpty()) {
      return;
    }

    NotifyRejectedParams params = new NotifyRejectedParams(uuids);
    getLanguageServerConnection().notifyRejected(params);
  }
}

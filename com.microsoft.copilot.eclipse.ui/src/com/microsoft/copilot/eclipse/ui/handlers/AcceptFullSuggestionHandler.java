package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.completion.CompletionCollection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyAcceptedParams;
import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;

/**
 * Handler for accepting the full suggestion.
 */
public class AcceptFullSuggestionHandler extends CopilotHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    CompletionManager handler = getActiveCompletionManager();
    if (handler != null) {
      notifyAccepted(handler.getCompletions());
      handler.acceptFullSuggestion();
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

  private void notifyAccepted(CompletionCollection completions) {
    if (completions == null || completions.getSize() == 0) {
      return;
    }

    CompletionItem item = completions.getCurrentItem();
    String uuid = item.getUuid();
    NotifyAcceptedParams params = new NotifyAcceptedParams(uuid);
    getLanguageServerConnection().notifyAccepted(params);
  }
}

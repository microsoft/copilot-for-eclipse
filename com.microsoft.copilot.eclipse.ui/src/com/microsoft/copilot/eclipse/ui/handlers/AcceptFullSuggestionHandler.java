package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.completion.AcceptSuggestionType;
import com.microsoft.copilot.eclipse.core.completion.SuggestionUpdateManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyAcceptedParams;
import com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager;

/**
 * Handler for accepting the full suggestion.
 */
public class AcceptFullSuggestionHandler extends CopilotHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    BaseCompletionManager handler = getActiveCompletionManager();
    if (handler != null) {
      notifyAccepted(handler.getSuggestionUpdateManager());
      handler.acceptSuggestion(AcceptSuggestionType.FULL);
    }
    return null;
  }

  @Override
  public boolean isEnabled() {
    BaseCompletionManager manager = getActiveCompletionManager();
    if (manager == null) {
      return false;
    }
    SuggestionUpdateManager suggestionUpdateManager = manager.getSuggestionUpdateManager();
    if (suggestionUpdateManager == null) {
      return false;
    }

    CompletionItem item = suggestionUpdateManager.getCurrentItem();
    return item != null;
  }

  private void notifyAccepted(SuggestionUpdateManager manager) {
    if (manager == null) {
      return;
    }

    CompletionItem item = manager.getCurrentItem();
    if (item == null) {
      return;
    }
    String uuid = item.getUuid();
    NotifyAcceptedParams params = new NotifyAcceptedParams(uuid);
    getLanguageServerConnection().notifyAccepted(params);
  }
}

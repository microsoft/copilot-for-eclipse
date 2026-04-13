package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.completion.AcceptSuggestionType;
import com.microsoft.copilot.eclipse.core.completion.SuggestionUpdateManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager;

/**
 * Handler for accepting the next word suggestion.
 */
public class AcceptNextWordHandler extends CopilotHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    BaseCompletionManager handler = getActiveCompletionManager();
    if (handler != null) {
      handler.acceptSuggestion(AcceptSuggestionType.NEXT_WORD);
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

}

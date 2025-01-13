package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;

/**
 * Handler for triggering the inline suggestion.
 */
public class TriggerInlineSuggestionHandler extends CopilotHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    CompletionManager manager = getActiveCompletionManager();
    if (manager != null) {
      manager.triggerCompletion();
    }
    return null;
  }

}

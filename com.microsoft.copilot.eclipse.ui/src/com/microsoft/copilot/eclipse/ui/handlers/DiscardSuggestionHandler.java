package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.ui.completion.CompletionHandler;

/**
 * Handler for clearing the completion ghost text.
 */
public class DiscardSuggestionHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    CompletionHandler handler = getActiveCompletionHandler();
    if (handler != null) {
      handler.clearCompletionRendering();
    }
    return null;
  }

  @Override
  public boolean isEnabled() {
    CompletionHandler handler = getActiveCompletionHandler();
    if (handler != null) {
      return handler.hasCompletion();
    }
    return false;
  }
}

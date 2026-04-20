// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager;

/**
 * Handler for triggering the inline suggestion.
 */
public class TriggerInlineSuggestionHandler extends CopilotHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    BaseCompletionManager manager = getActiveCompletionManager();
    if (manager != null) {
      manager.triggerCompletion();
    }
    return null;
  }

}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;

/**
 * Handler for enabling and disabling auto show completions.
 */
public class AutoShowCompletionsHandler extends AbstractHandler implements IHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    LanguageServerSettingManager settingManager = CopilotUi.getPlugin().getLanguageServerSettingManager();
    boolean autoShowCompletions = settingManager.isAutoShowCompletionEnabled();
    settingManager.setAutoShowCompletion(!autoShowCompletions);

    return null;
  }

}

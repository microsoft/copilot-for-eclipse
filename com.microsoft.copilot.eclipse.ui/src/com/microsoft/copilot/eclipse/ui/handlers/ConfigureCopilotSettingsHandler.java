// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler to open the GitHub Copilot settings configuration page.
 */
public class ConfigureCopilotSettingsHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      UiUtils.openLink(Constants.GITHUB_COPILOT_SETTINGS_URL);
    } catch (Exception e) {
      CopilotCore.LOGGER.error(e);
    }

    return null;
  }
}

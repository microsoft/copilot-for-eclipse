// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for opening the GitHub Copilot Jobs view.
 */
public class OpenGitHubJobsHandler extends CopilotHandler {
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    UiUtils.openE4Part(Constants.GITHUB_JOBS_VIEW_ID);
    return null;
  }
}

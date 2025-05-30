package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Open the manage Copilot page in browser.
 */
public class ManageCopilotHandler extends AbstractHandler {
  //TODO: can be merge with the feedback handler, ManageCopilotOverageHandler , etc.
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      UiUtils.openLink(UiConstants.MANAGE_COPILOT_URL);
    } catch (Exception e) {
      CopilotCore.LOGGER.error(e);
    }

    return null;
  }

}
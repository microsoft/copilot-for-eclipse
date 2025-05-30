package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for opening the Copilot upgrade plan page in the browser.
 */
public class CopilotUpgradePlanHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      UiUtils.openLink(UiConstants.COPILOT_UPGRADE_PLAN_URL);
    } catch (Exception e) {
      CopilotCore.LOGGER.error(e);
    }

    return null;
  }

}

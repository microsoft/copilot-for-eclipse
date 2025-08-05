package com.microsoft.copilot.eclipse.ui.handlers;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler to open a URL in the default web browser. The URL is passed as a parameter in the execution event.
 */
public class OpenUrlHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    String url = event.getParameter(UiConstants.OPEN_URL_PARAMETER_NAME);
    if (StringUtils.isNotBlank(url)) {
      try {
        UiUtils.openLink(url);
      } catch (Exception e) {
        CopilotCore.LOGGER.error(e);
      }
    }

    return null;
  }
}

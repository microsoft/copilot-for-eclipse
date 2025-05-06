package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Handler for opening the preferences dialog.
 */
public class OpenPreferencesHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = SwtUtils.getShellFromEvent(event);
    PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell,
        "com.microsoft.copilot.eclipse.ui.preferences.CopilotPreferencesPage",
        new String[] { "com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage" }, null);
    dialog.open();

    return null;
  }

}

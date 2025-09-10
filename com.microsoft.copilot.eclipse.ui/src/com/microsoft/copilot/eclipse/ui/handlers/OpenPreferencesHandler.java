package com.microsoft.copilot.eclipse.ui.handlers;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.microsoft.copilot.eclipse.ui.preferences.ChatPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CompletionsPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CopilotPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CustomInstructionPreferencePage;
import com.microsoft.copilot.eclipse.ui.preferences.GeneralPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Handler for opening the preferences dialog.
 */
public class OpenPreferencesHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = SwtUtils.getShellFromEvent(event);
    PreferenceDialog dialog;

    String activePageId = event.getParameter("com.microsoft.copilot.eclipse.commands.openPreferences.activePageId");
    if (!StringUtils.isBlank(activePageId)) {
      String[] pageIds = event.getParameter("com.microsoft.copilot.eclipse.commands.openPreferences.pageIds")
          .split(",");
      dialog = PreferencesUtil.createPreferenceDialogOn(shell, activePageId, pageIds, null);
    } else {
      dialog = PreferencesUtil.createPreferenceDialogOn(shell, CopilotPreferencesPage.ID,
          new String[] { GeneralPreferencesPage.ID, ChatPreferencesPage.ID, CompletionsPreferencesPage.ID,
              CustomInstructionPreferencePage.ID, McpPreferencePage.ID },
          null);
    }

    dialog.open();
    return null;
  }

}

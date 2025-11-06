package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class is used to create the root preference page for the plugin.
 */
public class CopilotPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.CopilotPreferencesPage";

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, true));
    Label label = new Label(container, SWT.WRAP);
    label.setText("Select a category to view or change its settings.");

    PreferencePageUtils.createPreferenceLink(getShell(), container, "<a>General</a>", null, GeneralPreferencesPage.ID);
    PreferencePageUtils.createPreferenceLink(getShell(), container, "<a>Chat</a>", null, ChatPreferencesPage.ID);
    PreferencePageUtils.createPreferenceLink(getShell(), container, "<a>Completions</a>", null,
        CompletionsPreferencesPage.ID);
    PreferencePageUtils.createPreferenceLink(getShell(), container, "<a>Custom Instructions</a>", null,
        CustomInstructionPreferencePage.ID);
    PreferencePageUtils.createPreferenceLink(getShell(), container, "<a>Custom Modes</a>", null,
        CustomModesPreferencePage.ID);
    PreferencePageUtils.createPreferenceLink(getShell(), container, "<a>Model Context Protocol (MCP)</a>", null,
        McpPreferencePage.ID);
    PreferencePageUtils.createPreferenceLink(getShell(), container, "<a>Model Management</a>", null,
        ByokPreferencePage.ID);
    return container;
  }

  @Override
  public void init(IWorkbench workbench) {
    // do nothing
  }
}

package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * This class is used to create the preference page for the plugin.
 */
public class CopilotPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  /**
   * Constructor.
   */
  public CopilotPreferencesPage() {
    super(GRID);
  }

  @Override
  public void createFieldEditors() {
    var parent = getFieldEditorParent();
    parent.setLayout(new org.eclipse.swt.layout.GridLayout(1, true));
    var gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);

    // editor group
    Group editorGroup = new Group(parent, SWT.NONE);
    gdf.applyTo(editorGroup);
    editorGroup.setText(Messages.preferences_page_editor_settings);
    // add auto show completion field
    addField(new BooleanFieldEditor(Constants.AUTO_SHOW_COMPLETION, Messages.preferencesPage_autoShowCompletion,
        editorGroup));

    // proxy group
    Group proxyGroup = new Group(parent, SWT.NONE);
    gdf.applyTo(proxyGroup);
    proxyGroup.setText(Messages.preferences_page_proxy_settings);
    // add proxy configuration link
    var link = new Link(proxyGroup, 0);
    gdf.applyTo(link);
    link.setText(Messages.preferences_page_proxy_config_link);
    link.addSelectionListener(new ProxyConfigLinkListener());

    // add strict ssl field
    addField(
        new BooleanFieldEditor(Constants.ENABLE_STRICT_SSL, Messages.preferences_page_enable_strict_ssl, proxyGroup));
    // add kerberos sp field
    addField(
        new StringFieldEditor(Constants.PROXY_KERBEROS_SP, Messages.preferences_page_proxy_kerberos_sp, proxyGroup));

    // auth group
    Group authGroup = new Group(parent, SWT.NONE);
    gdf.applyTo(authGroup);
    authGroup.setText(Messages.preferences_page_auth_settings);
    // add github enterprise field
    addField(
        new StringFieldEditor(Constants.GITHUB_ENTERPRISE, Messages.preferences_page_github_enterprise, authGroup));

  }

  /**
   * Listener for the proxy configuration link.
   */
  public class ProxyConfigLinkListener extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      PreferencesUtil.createPreferenceDialogOn(getShell(), "org.eclipse.ui.net.NetPreferences", null, e);
    }
  }

  @Override
  public void init(IWorkbench workbench) {
    // second parameter is typically the plug-in id
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

}
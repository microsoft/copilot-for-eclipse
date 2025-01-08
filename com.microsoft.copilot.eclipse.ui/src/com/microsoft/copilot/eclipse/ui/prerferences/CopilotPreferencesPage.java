package com.microsoft.copilot.eclipse.ui.prerferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
    addField(new BooleanFieldEditor(Constants.AUTO_SHOW_COMPLETION, Messages.preferencesPage_autoShowCompletion,
        (Composite) parent));

    Group group = new Group(parent, 0);
    group.setText("Proxy Configuration");
    GridDataFactory.fillDefaults().span(2, 1).align(4, 4).grab(true, false).applyTo((Control) group);
    var link = new Link(group, 0);
    link.setText(Messages.preferences_page_proxy_config_link);
    link.addSelectionListener(new ProxyConfigLinkListener());
    addField(new BooleanFieldEditor(Constants.ENABLE_STRICT_SSL, Messages.preferences_page_enable_strict_ssl,
        (Composite) group));
    GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo((Control) link);
    addField(new StringFieldEditor(Constants.PROXY_KERBEROS_SP, Messages.preferences_page_proxy_kerberos_sp,
        (Composite) group));
    addField(new StringFieldEditor(Constants.GITHUB_ENTERPRISE, Messages.preferences_page_github_enterprise,
        (Composite) parent));

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
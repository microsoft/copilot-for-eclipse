package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * General preference page.
 */
public class GeneralPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.GeneralPreferencesPage";

  // Use a dedicated config-scope store for fields that must persist in configuration scope
  private ScopedPreferenceStore configScopeStore;

  /**
   * Constructor.
   */
  public GeneralPreferencesPage() {
    super(GRID);
  }

  @Override
  public void createFieldEditors() {
    Composite parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));
    GridLayout gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);

    // proxy group
    Group grpProxy = new Group(parent, SWT.NONE);
    grpProxy.setLayout(gl);
    gdf.applyTo(grpProxy);
    grpProxy.setText(Messages.preferences_page_proxy_settings);
    // add proxy configuration link
    Composite linkContainer = new Composite(grpProxy, SWT.NONE);
    GridLayout glTextIndent = new GridLayout(1, false);
    glTextIndent.marginLeft = -3;
    glTextIndent.marginBottom = 1;
    linkContainer.setLayout(glTextIndent);
    PreferencePageUtils.createPreferenceLink(getShell(), linkContainer, Messages.preferences_page_proxy_config_link,
        Messages.preferences_page_proxy_config_link_tooltip, "org.eclipse.ui.net.NetPreferences");

    // add strict ssl field
    Composite ctnSsl = new Composite(grpProxy, SWT.NONE);
    ctnSsl.setLayout(gl);
    BooleanFieldEditor bfeSsl = new BooleanFieldEditor(Constants.ENABLE_STRICT_SSL,
        Messages.preferences_page_enable_strict_ssl, ctnSsl);
    bfeSsl.getDescriptionControl(ctnSsl).setToolTipText(Messages.preferences_page_enable_strict_ssl_tooltip);
    addField(bfeSsl);

    // add Note using WrappableNoteLabel
    new WrappableNoteLabel(grpProxy, Messages.preferences_page_note_prefix, Messages.preferences_page_note_content);

    // auth group
    Group grpAuth = new Group(parent, SWT.NONE);
    grpAuth.setLayout(gl);
    gdf.applyTo(grpAuth);
    grpAuth.setText(Messages.preferences_page_auth_settings);
    // add github enterprise field
    Composite ctnGhe = new Composite(grpAuth, SWT.NONE);
    ctnGhe.setLayout(gl);
    ctnGhe.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    StringFieldEditor sftGhe = new StringFieldEditor(Constants.GITHUB_ENTERPRISE,
        Messages.preferences_page_github_enterprise, ctnGhe);
    sftGhe.getLabelControl(ctnGhe).setToolTipText(Messages.preferences_page_github_enterprise_tooltip);
    addField(sftGhe);

    // What's new group
    Group whatsNewGroup = new Group(parent, SWT.NONE);
    whatsNewGroup.setLayout(gl);
    gdf.applyTo(whatsNewGroup);
    whatsNewGroup.setText(Messages.preferences_page_whats_new_settings);

    // auto show "What is new" field
    Composite whatsNewComposite = new Composite(whatsNewGroup, SWT.NONE);
    whatsNewComposite.setLayout(gl);
    whatsNewComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    // Use configuration scope store for this specific field
    this.configScopeStore = new ScopedPreferenceStore(ConfigurationScope.INSTANCE, Constants.PLUGIN_ID);
    BooleanFieldEditor showWhatsNewField = new BooleanFieldEditor(Constants.AUTO_SHOW_WHAT_IS_NEW,
        Messages.preferences_page_enable_whats_new, whatsNewComposite);
    showWhatsNewField.getDescriptionControl(whatsNewComposite)
        .setToolTipText(Messages.preferences_page_enable_whats_new_tooltip);
    // Ensure the preference page manages this field (Apply/Defaults/OK),
    // but keep it stored in configuration scope.
    addField(showWhatsNewField);
    showWhatsNewField.setPreferenceStore(this.configScopeStore);
    showWhatsNewField.load();
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

  @Override
  public boolean performOk() {
    boolean oldWhatsNewValue = this.configScopeStore.getBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW);
    boolean result = super.performOk();

    boolean newWhatsNewValue = this.configScopeStore.getBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW);
    if (oldWhatsNewValue ^ newWhatsNewValue) {
      try {
        IEclipsePreferences configPrefs = ConfigurationScope.INSTANCE.getNode(Constants.PLUGIN_ID);
        configPrefs.putBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, newWhatsNewValue);
        configPrefs.flush();
      } catch (BackingStoreException ex) {
        CopilotCore.LOGGER.error("Failed to persist 'Auto show What's New' preference in ConfigurationScope", ex);
      }
    }

    return result;
  }

}
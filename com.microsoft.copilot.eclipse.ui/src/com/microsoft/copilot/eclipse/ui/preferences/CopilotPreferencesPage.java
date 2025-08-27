package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * This class is used to create the preference page for the plugin.
 */
public class CopilotPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  private Composite parent;
  private ProxyConfigLinkListener proxyConfigLinkListener;
  private Link link;
  private ControlListener controlListener;
  // Use a dedicated config-scope store for fields that must persist in configuration scope
  private ScopedPreferenceStore configScopeStore;

  /**
   * Constructor.
   */
  public CopilotPreferencesPage() {
    super(GRID);
  }

  @Override
  public void createFieldEditors() {
    this.parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));
    var gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    // editor group
    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);
    Group grpEditor = new Group(parent, SWT.NONE);
    grpEditor.setLayout(gl);
    gdf.applyTo(grpEditor);
    grpEditor.setText(Messages.preferences_page_editor_settings);
    // add auto show completion field
    var ctnAutoComplete = new Composite(grpEditor, SWT.NONE);
    ctnAutoComplete.setLayout(gl);
    var bfeAutoComplete = new BooleanFieldEditor(Constants.AUTO_SHOW_COMPLETION,
        Messages.preferencesPage_autoShowCompletion, ctnAutoComplete);
    addField(bfeAutoComplete);

    // proxy group
    Group grpProxy = new Group(parent, SWT.NONE);
    grpProxy.setLayout(gl);
    gdf.applyTo(grpProxy);
    grpProxy.setText(Messages.preferences_page_proxy_settings);
    // add proxy configuration link
    var linkContainer = new Composite(grpProxy, SWT.NONE);
    var glTextIndent = new GridLayout(1, false);
    glTextIndent.marginLeft = -3;
    glTextIndent.marginBottom = 1;
    linkContainer.setLayout(glTextIndent);
    this.link = new Link(linkContainer, SWT.NONE);
    link.setText(Messages.preferences_page_proxy_config_link);
    link.setToolTipText(Messages.preferences_page_proxy_config_link_tooltip);
    this.proxyConfigLinkListener = new ProxyConfigLinkListener();
    link.addSelectionListener(this.proxyConfigLinkListener);

    // add strict ssl field
    var ctnSsl = new Composite(grpProxy, SWT.NONE);
    ctnSsl.setLayout(gl);
    var bfeSsl = new BooleanFieldEditor(Constants.ENABLE_STRICT_SSL, Messages.preferences_page_enable_strict_ssl,
        ctnSsl);
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
    var ctnGhe = new Composite(grpAuth, SWT.NONE);
    ctnGhe.setLayout(gl);
    ctnGhe.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    var sftGhe = new StringFieldEditor(Constants.GITHUB_ENTERPRISE, Messages.preferences_page_github_enterprise,
        ctnGhe);
    sftGhe.getLabelControl(ctnGhe).setToolTipText(Messages.preferences_page_github_enterprise_tooltip);
    addField(sftGhe);

    // chat group
    Group chatGroup = new Group(parent, SWT.NONE);
    chatGroup.setLayout(gl);
    gdf.applyTo(chatGroup);
    chatGroup.setText(Messages.preferences_page_chat_settings);
    Composite chatComposite = new Composite(chatGroup, SWT.NONE);
    chatComposite.setLayout(gl);
    chatComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    BooleanFieldEditor workspaceContextField = new BooleanFieldEditor(Constants.WORKSPACE_CONTEXT_ENABLED,
        Messages.preferences_page_watched_files, SWT.WRAP, chatComposite);
    GridData workspaceContextFieldGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    workspaceContextFieldGridData.widthHint = 400;
    workspaceContextField.getDescriptionControl(chatComposite).setLayoutData(workspaceContextFieldGridData);

    addField(workspaceContextField);

    // add chat note using WrappableNoteLabel
    new WrappableNoteLabel(chatGroup, Messages.preferences_page_note_prefix,
        Messages.preferences_page_watched_files_note_content);

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

    // Add control listener to handle workspace context field resizing
    controlListener = new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        // resize the workspace context field description
        var pg = CopilotPreferencesPage.this;
        int width = pg.getFieldEditorParent().getSize().x - 20;
        ((GridData) workspaceContextField.getDescriptionControl(chatComposite).getLayoutData()).widthHint = width;
        pg.getFieldEditorParent().layout();
      }
    };
    parent.addControlListener(controlListener);
    parent.addDisposeListener(e -> {
      parent.removeControlListener(controlListener);
    });
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
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

  @Override
  public boolean performOk() {
    boolean oldWorkspaceContextValue = getPreferenceStore().getBoolean(Constants.WORKSPACE_CONTEXT_ENABLED);
    boolean oldWhatsNewValue = this.configScopeStore.getBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW);

    boolean result = super.performOk();
    boolean newWorkspaceContextValue = getPreferenceStore().getBoolean(Constants.WORKSPACE_CONTEXT_ENABLED);
    if (oldWorkspaceContextValue ^ newWorkspaceContextValue) {
      boolean restart = MessageDialog.openQuestion(getShell(), Messages.preferences_page_restart_required,
          Messages.preferences_page_watched_files_restart_question);

      if (restart) {
        try {
          // Explicitly save the preferences to disk to ensure they persist across the restart
          // CopilotUi.getPlugin().savePluginPreferences() is deprecated, flush is recommended
          InstanceScope.INSTANCE.getNode("com.microsoft.copilot.eclipse.ui").flush();
        } catch (BackingStoreException e) {
          CopilotCore.LOGGER.error("Failed to save preference 'Enable workspace context'", e);
        }

        // Close the preference dialog properly before restarting
        getShell().getDisplay().asyncExec(() -> {
          // Using asyncExec ensures the preference dialog completes its current operations
          PlatformUI.getWorkbench().restart();
        });
      }
    }

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

  @Override
  public void dispose() {
    if (link != null && !link.isDisposed() && proxyConfigLinkListener != null) {
      link.removeSelectionListener(proxyConfigLinkListener);
    }
    super.dispose();
  }
}
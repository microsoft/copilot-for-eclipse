package com.microsoft.copilot.eclipse.ui.preferences;

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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
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

  private Label lblProxyNoteContent;

  /**
   * Constructor.
   */
  public CopilotPreferencesPage() {
    super(GRID);
  }

  @Override
  public void createFieldEditors() {
    var parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));
    var gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    // editor group
    var gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);
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
    var link = new Link(linkContainer, SWT.NONE);
    link.setText(Messages.preferences_page_proxy_config_link);
    link.setToolTipText(Messages.preferences_page_proxy_config_link_tooltip);
    link.addSelectionListener(new ProxyConfigLinkListener());

    // add strict ssl field
    var ctnSsl = new Composite(grpProxy, SWT.NONE);
    ctnSsl.setLayout(gl);
    var bfeSsl = new BooleanFieldEditor(Constants.ENABLE_STRICT_SSL, Messages.preferences_page_enable_strict_ssl,
        ctnSsl);
    bfeSsl.getDescriptionControl(ctnSsl).setToolTipText(Messages.preferences_page_enable_strict_ssl_tooltip);
    addField(bfeSsl);

    // add Note
    var ctnNote = new Composite(grpProxy, SWT.NONE);
    ctnNote.setLayout(glTextIndent);
    ctnNote.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    var lblProxyNote = new Label(ctnNote, SWT.NONE);
    lblProxyNote.setText(Messages.preferences_page_note_text);
    FontData[] fontData = lblProxyNote.getFont().getFontData();
    for (FontData fd : fontData) {
      fd.setStyle(SWT.BOLD);
    }
    Font boldFont = new Font(parent.getDisplay(), fontData);
    lblProxyNote.setFont(boldFont);

    this.lblProxyNoteContent = new Label(ctnNote, SWT.WRAP);
    this.lblProxyNoteContent.setText(Messages.preferences_page_note_content);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.widthHint = 400;
    this.lblProxyNoteContent.setLayoutData(gd);
    // add kerberos sp field
    // addField(
    // new StringFieldEditor(Constants.PROXY_KERBEROS_SP, Messages.preferences_page_proxy_kerberos_sp, grpProxy));

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

    parent.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        // resize the note label
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        var pg = CopilotPreferencesPage.this;
        gd.widthHint = pg.getFieldEditorParent().getSize().x - 20;
        pg.lblProxyNoteContent.setLayoutData(gd);
        pg.getFieldEditorParent().layout();
      }
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
    // second parameter is typically the plug-in id
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

}
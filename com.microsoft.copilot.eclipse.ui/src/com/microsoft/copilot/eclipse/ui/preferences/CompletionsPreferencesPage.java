package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

/**
 * Completions preference page.
 */
public class CompletionsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.CompletionsPreferencesPage";

  private static final String CODE_MINING_PREF_PAGE_ID =
      "org.eclipse.jdt.ui.preferences.JavaEditorCodeMiningPreferencePage";

  /**
   * Constructor.
   */
  public CompletionsPreferencesPage() {
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

    Composite completionComposite = new Composite(parent, SWT.NONE);
    completionComposite.setLayout(gl);
    gdf.applyTo(completionComposite);
    // add auto show completion field
    Composite ctnAutoComplete = new Composite(completionComposite, SWT.NONE);
    ctnAutoComplete.setLayout(gl);
    BooleanFieldEditor bfeAutoComplete = new BooleanFieldEditor(Constants.AUTO_SHOW_COMPLETION,
        Messages.preferences_page_completions_autoShowCompletion, ctnAutoComplete);
    addField(bfeAutoComplete);

    // If JDT UI is available, add a link to the code mining preference page.
    // The ghost text will not be shown, if code mining is disabled.
    if (Platform.getBundle("org.eclipse.jdt.ui") != null) {
      PreferencePageUtils.createPreferenceLink(getShell(), completionComposite,
          Messages.preferences_page_completions_codeMiningNote, null, CODE_MINING_PREF_PAGE_ID);
    }

  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }
}
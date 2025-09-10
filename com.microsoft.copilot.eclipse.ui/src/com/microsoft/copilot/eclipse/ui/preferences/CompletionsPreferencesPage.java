package com.microsoft.copilot.eclipse.ui.preferences;

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
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Completions preference page.
 */
public class CompletionsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.CompletionsPreferencesPage";

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
        Messages.preferencesPage_autoShowCompletion, ctnAutoComplete);
    addField(bfeAutoComplete);
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }
}
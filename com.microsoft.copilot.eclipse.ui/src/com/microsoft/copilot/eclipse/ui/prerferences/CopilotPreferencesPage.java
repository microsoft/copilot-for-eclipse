package com.microsoft.copilot.eclipse.ui.prerferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

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
    var editorGroup = new Composite(parent, 0);
    addField(new BooleanFieldEditor(Constants.AUTO_SHOW_COMPLETION, Messages.preferencesPage_autoShowCompletion,
        (Composite) editorGroup));
  }

  @Override
  public void init(IWorkbench workbench) {
    // second parameter is typically the plug-in id
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

}
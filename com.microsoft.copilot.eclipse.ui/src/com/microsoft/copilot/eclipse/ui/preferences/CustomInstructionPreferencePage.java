package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Preference page for GitHub Copilot Custom Instructions settings.
 */
public class CustomInstructionPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  private BooleanFieldEditor enableWorkspaceInstrField;

  /**
   * Constructor for CustomInstructionPreferencePage.
   */
  public CustomInstructionPreferencePage() {
    super(GRID);
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

  @Override
  protected void createFieldEditors() {
    Composite parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));
    GridLayout gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    createWorkspaceInstructionsField(parent, gl);
  }

  private void createWorkspaceInstructionsField(Composite parent, GridLayout gl) {
    // workspace instructions group
    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);
    Group workspaceInstrGroup = new Group(parent, SWT.NONE);
    workspaceInstrGroup.setLayout(gl);
    gdf.applyTo(workspaceInstrGroup);
    workspaceInstrGroup.setText(Messages.preferences_page_custom_instructions_copilot_instructions);

    // Add workspace instructions field
    Composite workspaceInstrFieldContainer = new Composite(workspaceInstrGroup, SWT.NONE);
    workspaceInstrFieldContainer.setLayout(gl);
    workspaceInstrFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    Label workspaceDescLabel = new Label(workspaceInstrFieldContainer, SWT.WRAP);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    workspaceDescLabel.setLayoutData(gd);
    workspaceDescLabel.setText(Messages.preferences_page_custom_instructions_workspace);
    // Bold the workspace instructions title
    FontDescriptor boldDescriptor = FontDescriptor.createFrom(workspaceDescLabel.getFont()).setStyle(SWT.BOLD);
    Font boldFont = boldDescriptor.createFont(workspaceDescLabel.getDisplay());
    workspaceDescLabel.setFont(boldFont);
    workspaceDescLabel.addDisposeListener(e -> boldFont.dispose());

    // Add enable workspace instructions checkbox - use same parent as workspaceInstrField
    enableWorkspaceInstrField = new BooleanFieldEditor(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED,
        Messages.preferences_page_custom_instructions_workspace_enable, workspaceInstrFieldContainer);
    addField(enableWorkspaceInstrField);

    StringFieldEditor workspaceInstrField = new StringFieldEditor(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE,
        Messages.preferences_page_custom_instructions_copilot_instructions_desc, StringFieldEditor.UNLIMITED, 4,
        StringFieldEditor.VALIDATE_ON_KEY_STROKE, workspaceInstrFieldContainer);
    Label workspaceInstrFieldLabel = workspaceInstrField.getLabelControl(workspaceInstrFieldContainer);
    workspaceInstrFieldLabel.setToolTipText(Messages.preferences_page_custom_instructions_workspace_tooltip);
    // @formatter:off
    workspaceInstrFieldLabel.setLayoutData(new GridData(
        SWT.LEFT, 
        SWT.TOP, 
        false, 
        false, 
        2, // The label-control will take up 2 column cells itself, so the text-control will be underneath it.
        1));
    // @formatter:on
    addField(workspaceInstrField);

    // Add note using WrappableNoteLabel
    new WrappableNoteLabel(workspaceInstrGroup, Messages.preferences_page_note_prefix,
        Messages.preferences_page_custom_instructions_copilot_instructions_note);
  }
}
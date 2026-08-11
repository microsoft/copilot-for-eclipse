// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

/**
 * Chat preference page.
 */
public class ChatPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.ChatPreferencesPage";
  private static final int FIELD_WIDTH_HINT = 400;

  /**
   * Constructor.
   */
  public ChatPreferencesPage() {
    super(GRID);
  }

  @Override
  public void createFieldEditors() {
    Composite parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));

    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);

    Composite skillsComposite = createSectionComposite(parent, gdf);
    BooleanFieldEditor skillsField = new BooleanFieldEditor(Constants.ENABLE_SKILLS,
        Messages.preferences_page_skills_enabled, SWT.WRAP, skillsComposite);
    applyFieldWidthHint(skillsField, skillsComposite);
    addField(skillsField);

    addNote(parent, Messages.preferences_page_skills_enabled_note_content);
    addSeparator(parent);

    // Add Agent Max Requests field
    Composite agentMaxRequestsComposite = createSectionComposite(parent, gdf);

    IntegerFieldEditor agentMaxRequestsField = new IntegerFieldEditor(Constants.AGENT_MAX_REQUESTS,
        Messages.preferences_page_agent_max_requests, agentMaxRequestsComposite);
    agentMaxRequestsField.setValidRange(1, 500);
    agentMaxRequestsField.setErrorMessage(Messages.preferences_page_agent_max_requests_validation_error);
    addField(agentMaxRequestsField);

    addNote(parent, Messages.preferences_page_agent_max_requests_desc);
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

  private Composite createSectionComposite(Composite parent, GridDataFactory gdf) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(1, true));
    gdf.applyTo(composite);
    return composite;
  }

  private void applyFieldWidthHint(BooleanFieldEditor field, Composite parent) {
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    gridData.widthHint = FIELD_WIDTH_HINT;
    field.getDescriptionControl(parent).setLayoutData(gridData);
  }

  private void addNote(Composite parent, String noteContent) {
    WrappableNoteLabel note = new WrappableNoteLabel(parent, Messages.preferences_page_note_prefix + " ", noteContent);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gridData.horizontalSpan = 2;
    note.setLayoutData(gridData);
  }

  private void addSeparator(Composite parent) {
    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gridData.horizontalSpan = 2;
    separator.setLayoutData(gridData);
  }
}
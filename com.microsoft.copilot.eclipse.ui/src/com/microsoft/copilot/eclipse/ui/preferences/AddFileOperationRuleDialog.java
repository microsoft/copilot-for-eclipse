// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for adding a file-operation auto-approve rule with pattern, description, and allow/deny.
 */
public class AddFileOperationRuleDialog extends Dialog {

  private Text patternText;
  private Text descriptionText;
  private Button allowRadio;

  private String pattern;
  private String description;
  private boolean autoApprove;

  /**
   * Creates the dialog.
   *
   * @param parent the parent shell
   */
  public AddFileOperationRuleDialog(Shell parent) {
    super(parent);
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(
        Messages.preferences_page_file_op_auto_approve_add_dialog_title);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);
    container.setLayout(new GridLayout(2, false));
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // Pattern *
    Label patternLabel = new Label(container, SWT.NONE);
    patternLabel.setText(
        Messages.preferences_page_file_op_auto_approve_add_dialog_pattern
            + " *");
    patternText = new Text(container, SWT.BORDER);
    GridData patternData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    patternData.widthHint = 300;
    patternText.setLayoutData(patternData);
    patternText.setMessage(
        Messages.preferences_page_file_op_auto_approve_add_dialog_pattern_hint);
    patternText.addModifyListener(e -> updateOkButton());

    // Description
    Label descLabel = new Label(container, SWT.NONE);
    descLabel.setText(
        Messages.preferences_page_file_op_auto_approve_add_dialog_description);
    descriptionText = new Text(container, SWT.BORDER);
    descriptionText.setMessage(
        Messages.preferences_page_file_op_auto_approve_add_dialog_description_hint);
    descriptionText.setLayoutData(
        new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Allow / Deny
    Label approveLabel = new Label(container, SWT.NONE);
    approveLabel.setText(
        Messages.preferences_page_auto_approve_add_dialog_approve);
    Composite radioGroup = new Composite(container, SWT.NONE);
    radioGroup.setLayout(new GridLayout(2, false));
    allowRadio = new Button(radioGroup, SWT.RADIO);
    allowRadio.setText(Messages.preferences_page_auto_approve_allow);
    allowRadio.setSelection(true);
    Button denyRadio = new Button(radioGroup, SWT.RADIO);
    denyRadio.setText(Messages.preferences_page_auto_approve_deny);

    return area;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    updateOkButton();
  }

  private void updateOkButton() {
    Button ok = getButton(OK);
    if (ok != null) {
      ok.setEnabled(
          patternText != null && !patternText.getText().trim().isEmpty());
    }
  }

  @Override
  protected void okPressed() {
    pattern = patternText.getText().trim();
    description = descriptionText.getText().trim();
    autoApprove = allowRadio.getSelection();
    super.okPressed();
  }

  public String getPattern() {
    return pattern;
  }

  public String getDescription() {
    return description;
  }

  public boolean isAutoApprove() {
    return autoApprove;
  }
}

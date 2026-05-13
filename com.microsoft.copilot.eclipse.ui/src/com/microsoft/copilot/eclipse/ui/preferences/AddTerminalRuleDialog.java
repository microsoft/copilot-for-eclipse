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
 * Dialog for adding a terminal auto-approve rule with command and allow/deny.
 */
public class AddTerminalRuleDialog extends Dialog {

  private Text commandText;
  private Button allowRadio;

  private String command;
  private boolean autoApprove;

  /**
   * Creates the dialog.
   *
   * @param parent the parent shell
   */
  public AddTerminalRuleDialog(Shell parent) {
    super(parent);
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(Messages.preferences_page_terminal_auto_approve_add_dialog_title);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);
    container.setLayout(new GridLayout(2, false));
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // Command *
    Label commandLabel = new Label(container, SWT.NONE);
    commandLabel.setText(
        Messages.preferences_page_terminal_auto_approve_add_dialog_command + " *");
    commandText = new Text(container, SWT.BORDER);
    GridData commandData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    commandData.widthHint = 300;
    commandText.setLayoutData(commandData);
    commandText.setMessage(Messages.preferences_page_terminal_auto_approve_add_dialog_placeholder);
    commandText.addModifyListener(e -> updateOkButton());

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
      ok.setEnabled(commandText != null && !commandText.getText().trim().isEmpty());
    }
  }

  @Override
  protected void okPressed() {
    command = commandText.getText().trim();
    autoApprove = allowRadio.getSelection();
    super.okPressed();
  }

  public String getCommand() {
    return command;
  }

  public boolean isAutoApprove() {
    return autoApprove;
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
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
 * Dialog for configuring the URL shared by Ollama models.
 */
public class AddOllamaUrlDialog extends TrayDialog {

  private static final String DEFAULT_ENDPOINT = "http://localhost:11434";
  private static final int CONTAINER_WIDTH = 400;

  private final String endpoint;
  private final boolean editMode;
  private final Consumer<String> onSave;
  private Text endpointText;
  private Button okButton;

  /**
   * Creates an Ollama URL dialog.
   *
   * @param parentShell parent shell
   * @param endpoint    existing endpoint, or {@code null} for the default
   * @param onSave      endpoint consumer
   */
  public AddOllamaUrlDialog(Shell parentShell, String endpoint, Consumer<String> onSave) {
    super(parentShell);
    this.editMode = StringUtils.isNotBlank(endpoint);
    this.endpoint = StringUtils.defaultIfBlank(endpoint, DEFAULT_ENDPOINT);
    this.onSave = onSave;
    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(editMode ? Messages.preferences_page_byok_ollama_dialog_title
        : Messages.preferences_page_byok_ollama_create_dialog_title);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 10;
    layout.marginHeight = 10;
    container.setLayout(layout);
    GridData containerData = new GridData(SWT.FILL, SWT.FILL, true, true);
    containerData.widthHint = CONTAINER_WIDTH;
    container.setLayoutData(containerData);

    new Label(container, SWT.NONE).setText(Messages.preferences_page_byok_ollama_endpoint);
    endpointText = new Text(container, SWT.BORDER);
    endpointText.setText(endpoint);
    endpointText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    endpointText.addModifyListener(event -> updateOkButton());
    return container;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    updateOkButton();
  }

  @Override
  protected void okPressed() {
    String newEndpoint = endpointText.getText().trim();
    if (!isValidEndpoint(newEndpoint)) {
      endpointText.setFocus();
      return;
    }
    onSave.accept(newEndpoint);
    super.okPressed();
  }

  private void updateOkButton() {
    if (okButton != null && !okButton.isDisposed()) {
      okButton.setEnabled(isValidEndpoint(endpointText.getText().trim()));
    }
  }

  private boolean isValidEndpoint(String value) {
    try {
      URI uri = new URI(value);
      return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          && StringUtils.isNotBlank(uri.getHost());
    } catch (URISyntaxException e) {
      return false;
    }
  }
}

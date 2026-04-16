// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.dialogs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.copilot.eclipse.core.lsp.mcp.McpOauthRequest;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpOauthRequest.DynamicOauthInput;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog for handling Dynamic OAuth requests from MCP servers.
 */
public class DynamicOauthDialog extends Dialog {
  private final McpOauthRequest request;
  private final Map<String, Text> inputFields = new HashMap<>();
  private Map<String, String> resultValues = null;
  private Font hintFont;
  private Font headerFont;
  private Button okButton;
  private Image icon;

  /**
   * Constructor.
   *
   * @param parentShell the parent shell
   * @param request the OAuth request containing input field configurations
   */
  public DynamicOauthDialog(Shell parentShell, McpOauthRequest request) {
    super(parentShell);
    this.request = request;
    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    if (request.getTitle() != null) {
      newShell.setText(request.getTitle());
    }

    icon = UiUtils.buildImageFromPngPath("/icons/github_copilot.png");
    if (icon != null) {
      newShell.setImage(icon);
    }

    newShell.addDisposeListener(e -> {
      disposeResources();
    });
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 20;
    layout.marginHeight = 20;
    layout.verticalSpacing = 10;
    Composite container = (Composite) super.createDialogArea(parent);
    container.setLayout(layout);

    if (request.getHeader() != null && !request.getHeader().isBlank()) {
      Label headerLabel = new Label(container, SWT.WRAP);
      headerLabel.setText(request.getHeader());

      FontData[] fontData = headerLabel.getFont().getFontData();
      for (FontData fd : fontData) {
        fd.setStyle(SWT.BOLD);
        fd.setHeight(fd.getHeight() + 2);
      }
      headerFont = new Font(headerLabel.getDisplay(), fontData);
      headerLabel.setFont(headerFont);

      GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
      headerLabel.setLayoutData(gd);
    }

    if (request.getDetail() != null && !request.getDetail().isBlank()) {
      Label detailLabel = new Label(container, SWT.WRAP);
      detailLabel.setText(request.getDetail());
      GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
      detailLabel.setLayoutData(gd);
    }

    if (request.getInputs() != null) {
      Label spacer = new Label(container, SWT.NONE);
      GridData spacerGd = new GridData();
      spacerGd.heightHint = 10;
      spacer.setLayoutData(spacerGd);
      for (DynamicOauthInput input : request.getInputs()) {
        createInputField(container, input);
      }
    }
    return container;
  }

  private void createInputField(Composite parent, DynamicOauthInput input) {
    // Create a row container with 2 columns: label on left, input+hint on right
    GridLayout rowLayout = new GridLayout(2, false);
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    rowLayout.horizontalSpacing = 10;
    Composite fieldRow = new Composite(parent, SWT.NONE);
    fieldRow.setLayout(rowLayout);
    GridData rowData = new GridData(SWT.FILL, SWT.TOP, true, false);
    rowData.verticalIndent = 5;
    fieldRow.setLayoutData(rowData);

    String labelText = input.getTitle();
    if (Boolean.TRUE.equals(input.getRequired())) {
      labelText += " *";
    }

    Label fieldLabel = new Label(fieldRow, SWT.NONE);
    fieldLabel.setText(labelText);
    GridData labelData = new GridData(SWT.LEFT, SWT.TOP, false, false);
    labelData.widthHint = 120;
    fieldLabel.setLayoutData(labelData);

    // Create a container for text field and hint text (right column)
    GridLayout inputGroupLayout = new GridLayout(1, false);
    inputGroupLayout.marginWidth = 0;
    inputGroupLayout.marginHeight = 0;
    inputGroupLayout.verticalSpacing = 2;
    Composite inputGroup = new Composite(fieldRow, SWT.NONE);
    inputGroup.setLayout(inputGroupLayout);
    GridData inputGroupData = new GridData(SWT.FILL, SWT.TOP, true, false);
    inputGroup.setLayoutData(inputGroupData);
    Text textField = new Text(inputGroup, SWT.BORDER | SWT.SINGLE);
    if (input.getPlaceholder() != null && !input.getPlaceholder().isEmpty()) {
      textField.setMessage(input.getPlaceholder());
    }

    GridData textData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    textField.setLayoutData(textData);

    String fieldKey = input.getValue();
    inputFields.put(fieldKey, textField);

    if (Boolean.TRUE.equals(input.getRequired())) {
      textField.addModifyListener(e -> validateRequiredFields());
    }
    if (input.getDescription() != null && !input.getDescription().isBlank()) {
      Label hintLabel = new Label(inputGroup, SWT.WRAP);
      hintLabel.setText(input.getDescription());
      if (hintFont == null) {
        FontData[] fontData = hintLabel.getFont().getFontData();
        for (FontData fd : fontData) {
          fd.setHeight(fd.getHeight() - 1);
        }
        hintFont = new Font(hintLabel.getDisplay(), fontData);
      }

      hintLabel.setFont(hintFont);
      hintLabel.setForeground(hintLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
      GridData hintData = new GridData(SWT.FILL, SWT.TOP, true, false);
      hintLabel.setLayoutData(hintData);
    }
  }

  /**
   * Validates all required fields and enables/disables the OK button accordingly.
   */
  private void validateRequiredFields() {
    if (okButton == null || okButton.isDisposed()) {
      return;
    }

    // Check if all required fields have non-empty values
    if (request.getInputs() != null) {
      for (DynamicOauthInput input : request.getInputs()) {
        if (Boolean.TRUE.equals(input.getRequired())) {
          String key = input.getValue();
          Text textField = inputFields.get(key);
          if (textField != null && (textField.getText() == null || textField.getText().trim().isEmpty())) {
            okButton.setEnabled(false);
            return;
          }
        }
      }
    }

    // All required fields are filled
    okButton.setEnabled(true);
  }

  /**
   * Disposes all resources to prevent resource leaks.
   */
  private void disposeResources() {
    if (hintFont != null && !hintFont.isDisposed()) {
      hintFont.dispose();
      hintFont = null;
    }
    if (headerFont != null && !headerFont.isDisposed()) {
      headerFont.dispose();
      headerFont = null;
    }
    if (icon != null && !icon.isDisposed()) {
      icon.dispose();
      icon = null;
    }
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    validateRequiredFields();
  }

  @Override
  protected void okPressed() {
    resultValues = new HashMap<>();
    for (Map.Entry<String, Text> entry : inputFields.entrySet()) {
      resultValues.put(entry.getKey(), entry.getValue().getText());
    }

    super.okPressed();
  }

  @Override
  protected void cancelPressed() {
    resultValues = null;
    super.cancelPressed();
  }

  @Override
  public boolean close() {
    disposeResources();
    return super.close();
  }

  /**
   * Gets the input values entered by the user.
   *
   * @return a map of field names to values, or null if the dialog was cancelled
   */
  public Map<String, String> getInputValues() {
    return resultValues;
  }

  @Override
  protected Point getInitialSize() {
    return new Point(600, 400);
  }

  @Override
  protected boolean isResizable() {
    return true;
  }
}

package com.microsoft.copilot.eclipse.ui.preferences;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog for adding or editing BYOK API keys.
 */
public class AddApiKeyDialog extends TrayDialog {
  private static final int CONTAINER_WIDTH = 400;
  private String providerName;
  private String apiKey;
  private Text apiKeyText;
  private Button toggleEyeBtn;
  private Image eyeOpenImg;
  private Image eyeClosedImg;
  private Button addButton;
  private boolean hasApiKey;
  private Consumer<String> onSave;

  /**
   * Constructor for AddApiKeyDialog.
   */
  public AddApiKeyDialog(Shell parentShell, String providerName, Consumer<String> onSave) {
    this(parentShell, providerName, null, onSave);
  }

  /**
   * Constructor for ChangeApiKeyDialog with existing API key.
   */
  public AddApiKeyDialog(Shell parentShell, String providerName, String existingApiKey, Consumer<String> onSave) {
    super(parentShell);
    this.providerName = providerName;
    this.apiKey = existingApiKey;
    this.hasApiKey = true;
    this.onSave = onSave;
    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    String title = (apiKey == null) ? String.format(Messages.preferences_page_byok_addModel_dialog_title, providerName)
        : providerName;
    newShell.setText(title);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    String helpContextId = getHelpContextId();
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, helpContextId);
    Composite container = (Composite) super.createDialogArea(parent);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 10;
    layout.marginHeight = 10;
    container.setLayout(layout);
    GridData containerGd = new GridData(SWT.FILL, SWT.FILL, true, true);
    containerGd.widthHint = CONTAINER_WIDTH;
    container.setLayoutData(containerGd);
    // API Key field
    new Label(container, SWT.NONE).setText(Messages.preferences_page_byok_addModel_apiKey);
    Composite apiKeyRow = new Composite(container, SWT.NONE);
    GridLayout rowLayout = new GridLayout(2, false);
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    apiKeyRow.setLayout(rowLayout);
    apiKeyRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    apiKeyText = new Text(apiKeyRow, SWT.BORDER);
    apiKeyText.setEchoChar('*');
    apiKeyText.setText(apiKey != null ? apiKey : "");
    apiKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    apiKeyText.addModifyListener(this::onFieldChanged);

    eyeOpenImg = UiUtils.buildImageFromPngPath("/icons/chat/eye.png");
    eyeClosedImg = UiUtils.buildImageFromPngPath("/icons/chat/eye_closed.png");
    toggleEyeBtn = new Button(apiKeyRow, SWT.PUSH);
    toggleEyeBtn.setImage(eyeClosedImg);
    toggleEyeBtn.addListener(SWT.Selection, e -> togglePasswordVisibility());
    toggleEyeBtn.addDisposeListener(e -> {
      if (eyeOpenImg != null && !eyeOpenImg.isDisposed()) {
        eyeOpenImg.dispose();
      }
      if (eyeClosedImg != null && !eyeClosedImg.isDisposed()) {
        eyeClosedImg.dispose();
      }
    });

    return container;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    addButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    addButton.setEnabled(hasApiKey);
  }

  @Override
  protected void okPressed() {
    apiKey = apiKeyText.getText().trim();
    if (apiKey.isEmpty()) {
      apiKeyText.setFocus();
      return;
    }
    onSave.accept(apiKey);
    super.okPressed();
  }
  
  /**
   * Get the appropriate help context ID based on the provider.
   */
  private String getHelpContextId() {
    return "com.microsoft.copilot.eclipse.ui.add_byok_model_dialog_" + providerName.toLowerCase();
  }


  private void onFieldChanged(ModifyEvent e) {
    validateAll();
  }

  private boolean validateAll() {
    boolean ok = StringUtils.isNotBlank(apiKeyText.getText());
    if (addButton != null) {
      addButton.setEnabled(ok);
    }
    return ok;
  }

  private void togglePasswordVisibility() {
    if (apiKeyText == null || toggleEyeBtn == null) {
      return;
    }
    if (apiKeyText.getEchoChar() == '*') {
      apiKeyText.setEchoChar('\0');
      toggleEyeBtn.setImage(eyeOpenImg);
    } else {
      apiKeyText.setEchoChar('*');
      toggleEyeBtn.setImage(eyeClosedImg);
    }
  }
}
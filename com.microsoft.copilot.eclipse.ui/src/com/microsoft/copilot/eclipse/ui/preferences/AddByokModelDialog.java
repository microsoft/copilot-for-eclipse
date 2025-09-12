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

import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModelCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModelProvider;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog to add a new BYOK model that adapts based on the provider.
 */
public class AddByokModelDialog extends TrayDialog {

  private static final int CONTAINER_WIDTH = 600;

  private Text modelIdText;
  private Text deploymentUrlText;
  private Text apiKeyText;
  private Text displayNameText;
  private Button supportToolCallingCheck;
  private Button supportVisionCheck;
  private Button addButton;

  private Button toggleEyeBtn;
  private Image eyeOpenImg;
  private Image eyeClosedImg;

  private final String providerName;
  private final Consumer<ByokModel> onSave;

  /**
   * Create the dialog.
   */
  public AddByokModelDialog(Shell parentShell, String providerName, Consumer<ByokModel> onSave) {
    super(parentShell);
    this.providerName = providerName;
    this.onSave = onSave;
    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(Messages.preferences_page_byok_addModel_dialog_title);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    String helpContextId = getHelpContextId();
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, helpContextId);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 20;
    layout.marginHeight = 20;
    layout.verticalSpacing = 15;
    Composite container = (Composite) super.createDialogArea(parent);
    container.setLayout(layout);
    GridData containerGd = new GridData(SWT.FILL, SWT.FILL, true, true);
    containerGd.widthHint = CONTAINER_WIDTH;
    container.setLayoutData(containerGd);

    // Model ID *
    new Label(container, SWT.NONE).setText(Messages.preferences_page_byok_addModel_modelId);
    modelIdText = new Text(container, SWT.BORDER);
    modelIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    modelIdText.addModifyListener(this::onFieldChanged);

    // Provider-specific fields
    if (providerName.equals(ByokModelProvider.AZURE.getDisplayName())) {
      createAzureSpecificFields(container);
    }

    // Display Name (optional for all providers)
    new Label(container, SWT.NONE).setText(Messages.preferences_page_byok_addModel_displayName);
    displayNameText = new Text(container, SWT.BORDER);
    displayNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Capabilities
    Composite caps = new Composite(container, SWT.NONE);
    GridLayout capsLayout = new GridLayout(2, false);
    capsLayout.marginWidth = 0;
    capsLayout.marginHeight = 0;
    caps.setLayout(capsLayout);
    GridData capsGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
    capsGd.horizontalSpan = 2;
    caps.setLayoutData(capsGd);

    supportToolCallingCheck = new Button(caps, SWT.CHECK);
    supportToolCallingCheck.setText(Messages.preferences_page_byok_addModel_supportToolCalling);
    supportToolCallingCheck.setSelection(true);
    supportVisionCheck = new Button(caps, SWT.CHECK);
    supportVisionCheck.setText(Messages.preferences_page_byok_addModel_supportVision);
    supportVisionCheck.setSelection(true);

    return container;
  }

  private void createAzureSpecificFields(Composite container) {
    // Deployment URL * (only for Azure)
    new Label(container, SWT.NONE).setText(Messages.preferences_page_byok_addModel_deploymentUrl);
    deploymentUrlText = new Text(container, SWT.BORDER);
    deploymentUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    deploymentUrlText.addModifyListener(this::onFieldChanged);

    // API Key *
    new Label(container, SWT.NONE).setText(Messages.preferences_page_byok_addModel_apiKey);
    Composite apiKeyRow = new Composite(container, SWT.NONE);
    GridLayout rowLayout = new GridLayout(2, false);
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    apiKeyRow.setLayout(rowLayout);
    apiKeyRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    apiKeyText = new Text(apiKeyRow, SWT.BORDER);
    apiKeyText.setEchoChar('*');
    apiKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    apiKeyText.addModifyListener(this::onFieldChanged);

    eyeOpenImg = UiUtils.buildImageFromPngPath("/icons/chat/eye.png");
    eyeClosedImg = UiUtils.buildImageFromPngPath("/icons/chat/eye_closed.png");

    toggleEyeBtn = new Button(apiKeyRow, SWT.FLAT);
    toggleEyeBtn.setImage(eyeClosedImg);
    toggleEyeBtn.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
    toggleEyeBtn.addListener(SWT.Selection, e -> togglePasswordVisibility());
  }

  /**
   * Get the appropriate help context ID based on the provider.
   */
  private String getHelpContextId() {
    return "com.microsoft.copilot.eclipse.ui.add_byok_model_dialog_azure";
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    addButton = createButton(parent, IDialogConstants.OK_ID, Messages.preferences_page_byok_dialog_add, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

    // Initially disable add button until required fields are filled
    addButton.setEnabled(false);
  }

  private void onFieldChanged(ModifyEvent e) {
    updateAddButtonState();
  }

  private void updateAddButtonState() {
    if (addButton != null && !addButton.isDisposed()) {
      boolean canAdd = isValidInput();
      addButton.setEnabled(canAdd);
    }
  }

  private boolean isValidInput() {
    // Model ID is always required
    if (StringUtils.isBlank(modelIdText.getText())) {
      return false;
    }

    if (providerName.equals(ByokModelProvider.AZURE.getDisplayName())) {
      if (deploymentUrlText == null || StringUtils.isBlank(deploymentUrlText.getText()) || apiKeyText == null
          || StringUtils.isBlank(apiKeyText.getText())) {
        return false;
      }
    }

    return true;
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

  @Override
  protected void okPressed() {
    if (onSave != null) {
      ByokModel model = buildModel();
      onSave.accept(model);
    }
    super.okPressed();
  }

  private ByokModel buildModel() {
    ByokModel model = new ByokModel();
    model.setModelId(modelIdText.getText().trim());
    model.setProviderName(providerName);
    model.setRegistered(true);
    model.setCustomModel(true);

    // Set provider-specific fields
    if (providerName.equals(ByokModelProvider.AZURE.getDisplayName()) && deploymentUrlText != null
        && apiKeyText != null) {
      model.setDeploymentUrl(deploymentUrlText.getText().trim());
      model.setApiKey(apiKeyText.getText().trim());
    }

    // Set capabilities
    ByokModelCapabilities capabilities = new ByokModelCapabilities();
    String displayedName = displayNameText.getText().trim().isEmpty() ? modelIdText.getText().trim()
        : displayNameText.getText().trim();
    capabilities.setName(displayedName);
    capabilities.setToolCalling(supportToolCallingCheck.getSelection());
    capabilities.setVision(supportVisionCheck.getSelection());
    model.setModelCapabilities(capabilities);

    return model;
  }

  @Override
  public boolean close() {
    // Dispose images
    if (eyeOpenImg != null && !eyeOpenImg.isDisposed()) {
      eyeOpenImg.dispose();
    }
    if (eyeClosedImg != null && !eyeClosedImg.isDisposed()) {
      eyeClosedImg.dispose();
    }
    return super.close();
  }
}

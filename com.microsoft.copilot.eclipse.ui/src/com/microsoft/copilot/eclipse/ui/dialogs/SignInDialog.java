package com.microsoft.copilot.eclipse.ui.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog for signing in to GitHub Copilot.
 */
public class SignInDialog extends MessageDialog {

  private final SignInInitiateResult signInInitiateResult;

  /**
   * Constructs a new SignInDialog.
   *
   * @param parentShell the parent shell
   * @param initResult the sign-in initiation result
   */
  public SignInDialog(Shell parentShell, SignInInitiateResult initResult) {
    super(parentShell, Messages.signInDialog_title, null, null, MessageDialog.INFORMATION,
        new String[] { Messages.signInDialog_button_cancel, Messages.signInDialog_button_copyOpen }, 1);
    this.signInInitiateResult = initResult;
  }

  @Override
  protected Control createCustomArea(Composite parent) {
    Composite composite = createComposite(parent);
    createDeviceCodeSection(composite);
    createWebsiteSection(composite);
    return composite;
  }

  private Composite createComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
    composite.setLayout(new GridLayout(2, false));
    return composite;
  }

  private void createDeviceCodeSection(Composite composite) {
    Label deviceCodeLabel = new Label(composite, SWT.NONE);
    deviceCodeLabel.setText(Messages.signInDialog_info_deviceCodePrefix);
    Text deviceCodeText = new Text(composite, SWT.SINGLE | SWT.READ_ONLY);
    deviceCodeText.setText(this.signInInitiateResult.getUserCode());
    deviceCodeText.setCursor(composite.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    deviceCodeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void createWebsiteSection(Composite composite) {
    Label websiteLabel = new Label(composite, SWT.NONE);
    websiteLabel.setText(Messages.signInDialog_info_gitHubWebSitePrefix);
    Link websiteLink = new Link(composite, SWT.NONE);
    websiteLink.setText("<a>" + this.signInInitiateResult.getVerificationUri() + "</a>");
    websiteLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        UiUtils.openLink(signInInitiateResult.getVerificationUri());
      }
    });
  }

  @Override
  protected Control createMessageArea(Composite composite) {
    this.message = Messages.signInDialog_info_instructions;
    return super.createMessageArea(composite);
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == 1) {
      copyCodeToClipboard();
    }
    super.buttonPressed(buttonId);
  }

  private void copyCodeToClipboard() {
    Clipboard clipboard = new Clipboard(SwtUtils.getDisplay());
    TextTransfer textTransfer = TextTransfer.getInstance();
    clipboard.setContents(new Object[] { this.signInInitiateResult.getUserCode() }, new Transfer[] { textTransfer });
    clipboard.dispose();
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.dialogs.jobs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Base class for Copilot dialogs with common UI elements.
 */
public abstract class BaseCopilotDialog extends Dialog {

  /**
   * Constructs a new BaseCopilotDialog.
   *
   * @param parentShell the parent shell
   */
  protected BaseCopilotDialog(Shell parentShell) {
    super(parentShell);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(getDialogTitle());
    loadCopilotIcon(newShell);
  }

  /**
   * Loads the Copilot icon for the dialog and sets up disposal listener.
   *
   * @param shell the shell to set the icon on
   */
  protected void loadCopilotIcon(Shell shell) {
    Image dialogTitleImage = UiUtils.buildImageFromPngPath("/icons/github_copilot.png");
    if (dialogTitleImage != null) {
      shell.setImage(dialogTitleImage);
    }
    shell.addDisposeListener(e -> {
      if (dialogTitleImage != null && !dialogTitleImage.isDisposed()) {
        dialogTitleImage.dispose();
      }
    });
  }

  /**
   * Creates a message composite with an information icon and message text.
   *
   * @param parent the parent composite
   * @param messageText the message text to display
   * @return the created message composite
   */
  protected Composite createMessageComposite(Composite parent, String messageText) {
    // Create a composite for icon and message
    Composite messageComposite = new Composite(parent, SWT.NONE);
    messageComposite.setLayout(new GridLayout(2, false));
    messageComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Create icon - centered vertically
    Label iconLabel = new Label(messageComposite, SWT.NONE);
    iconLabel.setImage(parent.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
    GridData iconData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    iconLabel.setLayoutData(iconData);

    // Create message label - centered vertically
    Label messageLabel = new Label(messageComposite, SWT.WRAP);
    messageLabel.setText(messageText);
    GridData messageData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    messageData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    messageLabel.setLayoutData(messageData);

    return messageComposite;
  }

  @Override
  protected Control createButtonBar(Composite parent) {
    String learnMoreUrl = getLearnMoreUrl();
    if (learnMoreUrl == null) {
      // If no learn more URL, use default button bar
      return super.createButtonBar(parent);
    }

    Composite buttonBar = new Composite(parent, SWT.NONE);
    buttonBar.setLayout(new GridLayout(2, false));
    buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Create "Learn More" link on the left
    Link learnMoreLink = new Link(buttonBar, SWT.NONE);
    learnMoreLink.setText(getLearnMoreLinkText());
    GridData linkData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    linkData.horizontalIndent = 10;
    learnMoreLink.setLayoutData(linkData);
    learnMoreLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        UiUtils.openLink(learnMoreUrl);
      }
    });

    // Create buttons on the right
    Composite buttonComposite = new Composite(buttonBar, SWT.NONE);
    buttonComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    buttonComposite.setLayout(new GridLayout());
    createButtonsForButtonBar(buttonComposite);

    return buttonBar;
  }

  /**
   * Gets the dialog title.
   *
   * @return the dialog title
   */
  protected abstract String getDialogTitle();

  /**
   * Gets the learn more URL. Return null if no learn more link should be displayed.
   *
   * @return the learn more URL or null
   */
  protected abstract String getLearnMoreUrl();

  /**
   * Gets the text for the learn more link.
   *
   * @return the learn more link text
   */
  protected abstract String getLearnMoreLinkText();
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.dialogs.jobs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.utils.WorkspaceUtils;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.dialogs.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;

/**
 * Dialog to inform users about GitHub Coding Agent.
 */
public class GitHubCodingAgentDialog extends BaseCopilotDialog {

  private Button dontAskAgainCheckbox;

  /**
   * Constructs a new GitHubCodingAgentDialog.
   *
   * @param parentShell the parent shell
   */
  public GitHubCodingAgentDialog(Shell parentShell) {
    super(parentShell);
  }

  @Override
  protected String getDialogTitle() {
    return Messages.githubCodingAgentDialog_title;
  }

  @Override
  protected String getLearnMoreUrl() {
    return UiConstants.GITHUB_COPILOT_CODING_AGENT_LEARN_MORE_URL;
  }

  @Override
  protected String getLearnMoreLinkText() {
    return Messages.githubCodingAgent_link_learnMore;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);

    // Create a composite for icon and message
    Composite messageComposite = new Composite(container, SWT.NONE);
    messageComposite.setLayout(new GridLayout(2, false));
    messageComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Create icon
    Label iconLabel = new Label(messageComposite, SWT.NONE);
    iconLabel.setImage(parent.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
    GridData iconData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    iconLabel.setLayoutData(iconData);

    // Get the project name
    String projectName = WorkspaceUtils.listTopLevelProjectsWithGitRepository().get(0).getName();
    String fullMessage = NLS.bind(Messages.githubCodingAgentDialog_info_description, projectName);

    // Create message using StyledText to support bold formatting
    StyledText messageText = new StyledText(messageComposite, SWT.WRAP | SWT.READ_ONLY);
    messageText.setText(fullMessage);
    messageText.setBackground(parent.getBackground());
    messageText.setCaret(null); // Hide caret
    GridData messageData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    messageData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    messageText.setLayoutData(messageData);

    // Find the project name in the message and apply bold style
    int startIndex = fullMessage.indexOf(projectName);
    if (startIndex != -1) {
      StyleRange boldStyle = new StyleRange();
      boldStyle.start = startIndex;
      boldStyle.length = projectName.length();
      boldStyle.fontStyle = SWT.BOLD;
      messageText.setStyleRange(boldStyle);
    }

    // Calculate the left margin based on the icon width and spacing
    int iconWidth = parent.getDisplay().getSystemImage(SWT.ICON_INFORMATION).getBounds().width;
    int horizontalSpacing = ((GridLayout) messageComposite.getLayout()).horizontalSpacing;
    int leftMargin = iconWidth + horizontalSpacing;

    // Create "Don't ask again" checkbox below the message
    dontAskAgainCheckbox = new Button(container, SWT.CHECK);
    dontAskAgainCheckbox.setText(Messages.githubCodingAgentDialog_checkbox_dontAskAgain);
    GridData checkboxData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    checkboxData.horizontalIndent = leftMargin; // Indent to align with message text
    dontAskAgainCheckbox.setLayoutData(checkboxData);

    return container;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    // Create Continue button (default)
    Button continueButton = createButton(parent, OK, Messages.githubCodingAgentDialog_button_continue, true);
    continueButton.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");
    continueButton.setFocus();

    // Create Cancel button
    createButton(parent, CANCEL, Messages.githubCodingAgentDialog_button_cancel, false);
  }

  @Override
  protected void okPressed() {
    // Save the "don't ask again" to user preference if the checkbox is selected
    if (dontAskAgainCheckbox != null && dontAskAgainCheckbox.getSelection()) {
      ChatServiceManager chatServiceManager = (ChatServiceManager) CopilotCore.getPlugin().getChatServiceManager();
      if (chatServiceManager != null) {
        UserPreferenceService userPreferenceService = chatServiceManager.getUserPreferenceService();
        if (userPreferenceService != null) {
          userPreferenceService.setSkipGitHubJobConfirmDialog(true);
        }
      }
    }
    super.okPressed();
  }

  /**
   * Opens the dialog and returns true if the user clicked Continue, false otherwise.
   *
   * @param parentShell the parent shell
   * @return true if the user clicked Continue or false if Cancel
   */
  public static boolean open(Shell parentShell) {
    GitHubCodingAgentDialog dialog = new GitHubCodingAgentDialog(parentShell);
    return dialog.open() == OK;
  }
}

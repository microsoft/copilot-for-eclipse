package com.microsoft.copilot.eclipse.ui.chat.viewers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.WrapLabel;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A widget that displays a initial chat introduction.
 */
public class BeforeLoginWelcomeViewer extends BaseViewer {
  private Image mainIcon;
  private Font mainLabelFont;
  private Image codeIcon;
  private Image chatIcon;
  private Image agentIcon;
  private Image mcpIcon;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public BeforeLoginWelcomeViewer(Composite parent, int style) {
    super(parent, style);

    GridLayout layout = new GridLayout(1, true);
    layout.verticalSpacing = ALIGNED_MARGIN_TOP * 2;
    setLayout(layout);
    GridData gridData = new GridData(SWT.CENTER, SWT.FILL, true, true);
    setLayoutData(gridData);
    buildMainIconAndLabel();
    buildSubComposite();
    buildCopilotForFreeLinkAndSignInButton();
    buildFooterLinks();
  }

  private void buildMainIconAndLabel() {
    Composite iconLabelComposite = new Composite(this, SWT.NONE);
    GridLayout iconLabelGridlayout = new GridLayout(1, true);
    iconLabelGridlayout.verticalSpacing = ALIGNED_MARGIN_TOP;
    iconLabelComposite.setLayout(iconLabelGridlayout);
    iconLabelComposite.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

    this.mainIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_welcome.png");
    Label icon = new Label(iconLabelComposite, SWT.CENTER);
    icon.setImage(mainIcon);
    icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));

    WrapLabel label = new WrapLabel(iconLabelComposite, SWT.CENTER);
    label.setText(Messages.chat_welcomeView_title);
    label.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
    FontData fontData = new FontData();
    fontData.setHeight(ALIGNED_TITLE_HEIGHT);
    fontData.setStyle(SWT.BOLD);
    if (this.mainLabelFont != null && !this.mainLabelFont.isDisposed()) {
      this.mainLabelFont.dispose();
    }
    this.mainLabelFont = new Font(Display.getCurrent(), fontData);
    label.setFont(mainLabelFont);

    WrapLabel subLabel = new WrapLabel(iconLabelComposite, SWT.CENTER);
    subLabel.setText(Messages.chat_welcomeView_description);
    subLabel.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
  }

  private void buildSubComposite() {
    Composite subComposite = new Composite(this, SWT.NONE);
    GridData gridData = new GridData(SWT.CENTER, SWT.FILL, false, true);
    subComposite.setLayoutData(gridData);
    subComposite.setLayout(new GridLayout(1, true));

    if (this.agentIcon != null && !this.agentIcon.isDisposed()) {
      this.agentIcon.dispose();
    }
    this.agentIcon = UiUtils.buildImageFromPngPath("/icons/github_copilot.png");
    buildLabelWithIcon(subComposite, agentIcon, Messages.chat_welcomeView_agentSuffix);

    if (this.mcpIcon != null && !this.mcpIcon.isDisposed()) {
      this.mcpIcon.dispose();
    }
    this.mcpIcon = UiUtils.buildImageFromPngPath("/icons/chat/tools.png");
    buildLabelWithIcon(subComposite, mcpIcon, Messages.chat_welcomeView_mcpSuffix);

    if (this.chatIcon != null && !this.chatIcon.isDisposed()) {
      this.chatIcon.dispose();
    }
    this.chatIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_chat.png");
    buildLabelWithIcon(subComposite, chatIcon, Messages.chat_welcomeView_chatSuffix);

    if (this.codeIcon != null && !this.codeIcon.isDisposed()) {
      this.codeIcon.dispose();
    }
    this.codeIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_code.png");
    buildLabelWithIcon(subComposite, codeIcon, Messages.chat_welcomeView_completionSuffix);
  }

  private void buildCopilotForFreeLinkAndSignInButton() {
    Composite mainComposite = new Composite(this, SWT.NONE);
    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.verticalSpacing = ALIGNED_MARGIN_TOP;
    mainComposite.setLayout(gridLayout);
    mainComposite.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, true));

    // Copilot for free link
    StringBuilder linkText = new StringBuilder();
    linkText.append(Messages.chat_welcomeView_freeCopilotIntroPrefix).append(Messages.chat_welcomeView_freeCopilotLink)
        .append(Messages.chat_welcomeView_freeCopilotIntroSuffix);
    buildTextWithLinkAndListener(mainComposite, linkText.toString(), null, event -> {
      UiUtils.openLink(Messages.chat_welcomeView_freeCopilotLink);
    });

    // Sign-in button
    Button signInButton = new Button(mainComposite, SWT.PUSH);
    signInButton.setText(Messages.chat_welcomeView_signInButton);
    signInButton.setToolTipText(Messages.chat_welcomeView_signInButton_Tooltip);
    GridData buttonGridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
    buttonGridData.widthHint = 200;
    signInButton.setLayoutData(buttonGridData);
    signInButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
        IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
        try {
          handlerService.executeCommand("com.microsoft.copilot.eclipse.commands.signIn", null);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void buildFooterLinks() {
    GridLayout footerLayout = new GridLayout(1, false);
    footerLayout.marginTop = ALIGNED_MARGIN_TOP * 2;
    footerLayout.marginLeft = 50;
    footerLayout.marginRight = 50;
    Composite footerComposite = new Composite(this, SWT.NONE);
    footerComposite.setLayout(footerLayout);

    GridData footerGridData = new GridData(SWT.CENTER, SWT.BOTTOM, true, false);
    footerComposite.setLayoutData(footerGridData);

    // Terms and Privacy links
    StringBuilder termsAndPrivacyText = new StringBuilder();
    termsAndPrivacyText.append(Messages.chat_welcomeView_termsPrefix).append(Messages.chat_welcomeView_termsLink)
        .append(Messages.chat_welcomeView_termsSuffix).append(Messages.chat_welcomeView_privacyPolicyPrefix)
        .append(Messages.chat_welcomeView_privacyPolicyLink).append(Messages.chat_welcomeView_privacyPolicySuffix);
    buildTextWithLinkAndListener(footerComposite, termsAndPrivacyText.toString(), null, event -> {
      if (event.text.equals(Messages.chat_welcomeView_termsLink)) {
        UiUtils.openLink(Messages.chat_welcomeView_termsLink);
      } else {
        UiUtils.openLink(Messages.chat_welcomeView_privacyPolicyLink);
      }
    });

    // Public Code and Settings links
    StringBuilder publicCodeAndSettingsLink = new StringBuilder();
    publicCodeAndSettingsLink.append(Messages.chat_welcomeView_footerPublicCodePrefix)
        .append(Messages.chat_welcomeView_footerPublicCodeLink).append(Messages.chat_welcomeView_footerPublicCodeSuffix)
        .append(Messages.chat_welcomeView_footerSettingsPrefix).append(Messages.chat_welcomeView_footerSettingsLink)
        .append(Messages.chat_welcomeView_footerSettingsSuffix);
    buildTextWithLinkAndListener(footerComposite, publicCodeAndSettingsLink.toString(), null, event -> {
      if (event.text.equals(Messages.chat_welcomeView_footerPublicCodeLink)) {
        UiUtils.openLink(Messages.chat_welcomeView_footerPublicCodeLink);
      } else {
        UiUtils.openLink(Messages.chat_welcomeView_footerSettingsLink);
      }
    });
  }

  @Override
  public void dispose() {
    if (this.mainIcon != null) {
      this.mainIcon.dispose();
    }
    if (this.mainLabelFont != null) {
      this.mainLabelFont.dispose();
    }
    if (this.codeIcon != null) {
      this.codeIcon.dispose();
    }
    if (this.chatIcon != null) {
      this.chatIcon.dispose();
    }
    super.dispose();
  }
}
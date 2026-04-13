// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.viewers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
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

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
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

  private Label copilotIconLabel;
  private WrapLabel welcomeSubLabel;
  private Composite subComposite;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public BeforeLoginWelcomeViewer(Composite parent, int style) {
    super(parent, style);

    GridLayout layout = new GridLayout(1, true);
    setLayout(layout);
    GridData gridData = new GridData(SWT.CENTER, SWT.FILL, true, true);
    setLayoutData(gridData);

    buildEmptyComposite();

    Composite mainComposite = new Composite(this, SWT.NONE);
    GridLayout mainLayout = new GridLayout(1, true);
    mainLayout.verticalSpacing = ALIGNED_MARGIN_TOP;
    mainComposite.setLayout(mainLayout);
    mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    buildMainIconAndLabel(mainComposite);
    buildSubComposite(mainComposite);
    buildCopilotForFreeLinkAndSignInButton(mainComposite);

    buildEmptyComposite();

    buildFooterLinks();

    handleResize(parent); // Call resize for the initial layout to prepare appropriate layout after view switch.

    addControlListener(ControlListener.controlResizedAdapter(e -> handleResize(parent)));
  }

  private void handleResize(Composite listenedComposite) {
    int height = listenedComposite.getBounds().height;

    boolean shouldShowNarrowView = height >= SHOW_NARROW_VIEW_HEIGHT_THRESHOLD;
    if (this.subComposite != null && !this.subComposite.isDisposed()
        && this.subComposite.getVisible() != shouldShowNarrowView) {
      this.subComposite.setVisible(shouldShowNarrowView);
      ((GridData) this.subComposite.getLayoutData()).exclude = !shouldShowNarrowView;
      this.subComposite.requestLayout();
    }

    if (this.welcomeSubLabel != null && !this.welcomeSubLabel.isDisposed()
        && this.welcomeSubLabel.getVisible() != shouldShowNarrowView) {
      this.welcomeSubLabel.setVisible(shouldShowNarrowView);
      ((GridData) this.welcomeSubLabel.getLayoutData()).exclude = !shouldShowNarrowView;
      this.welcomeSubLabel.requestLayout();
    }

    if (this.copilotIconLabel != null && !this.copilotIconLabel.isDisposed()
        && this.copilotIconLabel.getVisible() != shouldShowNarrowView) {
      this.copilotIconLabel.setVisible(shouldShowNarrowView);
      ((GridData) this.copilotIconLabel.getLayoutData()).exclude = !shouldShowNarrowView;
      this.copilotIconLabel.requestLayout();
    }
  }

  private void buildEmptyComposite() {
    Composite emptyComposite = new Composite(this, SWT.NONE);
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    emptyComposite.setLayoutData(gridData);
    emptyComposite.setLayout(new GridLayout(1, true));
  }

  private void buildMainIconAndLabel(Composite parent) {
    Composite iconLabelComposite = new Composite(parent, SWT.NONE);
    GridLayout iconLabelGridlayout = new GridLayout(1, true);
    iconLabelGridlayout.verticalSpacing = ALIGNED_MARGIN_TOP;
    iconLabelComposite.setLayout(iconLabelGridlayout);
    iconLabelComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    this.mainIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_welcome.png");
    this.copilotIconLabel = new Label(iconLabelComposite, SWT.CENTER);
    this.copilotIconLabel.setImage(mainIcon);
    this.copilotIconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));

    WrapLabel label = new WrapLabel(iconLabelComposite, SWT.CENTER);
    label.setText(Messages.chat_welcomeView_title);
    FontData fontData = new FontData();
    fontData.setHeight(ALIGNED_TITLE_HEIGHT);
    fontData.setStyle(SWT.BOLD);
    if (this.mainLabelFont != null && !this.mainLabelFont.isDisposed()) {
      this.mainLabelFont.dispose();
    }
    this.mainLabelFont = new Font(Display.getCurrent(), fontData);
    label.setFont(mainLabelFont);

    this.welcomeSubLabel = new WrapLabel(iconLabelComposite, SWT.CENTER);
    this.welcomeSubLabel.setText(Messages.chat_welcomeView_description);
  }

  private void buildSubComposite(Composite parent) {
    subComposite = new Composite(parent, SWT.NONE);
    GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
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

  private void buildCopilotForFreeLinkAndSignInButton(Composite parent) {
    Composite linkAndButtonComposite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.verticalSpacing = ALIGNED_MARGIN_TOP;
    linkAndButtonComposite.setLayout(gridLayout);
    GridData mainGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    mainGridData.minimumHeight = 70;
    linkAndButtonComposite.setLayoutData(mainGridData);

    // Copilot for free link
    StringBuilder linkText = new StringBuilder();
    linkText.append(Messages.chat_welcomeView_freeCopilotIntroPrefix).append(Messages.chat_welcomeView_freeCopilotLink)
        .append(Messages.chat_welcomeView_freeCopilotIntroSuffix);
    buildTextWithLinkAndListener(linkAndButtonComposite, linkText.toString(), null, event -> {
      UiUtils.openLink(Messages.chat_welcomeView_freeCopilotLink);
    });

    // Sign-in button
    Button signInButton = new Button(linkAndButtonComposite, SWT.PUSH);
    signInButton.setText(Messages.chat_welcomeView_signInButton);
    signInButton.setToolTipText(Messages.chat_welcomeView_signInButton_Tooltip);
    GridData buttonGridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
    buttonGridData.widthHint = 200;
    signInButton.setLayoutData(buttonGridData);
    signInButton.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");
    signInButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
        IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
        try {
          handlerService.executeCommand("com.microsoft.copilot.eclipse.commands.signIn", null);
        } catch (Exception e) {
          CopilotCore.LOGGER.error("Error executing sign-in command", e);
        }
      }
    });
  }

  private void buildFooterLinks() {
    GridLayout footerLayout = new GridLayout(1, false);
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
    buildTextWithLinkAndListener(footerComposite, termsAndPrivacyText.toString(),
        new GridData(SWT.LEFT, SWT.CENTER, true, true), event -> {
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
    if (this.agentIcon != null) {
      this.agentIcon.dispose();
    }
    if (this.mcpIcon != null) {
      this.mcpIcon.dispose();
    }
    super.dispose();
  }
}
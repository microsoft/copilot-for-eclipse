package com.microsoft.copilot.eclipse.ui.chat.viewers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
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

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public BeforeLoginWelcomeViewer(Composite parent, int style) {
    super(parent, style);

    buildMainIconAndLabel();
    buildSubComposite();
    buildCopilotForFreeLink();
    buildSignInButton();
    buildTermsAndPrivacyLink();
    buildPublicCodeAndSettingsLink();
  }

  private void buildMainIconAndLabel() {
    Composite iconLabelComposite = new Composite(this, SWT.NONE);
    GridLayout iconLabelGridlayout = new GridLayout(1, true);
    iconLabelComposite.setLayout(iconLabelGridlayout);
    iconLabelComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    this.mainIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_welcome.png");
    Label icon = new Label(iconLabelComposite, SWT.NONE);
    icon.setImage(mainIcon);
    icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

    WrapLabel label = new WrapLabel(iconLabelComposite, SWT.CENTER | SWT.WRAP);
    label.setText(Messages.chat_welcomeView_title);
    label.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
    FontData fontData = new FontData();
    fontData.setHeight(16);
    fontData.setStyle(SWT.BOLD);
    if (this.mainLabelFont != null && !this.mainLabelFont.isDisposed()) {
      this.mainLabelFont.dispose();
    }
    this.mainLabelFont = new Font(Display.getCurrent(), fontData);
    label.setFont(mainLabelFont);

    WrapLabel subLabel = new WrapLabel(iconLabelComposite, SWT.CENTER | SWT.WRAP);
    subLabel.setText(Messages.chat_welcomeView_description);
    subLabel.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
  }

  private void buildSubComposite() {
    Composite composite = new Composite(this, SWT.BORDER);
    GridData gridData = new GridData(SWT.CENTER, SWT.FILL, true, true);
    gridData.widthHint = ALIGNED_WIDTH;
    composite.setLayoutData(gridData);
    composite.setLayout(new GridLayout(1, true));

    if (this.codeIcon != null && !this.codeIcon.isDisposed()) {
      this.codeIcon.dispose();
    }
    this.codeIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_code.png");
    buildRawLayoutLabelWithIcon(composite, Messages.chat_welcomeView_completionSuffix, codeIcon);

    if (this.chatIcon != null && !this.chatIcon.isDisposed()) {
      this.chatIcon.dispose();
    }
    this.chatIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_chat.png");
    buildRawLayoutLabelWithIcon(composite, Messages.chat_welcomeView_chatSuffix, chatIcon);

    // Calculate the left and right margin for the border to align with the sign in button's size.
    composite.pack();
    updateMargins(composite);

    composite.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        updateMargins(composite);
      }
    });
  }

  private void buildRawLayoutLabelWithIcon(Composite parent, String text, Image labelIcon) {
    Composite composite = new Composite(parent, SWT.NONE);
    RowLayout rowLayout = new RowLayout();
    rowLayout.center = true;
    rowLayout.pack = true;
    composite.setLayout(rowLayout);

    Label icon = new Label(composite, SWT.PUSH);
    icon.setImage(labelIcon);

    Label label = new Label(composite, SWT.PUSH);
    label.setLayoutData(new RowData(SWT.DEFAULT, SWT.DEFAULT));
    label.setText(text);
    label.setForeground((this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY)));

    parent.layout();
    composite.pack();
  }

  private void updateMargins(Composite composite) {
    int maxChildrenWidth = 0;

    for (Control child : composite.getChildren()) {
      maxChildrenWidth = Math.max(child.getSize().x, maxChildrenWidth);
    }

    int labelWithIconMargin = (composite.getSize().x - maxChildrenWidth) / 2;
    if (composite.getLayout() instanceof GridLayout) {
      GridLayout compositelayout = (GridLayout) composite.getLayout();
      compositelayout.marginTop = 10;
      compositelayout.marginLeft = labelWithIconMargin;
      compositelayout.marginRight = labelWithIconMargin;
      composite.setLayout(compositelayout);
    }
  }

  private void buildCopilotForFreeLink() {
    Composite copilotFreeComposite = buildCompositeWithMarginTop(this, ALIGNED_MARGIN_TOP, SWT.NONE);
    StringBuilder linkText = new StringBuilder();
    linkText.append(Messages.chat_welcomeView_freeCopilotIntroPrefix).append(Messages.chat_welcomeView_freeCopilotLink)
        .append(Messages.chat_welcomeView_freeCopilotIntroSuffix);
    buildTextWithLinkAndListener(copilotFreeComposite, linkText.toString(), event -> {
      UiUtils.openLink(Messages.chat_welcomeView_freeCopilotLink);
    });
  }

  private void buildSignInButton() {
    Composite signInComposite = buildCompositeWithMarginTop(this, ALIGNED_MARGIN_TOP, SWT.NONE);
    Button signInButton = new Button(signInComposite, SWT.PUSH | SWT.WRAP);
    signInButton.setText(Messages.chat_welcomeView_signInButton);
    signInButton.setToolTipText(Messages.chat_welcomeView_signInButton_Tooltip);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
    gridData.widthHint = ALIGNED_WIDTH;
    signInButton.setLayoutData(gridData);
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

  private void buildTermsAndPrivacyLink() {
    Composite termsAndPrivacyComposite = buildCompositeWithMarginTop(this, ALIGNED_MARGIN_TOP, SWT.NONE);
    StringBuilder linkText = new StringBuilder();
    linkText.append(Messages.chat_welcomeView_termsPrefix).append(Messages.chat_welcomeView_termsLink)
        .append(Messages.chat_welcomeView_termsSuffix).append(Messages.chat_welcomeView_privacyPolicyPrefix)
        .append(Messages.chat_welcomeView_privacyPolicyLink).append(Messages.chat_welcomeView_privacyPolicySuffix);
    buildTextWithLinkAndListener(termsAndPrivacyComposite, linkText.toString(), event -> {
      if (event.text.equals(Messages.chat_welcomeView_termsLink)) {
        UiUtils.openLink(Messages.chat_welcomeView_termsLink);
      } else {
        UiUtils.openLink(Messages.chat_welcomeView_privacyPolicyLink);
      }
    });
  }

  private void buildPublicCodeAndSettingsLink() {
    Composite publicCodeAndSettingsComposite = buildCompositeWithMarginTop(this, ALIGNED_MARGIN_TOP, SWT.NONE);
    StringBuilder linkText = new StringBuilder();
    linkText.append(Messages.chat_welcomeView_footerPublicCodePrefix)
        .append(Messages.chat_welcomeView_footerPublicCodeLink).append(Messages.chat_welcomeView_footerPublicCodeSuffix)
        .append(Messages.chat_welcomeView_footerSettingsPrefix).append(Messages.chat_welcomeView_footerSettingsLink)
        .append(Messages.chat_welcomeView_footerSettingsSuffix);

    buildTextWithLinkAndListener(publicCodeAndSettingsComposite, linkText.toString(), event -> {
      if (event.text.equals(Messages.chat_welcomeView_footerPublicCodeLink)) {
        UiUtils.openLink(Messages.chat_welcomeView_footerPublicCodeLink);
      } else {
        UiUtils.openLink(Messages.chat_welcomeView_footerSettingsLink);
      }
    });
  }

  private void buildTextWithLinkAndListener(Composite parent, String text, Listener listener) {
    Link link = new Link(parent, SWT.CENTER);
    link.setText(text);
    link.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
    link.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
    link.addListener(SWT.Selection, listener);
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
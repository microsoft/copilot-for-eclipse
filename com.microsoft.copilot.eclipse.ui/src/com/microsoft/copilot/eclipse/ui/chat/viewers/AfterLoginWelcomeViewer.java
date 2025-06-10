package com.microsoft.copilot.eclipse.ui.chat.viewers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.WrapLabel;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A widget that displays a initial chat introduction.
 */
public class AfterLoginWelcomeViewer extends BaseViewer {
  private Image mainIcon;
  private Font mainLabelFont;
  private Image attachContextIcon;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public AfterLoginWelcomeViewer(Composite parent, int style) {
    super(parent, style);

    buildMainIconAndLabel();
    buildSubComposite();
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
    label.setText(Messages.chat_initialChatView_title);
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
    subLabel.setText(Messages.chat_aiWarn_description);
    subLabel.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
    GridData subLabelGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    subLabelGridData.widthHint = ALIGNED_LABEL_WIDTH;
    subLabelGridData.verticalIndent = 10;
    subLabel.setLayoutData(subLabelGridData);

  }

  private void buildSubComposite() {
    Composite subComposite = new Composite(this, SWT.NONE);
    GridLayout subCompositelayout = new GridLayout(1, true);
    subComposite.setLayout(subCompositelayout);
    GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    gridData.widthHint = ALIGNED_WIDTH;
    subComposite.setLayoutData(gridData);

    this.attachContextIcon = UiUtils.buildImageFromPngPath("/icons/chat/attach_context.png");
    buildLabelWithIcon(subComposite, this.attachContextIcon, Messages.chat_initialChatView_attactContextSuffix);

    WrapLabel subLabel = new WrapLabel(subComposite, SWT.CENTER);
    subLabel.setText(Messages.chat_initialChatView_useCommandsIntro);
    subLabel.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
  }

  @Override
  public void dispose() {
    if (this.attachContextIcon != null) {
      this.attachContextIcon.dispose();
    }
    if (this.mainIcon != null) {
      this.mainIcon.dispose();
    }
    if (this.mainLabelFont != null) {
      this.mainLabelFont.dispose();
    }
    super.dispose();
  }
}

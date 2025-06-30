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
public class AgentModeViewer extends BaseViewer {
  private Image mainIcon;
  private Font mainLabelFont;
  private Image attachContextIcon;
  private Image configureMcpIcon;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public AgentModeViewer(Composite parent, int style) {
    super(parent, style);

    GridLayout layout = new GridLayout(1, true);
    layout.verticalSpacing = ALIGNED_MARGIN_TOP * 2;
    setLayout(layout);
    buildMainIconAndLabel();
    buildSubComposite();
  }

  private void buildMainIconAndLabel() {
    Composite iconLabelComposite = new Composite(this, SWT.NONE);
    GridLayout iconLabelGridlayout = new GridLayout(1, true);
    iconLabelGridlayout.verticalSpacing = ALIGNED_MARGIN_TOP;
    iconLabelComposite.setLayout(iconLabelGridlayout);
    iconLabelComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    this.mainIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_welcome.png");
    Label icon = new Label(iconLabelComposite, SWT.CENTER);
    icon.addDisposeListener(e -> {
      if (this.mainIcon != null && !this.mainIcon.isDisposed()) {
        this.mainIcon.dispose();
      }
    });
    icon.setImage(mainIcon);
    icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

    WrapLabel label = new WrapLabel(iconLabelComposite, SWT.CENTER);
    label.setText(Messages.chat_agentModeView_title);
    label.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
    FontData fontData = new FontData();
    fontData.setHeight(ALIGNED_TITLE_HEIGHT);
    fontData.setStyle(SWT.BOLD);
    if (this.mainLabelFont != null && !this.mainLabelFont.isDisposed()) {
      this.mainLabelFont.dispose();
    }
    this.mainLabelFont = new Font(Display.getCurrent(), fontData);
    label.setFont(mainLabelFont);
    label.addDisposeListener(e -> {
      if (this.mainLabelFont != null && !this.mainLabelFont.isDisposed()) {
        this.mainLabelFont.dispose();
      }
    });

    Composite lineAndIntroComposite = new Composite(iconLabelComposite, SWT.NONE);
    GridLayout lineAndIntroLayout = new GridLayout(1, true);
    lineAndIntroLayout.verticalSpacing = 0;
    lineAndIntroComposite.setLayout(lineAndIntroLayout);
    GridData lineAndIntroGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    lineAndIntroComposite.setLayoutData(lineAndIntroGridData);

    WrapLabel introLabel = new WrapLabel(lineAndIntroComposite, SWT.CENTER);
    introLabel.setText(Messages.chat_agentModeView_agentModeIntro);
    introLabel.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
    GridData introGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    introGridData.widthHint = ALIGNED_LABEL_WIDTH;
    introLabel.setLayoutData(introGridData);

    WrapLabel warnLabel = new WrapLabel(iconLabelComposite, SWT.CENTER);
    warnLabel.setText(Messages.chat_aiWarn_description);
    warnLabel.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
    GridData warnGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    warnGridData.widthHint = ALIGNED_LABEL_WIDTH;
    warnLabel.setLayoutData(warnGridData);
  }

  private void buildSubComposite() {
    Composite subComposite = new Composite(this, SWT.NONE);
    GridLayout subCompositelayout = new GridLayout(1, true);
    subComposite.setLayout(subCompositelayout);
    GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    gridData.widthHint = ALIGNED_WIDTH;
    subComposite.setLayoutData(gridData);

    // configure MCP icon with label
    this.configureMcpIcon = UiUtils.buildImageFromPngPath("/icons/chat/tools.png");
    subComposite.addDisposeListener(e -> {
      if (this.configureMcpIcon != null && !this.configureMcpIcon.isDisposed()) {
        this.configureMcpIcon.dispose();
      }
    });
    buildLabelWithIcon(subComposite, this.configureMcpIcon, Messages.chat_agentModeView_configureMcpSuffix);

    // attach context icon with label
    this.attachContextIcon = UiUtils.buildImageFromPngPath("/icons/chat/attach_context.png");
    subComposite.addDisposeListener(e -> {
      if (this.attachContextIcon != null && !this.attachContextIcon.isDisposed()) {
        this.attachContextIcon.dispose();
      }
    });
    buildLabelWithIcon(subComposite, this.attachContextIcon, Messages.chat_agentModeView_attachContextSuffix);
  }
}

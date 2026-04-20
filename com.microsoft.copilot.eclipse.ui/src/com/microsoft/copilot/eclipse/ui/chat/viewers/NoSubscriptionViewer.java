// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

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

import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.WrapLabel;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A widget that displays a no Copilot subscription info.
 */
public class NoSubscriptionViewer extends BaseViewer {
  private Image mainIcon;
  private Font mainLabelFont;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public NoSubscriptionViewer(Composite parent, int style) {
    super(parent, style);

    GridLayout layout = new GridLayout(1, true);
    layout.verticalSpacing = ALIGNED_MARGIN_TOP * 2;
    setLayout(layout);
    buildMainIconAndLabel();
    buildcheckSubButton();
  }

  private void buildMainIconAndLabel() {
    Composite iconLabelComposite = new Composite(this, SWT.NONE);
    GridLayout iconLabelGridlayout = new GridLayout(1, true);
    iconLabelGridlayout.verticalSpacing = ALIGNED_MARGIN_TOP;
    iconLabelComposite.setLayout(iconLabelGridlayout);
    iconLabelComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    this.mainIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_not_authorized.png");
    Label icon = new Label(iconLabelComposite, SWT.NONE);
    icon.setImage(mainIcon);
    icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

    WrapLabel label = new WrapLabel(iconLabelComposite, SWT.CENTER);
    label.setText(Messages.chat_noAuthView_title);
    FontData fontData = new FontData();
    fontData.setHeight(ALIGNED_TITLE_HEIGHT);
    fontData.setStyle(SWT.BOLD);
    if (this.mainLabelFont != null && !this.mainLabelFont.isDisposed()) {
      this.mainLabelFont.dispose();
    }
    this.mainLabelFont = new Font(Display.getCurrent(), fontData);
    label.setFont(mainLabelFont);

    WrapLabel subLabel = new WrapLabel(iconLabelComposite, SWT.CENTER);
    subLabel.setText(Messages.chat_noAuthView_description);
  }

  private void buildcheckSubButton() {
    Composite checkSubComposite = buildCompositeWithMarginTop(this, 10, SWT.NONE);

    Button checkSubButton = new Button(checkSubComposite, SWT.PUSH);
    checkSubButton.setText(Messages.chat_noAuthView_checkSubButton);
    checkSubButton.setToolTipText(Messages.chat_noAuthView_checkSubButton_Tooltip);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gridData.widthHint = ALIGNED_WIDTH;
    checkSubButton.setLayoutData(gridData);
    checkSubButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
        UiUtils.openLink(Messages.chat_noAuthView_checkSubLink);
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
    super.dispose();
  }
}

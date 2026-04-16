// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

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
 * A widget that displays a loading view.
 */
public class LoadingViewer extends BaseViewer {

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public LoadingViewer(Composite parent, int style) {
    super(parent, style);

    GridLayout layout = new GridLayout(1, true);
    layout.verticalSpacing = ALIGNED_MARGIN_TOP * 2;
    setLayout(layout);
    buildMainIconAndLabel();
  }

  private void buildMainIconAndLabel() {
    Composite iconLabelComposite = new Composite(this, SWT.NONE);
    GridLayout iconLabelGridlayout = new GridLayout(1, true);
    iconLabelGridlayout.verticalSpacing = ALIGNED_MARGIN_TOP;
    iconLabelComposite.setLayout(iconLabelGridlayout);
    iconLabelComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    Image mainIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_loading.png");
    Label icon = new Label(iconLabelComposite, SWT.CENTER);
    icon.setImage(mainIcon);
    icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

    WrapLabel label = new WrapLabel(iconLabelComposite, SWT.CENTER);
    label.setText(Messages.chat_loadingView_title);
    FontData fontData = new FontData();
    fontData.setHeight(ALIGNED_TITLE_HEIGHT);
    fontData.setStyle(SWT.BOLD);
    Font mainLabelFont = new Font(Display.getCurrent(), fontData);
    label.setFont(mainLabelFont);

    WrapLabel subLabel = new WrapLabel(iconLabelComposite, SWT.CENTER);
    subLabel.setText(Messages.chat_loadingView_description);
    subLabel.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

    this.addDisposeListener(e -> {
      if (mainIcon != null && !mainIcon.isDisposed()) {
        mainIcon.dispose();
      }
      if (mainLabelFont != null && !mainLabelFont.isDisposed()) {
        mainLabelFont.dispose();
      }
    });
  }
}

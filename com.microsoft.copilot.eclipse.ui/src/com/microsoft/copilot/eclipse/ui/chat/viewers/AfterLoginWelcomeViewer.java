// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.viewers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
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
  private Font mainLabelFont;

  private Label copilotIconLabel;
  private Composite subComposite;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public AfterLoginWelcomeViewer(Composite parent, int style) {
    super(parent, style);

    GridLayout layout = new GridLayout(1, true);
    layout.verticalSpacing = ALIGNED_MARGIN_TOP * 2;
    setLayout(layout);
    buildMainIconAndLabel();
    buildSubComposite();
    buildInstructions();

    if (parent != null && !parent.isDisposed()) {
      Composite mainComposite = parent.getParent();
      if (mainComposite != null && !mainComposite.isDisposed()) {
        Composite contentWrapper = mainComposite.getParent();
        if (contentWrapper != null && !contentWrapper.isDisposed()) {
          Composite chatView = contentWrapper.getParent();
          handleResize(chatView); // Call resize for the initial layout to prepare appropriate layout after view switch.
          chatView.addControlListener(ControlListener.controlResizedAdapter(e -> handleResize(chatView)));
        }
      }
    }
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

    if (this.copilotIconLabel != null && !this.copilotIconLabel.isDisposed()
        && this.copilotIconLabel.getVisible() != shouldShowNarrowView) {
      this.copilotIconLabel.setVisible(shouldShowNarrowView);
      ((GridData) this.copilotIconLabel.getLayoutData()).exclude = !shouldShowNarrowView;
      this.copilotIconLabel.requestLayout();
    }
  }

  private void buildMainIconAndLabel() {
    Composite iconLabelComposite = new Composite(this, SWT.NONE);
    GridLayout iconLabelGridlayout = new GridLayout(1, true);
    iconLabelGridlayout.verticalSpacing = ALIGNED_MARGIN_TOP;
    iconLabelComposite.setLayout(iconLabelGridlayout);
    iconLabelComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    Image mainIcon = UiUtils.buildImageFromPngPath("/icons/chat/chatview_icon_welcome.png");
    this.copilotIconLabel = new Label(iconLabelComposite, SWT.NONE);
    this.copilotIconLabel.addDisposeListener(e -> {
      if (mainIcon != null && !mainIcon.isDisposed()) {
        mainIcon.dispose();
      }
    });
    this.copilotIconLabel.setImage(mainIcon);
    this.copilotIconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

    WrapLabel label = new WrapLabel(iconLabelComposite, SWT.CENTER | SWT.WRAP);
    label.setText(Messages.chat_initialChatView_title);
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

    // Set a width hint for the label to enable proper wrapping
    GridData labelGridData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
    labelGridData.widthHint = ALIGNED_LABEL_WIDTH;
    label.setLayoutData(labelGridData);
  }

  private void buildSubComposite() {
    this.subComposite = new Composite(this, SWT.NONE);
    this.subComposite.setLayout(new GridLayout(1, true));
    GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    gridData.widthHint = ALIGNED_LABEL_WIDTH;
    this.subComposite.setLayoutData(gridData);

    WrapLabel subLabel = new WrapLabel(this.subComposite, SWT.CENTER);
    subLabel.setText(Messages.chat_aiWarn_description);
    GridData subLabelGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    subLabelGridData.widthHint = ALIGNED_LABEL_WIDTH;
    subLabel.setLayoutData(subLabelGridData);
  }

  private void buildInstructions() {
    Composite instructionComposite = new Composite(this, SWT.NONE);
    GridLayout subCompositelayout = new GridLayout(1, true);
    instructionComposite.setLayout(subCompositelayout);
    GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    gridData.widthHint = ALIGNED_WIDTH;
    gridData.minimumHeight = 70;
    instructionComposite.setLayoutData(gridData);

    Image attachContextIcon = UiUtils.buildImageFromPngPath("/icons/chat/attach_context.png");
    instructionComposite.addDisposeListener(e -> {
      if (attachContextIcon != null && !attachContextIcon.isDisposed()) {
        attachContextIcon.dispose();
      }
    });
    buildLabelWithIcon(instructionComposite, attachContextIcon, Messages.chat_initialChatView_attachContextSuffix);

    WrapLabel subLabel = new WrapLabel(instructionComposite, SWT.CENTER);
    subLabel.setText(Messages.chat_initialChatView_useCommandsIntro);
  }
}
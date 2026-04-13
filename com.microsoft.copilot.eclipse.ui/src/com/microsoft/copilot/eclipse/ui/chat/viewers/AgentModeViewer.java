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
public class AgentModeViewer extends BaseViewer {
  private Font mainLabelFont;

  private Label copilotIconLabel;
  private Composite subComposite;

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
    this.copilotIconLabel = new Label(iconLabelComposite, SWT.CENTER);
    this.copilotIconLabel.addDisposeListener(e -> {
      if (mainIcon != null && !mainIcon.isDisposed()) {
        mainIcon.dispose();
      }
    });
    this.copilotIconLabel.setImage(mainIcon);
    this.copilotIconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

    WrapLabel label = new WrapLabel(iconLabelComposite, SWT.CENTER);
    // Always use Agent mode title for both default and custom modes
    label.setText(Messages.chat_agentModeView_title);
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
  }

  private void buildSubComposite() {
    this.subComposite = new Composite(this, SWT.NONE);
    GridLayout subCompositelayout = new GridLayout(1, true);
    this.subComposite.setLayout(subCompositelayout);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
    gridData.widthHint = ALIGNED_WIDTH;
    this.subComposite.setLayoutData(gridData);

    WrapLabel introLabel = new WrapLabel(this.subComposite, SWT.CENTER);
    // Always use default Agent mode description
    introLabel.setText(Messages.chat_agentModeView_agentModeIntro);
    GridData introGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    introGridData.widthHint = ALIGNED_LABEL_WIDTH;
    introLabel.setLayoutData(introGridData);

    WrapLabel warnLabel = new WrapLabel(this.subComposite, SWT.CENTER);
    warnLabel.setText(Messages.chat_aiWarn_description);
    GridData warnGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    warnGridData.widthHint = ALIGNED_LABEL_WIDTH;
    warnLabel.setLayoutData(warnGridData);
  }

  private void buildInstructions() {
    Composite instructionComposite = new Composite(this, SWT.NONE);
    GridLayout subCompositelayout = new GridLayout(1, true);
    instructionComposite.setLayout(subCompositelayout);
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
    gridData.widthHint = ALIGNED_WIDTH;
    gridData.minimumHeight = 70;
    instructionComposite.setLayoutData(gridData);

    // configure MCP icon with label
    Image configureMcpIcon = UiUtils.buildImageFromPngPath("/icons/chat/tools.png");
    instructionComposite.addDisposeListener(e -> {
      if (configureMcpIcon != null && !configureMcpIcon.isDisposed()) {
        configureMcpIcon.dispose();
      }
    });
    buildLabelWithIcon(instructionComposite, configureMcpIcon, Messages.chat_agentModeView_configureMcpSuffix);

    // attach context icon with label
    Image attachContextIcon = UiUtils.buildImageFromPngPath("/icons/chat/attach_context.png");
    instructionComposite.addDisposeListener(e -> {
      if (attachContextIcon != null && !attachContextIcon.isDisposed()) {
        attachContextIcon.dispose();
      }
    });
    buildLabelWithIcon(instructionComposite, attachContextIcon, Messages.chat_agentModeView_attachContextSuffix);
  }
}

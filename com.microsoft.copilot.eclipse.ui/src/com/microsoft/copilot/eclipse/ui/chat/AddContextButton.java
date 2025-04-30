package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A widget that displays a button to attach context.
 */
public class AddContextButton extends Composite {
  private Label lblAttachIcon;
  private Label lblButtonText;

  /**
   * Creates a new AttachContextButton.
   */
  public AddContextButton(Composite parent, ActionBar actionBar) {
    super(parent, SWT.BORDER);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 4;
    layout.marginHeight = 2;
    setLayout(layout);

    lblAttachIcon = new Label(this, SWT.NONE);
    lblAttachIcon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    Image attachImage = UiUtils.buildImageFromPngPath("/icons/chat/attach_context.png");
    lblAttachIcon.setImage(attachImage);
    lblAttachIcon.addDisposeListener(e -> {
      if (attachImage != null && !attachImage.isDisposed()) {
        attachImage.dispose();
      }
    });
    UiUtils.useParentBackground(this.lblAttachIcon);

    lblButtonText = new Label(this, SWT.NONE);
    lblButtonText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    lblButtonText.setText("Add Context...");
    UiUtils.useParentBackground(this.lblButtonText);

    MouseAdapter clickListener = new MouseAdapter() {
      @Override
      public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
        actionBar.onAddContextClicked();
      }
    };
    // Add mouse listener to 'this' so that clicking margin spaces will also trigger the action
    this.addMouseListener(clickListener);
    lblAttachIcon.addMouseListener(clickListener);
    lblButtonText.addMouseListener(clickListener);
    this.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

    UiUtils.useParentBackground(this);
  }

}

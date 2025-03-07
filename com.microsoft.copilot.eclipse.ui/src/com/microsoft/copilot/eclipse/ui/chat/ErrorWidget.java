package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.swt.WrapLabel;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Widget to display a message when the user has no quota.
 */
public class ErrorWidget extends Composite {
  private Image errorImage;
  private static final int MESSAGE_LEFT_MARGIN = 5;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param message the message to display
   */
  public ErrorWidget(Composite parent, int style, String message) {
    super(parent, style | SWT.BORDER);
    setLayout(new GridLayout(1, true));
    setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    buildWarnLabelWithIcon(message);
    parent.layout();
  }

  private void buildWarnLabelWithIcon(String message) {
    Composite composite = new Composite(this, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    Label icon = new Label(composite, SWT.CENTER);
    errorImage = UiUtils.buildImageFromPngPath("/icons/message_error.png");
    icon.setImage(errorImage);

    WrapLabel label = new WrapLabel(composite, SWT.LEFT);
    label.setText(message);
    label.setHorizontalIndent(MESSAGE_LEFT_MARGIN);

    composite.layout();
  }

  @Override
  public void dispose() {
    super.dispose();
    if (errorImage != null) {
      errorImage.dispose();
    }
  }
}

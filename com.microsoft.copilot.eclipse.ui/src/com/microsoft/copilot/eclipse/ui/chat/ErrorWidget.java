// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.swt.WrapLabel;

/**
 * Widget to display a message when the user has no quota.
 */
public class ErrorWidget extends Composite {
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
    icon.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));

    WrapLabel label = new WrapLabel(composite, SWT.LEFT);
    label.setText(message);
    label.setHorizontalIndent(MESSAGE_LEFT_MARGIN);

    composite.layout();
  }

}

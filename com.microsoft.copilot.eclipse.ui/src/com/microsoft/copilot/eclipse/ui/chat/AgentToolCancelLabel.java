// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.CopilotImages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;

/**
 * A label with icon that displays the cancel status of the agent tool.
 */
public class AgentToolCancelLabel extends Composite {
  private Image cancelIcon;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public AgentToolCancelLabel(Composite parent, int style, String cancelMessage) {
    super(parent, style);
    setLayout(new GridLayout(2, false));
    setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    this.cancelIcon = CopilotImages.getImage(CopilotImages.IMG_CANCEL_STATUS);
    Label iconLabel = new Label(this, SWT.LEFT);
    iconLabel.setImage(this.cancelIcon);

    Label textLabel = new Label(this, SWT.LEFT | SWT.WRAP);
    textLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    textLabel.setText(cancelMessage);
    textLabel.setData(CssConstants.CSS_CLASS_NAME_KEY, "text-secondary");
  }
}

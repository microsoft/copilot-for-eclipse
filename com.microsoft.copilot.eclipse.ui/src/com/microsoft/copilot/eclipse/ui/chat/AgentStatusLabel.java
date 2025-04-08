package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A label with icon that displays the running status of the agent.
 */
public class AgentStatusLabel extends Composite {
  private Image runningIcon;
  private Image completedIcon;
  private Label iconLabel;
  private Label textLabel;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public AgentStatusLabel(Composite parent, int style) {
    super(parent, style);
    setLayout(new GridLayout(2, false));
    this.addDisposeListener(e -> {
      if (this.runningIcon != null && !this.runningIcon.isDisposed()) {
        this.runningIcon.dispose();
      }
      if (this.completedIcon != null && !this.completedIcon.isDisposed()) {
        this.completedIcon.dispose();
      }
    });
  }

  /**
   * Set the status as completed for the agent with a status message.
   *
   * @param statusText the text to display when the agent is completed
   */
  public void setCompletedStatus(String statusText) {
    if (this.iconLabel == null) {
      this.iconLabel = new Label(this, SWT.LEFT);
    }

    if (this.completedIcon == null) {
      this.completedIcon = UiUtils.buildImageFromPngPath("/icons/complete_status.png");
    }
    iconLabel.setImage(completedIcon);

    setText(statusText);
  }

  /**
   * Set the status as running for the agent with a rotating spinner and a status message.
   *
   * @param statusText the text to display when the agent is running
   */
  public void setRunningStatus(String statusText) {
    if (this.iconLabel == null) {
      this.iconLabel = new Label(this, SWT.LEFT);
    }

    if (this.runningIcon == null) {
      this.runningIcon = UiUtils.buildImageFromPngPath("/icons/spinner/1.png");
    }
    iconLabel.setImage(runningIcon);

    setText(statusText);
  }

  private void setText(String text) {
    if (this.textLabel == null) {
      textLabel = new Label(this, SWT.LEFT);
      textLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
      textLabel.setForeground((this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY)));
    }
    textLabel.setText(text);
  }
}

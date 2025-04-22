package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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
  private int currentFrame = 1;
  private static final int TOTAL_FRAMES = 8; // Adjust based on actual number of spinner images
  private Runnable animationRunnable;

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
      stopAnimation();
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
    stopAnimation();
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

    // Stop any existing animation
    stopAnimation();

    // Start new animation
    startAnimation();

    setText(statusText);
  }

  private void startAnimation() {
    final Display display = getDisplay();

    animationRunnable = new Runnable() {
      @Override
      public void run() {
        if (isDisposed() || iconLabel.isDisposed()) {
          return;
        }

        // Dispose previous image
        if (runningIcon != null && !runningIcon.isDisposed()) {
          runningIcon.dispose();
        }

        // Load the next frame
        String imagePath = String.format("/icons/spinner/%d.png", currentFrame);
        runningIcon = UiUtils.buildImageFromPngPath(imagePath);
        iconLabel.setImage(runningIcon);

        // Update frame counter
        currentFrame = (currentFrame % TOTAL_FRAMES) + 1;

        // Schedule next frame
        display.timerExec(100, this);
      }
    };

    // Start the animation
    display.timerExec(0, animationRunnable);
  }

  private void stopAnimation() {
    if (animationRunnable != null) {
      getDisplay().timerExec(-1, animationRunnable);
      animationRunnable = null;
    }
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

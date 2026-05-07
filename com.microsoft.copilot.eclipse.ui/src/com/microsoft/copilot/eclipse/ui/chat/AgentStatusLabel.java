// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.swt.SpinnerAnimator;
import com.microsoft.copilot.eclipse.ui.utils.AccessibilityUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A label with icon that displays the running status of the agent.
 */
public class AgentStatusLabel extends Composite {
  private Image completedIcon;
  private Image cancelledIcon;
  private Label iconLabel;
  private ChatMarkupViewer textLabel;
  private SpinnerAnimator spinner;
  private Status status;
  private EventHandler cancelStatusHandler;
  private IEventBroker eventBroker;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public AgentStatusLabel(Composite parent, int style) {
    super(parent, style);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.horizontalSpacing = 0;
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    this.addDisposeListener(e -> {
      if (this.completedIcon != null && !this.completedIcon.isDisposed()) {
        this.completedIcon.dispose();
      }
      if (this.cancelledIcon != null && !this.cancelledIcon.isDisposed()) {
        this.cancelledIcon.dispose();
      }
      if (this.eventBroker != null) {
        this.eventBroker.unsubscribe(cancelStatusHandler);
      }
    });
    iconLabel = new Label(this, SWT.LEFT);
    spinner = new SpinnerAnimator(iconLabel);

    this.status = Status.RUNNING;
    this.cancelStatusHandler = new EventHandler() {
      @Override
      public void handleEvent(org.osgi.service.event.Event event) {
        setCancelledStatus();
      }
    };
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_MESSAGE_CANCELLED, cancelStatusHandler);
  }

  /**
   * Set the status as completed for the agent with a status message.
   *
   * @param statusText the text to display when the agent is completed
   */
  public void setCompletedStatus(String statusText) {
    spinner.stop();

    if (this.completedIcon == null) {
      this.completedIcon = UiUtils.buildImageFromPngPath("/icons/complete_status.png");
    }
    iconLabel.setImage(completedIcon);

    setText(statusText);
    this.status = Status.COMPLETED;
  }

  /**
   * Set the status as running for the agent with a rotating spinner and a status message.
   *
   * @param statusText the text to display when the agent is running
   */
  public void setRunningStatus(String statusText) {
    spinner.start();

    setText(statusText);
    this.status = Status.RUNNING;
  }

  /**
   * Set the error status for the agent with a status message.
   */
  public void setErrorStatus() {
    if (this.status == Status.RUNNING) {
      spinner.stop();
    }
    iconLabel.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
    this.status = Status.ERROR;
  }

  /**
   * Cancel the current running status of the agent status label.
   */
  public void setCancelledStatus() {
    if (this.status == Status.RUNNING) {
      spinner.stop();

      if (this.cancelledIcon == null) {
        this.cancelledIcon = UiUtils.buildImageFromPngPath("/icons/cancel_status.png");
      }
      iconLabel.setImage(cancelledIcon);

      this.status = Status.CANCELLED;
    }
  }

  /**
   * Set the text to display next to the icon.
   */
  public void setText(String text) {
    if (this.textLabel == null) {
      textLabel = new ChatMarkupViewer(this, SWT.LEFT | SWT.WRAP);
      StyledText styledText = textLabel.getTextWidget();
      styledText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
      styledText.setEditable(false);
      styledText.setData(CssConstants.CSS_CLASS_NAME_KEY, "text-secondary");
      AccessibilityUtils.addFocusBorderToComposite(styledText);
    }
    textLabel.setMarkup(text);
  }

  private enum Status {
    RUNNING, COMPLETED, ERROR, CANCELLED
  }
}

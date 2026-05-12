package com.microsoft.copilot.eclipse.ui.notifications;

import org.eclipse.jface.notifications.NotificationPopup;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Popup for quota warning notifications.
 */
public class QuotaWarningNotificationPopup {

  private static final int ALIGN_SPACING = 8;
  private static final String BORDER_INSTALLED_KEY = "quotaWarningNotificationPopup.borderInstalled";

  private final Display display;
  private final String message;
  private final int delayMs;

  /**
   * Creates a popup for a quota warning message.
   *
   * @param display the target display
   * @param message the quota warning message
   * @param delayMs the popup auto-close delay in milliseconds
   */
  public QuotaWarningNotificationPopup(Display display, String message, int delayMs) {
    this.display = display;
    this.message = message;
    this.delayMs = delayMs;
  }

  /**
   * Opens the popup.
   */
  public void open() {
    NotificationPopup.forDisplay(display).title(parent -> createNotificationTitle(parent), false).fadeIn(true)
        .delay(delayMs).content(parent -> createNotificationContent(parent, message)).open();
  }

  private Control createNotificationTitle(Composite parent) {
    addBorder(parent.getShell());

    Composite title = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 4;
    layout.marginHeight = 4;
    title.setLayout(layout);
    title.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

    Label iconLabel = new Label(title, SWT.NONE);
    Image icon = UiUtils.buildImageFromPngPath("/icons/github_copilot.png");
    iconLabel.setImage(icon);
    iconLabel.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    parent.addDisposeListener(e -> {
      if (icon != null && !icon.isDisposed()) {
        icon.dispose();
      }
    });

    Label titleLabel = new Label(title, SWT.NONE);
    titleLabel.setText(Messages.quotaWarning_title);
    titleLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));

    return title;
  }

  /**
   * Needs to add a border to this notification popup since the chat view has the same color with the notification
   * background in dark mode. The boundary of the notification will become unclear without the border.
   */
  private void addBorder(Shell shell) {
    if (Boolean.TRUE.equals(shell.getData(BORDER_INSTALLED_KEY))) {
      return;
    }

    Color borderColor = shell.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
    shell.addPaintListener(e -> {
      Rectangle bounds = shell.getClientArea();
      e.gc.setForeground(borderColor);
      e.gc.setLineWidth(1);
      e.gc.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
    });
    shell.setData(BORDER_INSTALLED_KEY, Boolean.TRUE);
  }

  private Composite createNotificationContent(Composite parent, String notificationMessage) {
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = ALIGN_SPACING;
    layout.marginHeight = ALIGN_SPACING;
    layout.marginBottom = ALIGN_SPACING;
    layout.horizontalSpacing = ALIGN_SPACING;
    Composite content = new Composite(parent, SWT.NONE);
    content.setLayout(layout);

    new Label(content, SWT.NONE).setImage(parent.getDisplay().getSystemImage(SWT.ICON_WARNING));

    Label messageLabel = new Label(content, SWT.WRAP);
    messageLabel.setText(notificationMessage);
    GridData messageLayoutData = new GridData(SWT.FILL, SWT.NONE, true, true);
    messageLabel.setLayoutData(messageLayoutData);

    Composite buttons = new Composite(content, SWT.NONE);
    GridLayout buttonLayout = new GridLayout(2, true);
    buttonLayout.horizontalSpacing = ALIGN_SPACING;
    buttonLayout.marginWidth = 0;
    buttons.setLayout(buttonLayout);
    buttons.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, true, false, 2, 1));

    createButton(buttons, Messages.quotaWarning_closeButton, e -> parent.getShell().close());
    createButton(buttons, Messages.quotaWarning_increaseBudgetButton, e -> {
      UiUtils.openLink(UiConstants.MANAGE_COPILOT_OVERAGE_URL);
      parent.getShell().close();
    });

    return content;
  }

  private void createButton(Composite parent, String label, Listener listener) {
    Button button = new Button(parent, SWT.PUSH);
    button.setText(label);
    button.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    button.addListener(SWT.Selection, listener);
  }
}
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A reusable banner widget that displays an informational message with an inline link and a close button. Shows an info
 * icon, the provided message with an appended link, and a dismiss (×) button.
 *
 * <p>
 * Usage example:
 * 
 * <pre>
 * var banner = new StaticBanner(parent, SWT.NONE, "You've used 90% of your rate limit.", "Get more info",
 *     "https://example.com", "Dismiss");
 * banner.show();
 * </pre>
 */
public class StaticBanner extends Composite {
  private Link messageLink;

  /**
   * Create a static informational banner.
   *
   * @param parent the parent composite
   * @param style the SWT style
   * @param message the informational message to display
   * @param linkText the text for the inline link (e.g. "Get more info")
   * @param linkUrl the URL to open when the link is clicked
   * @param closeTooltip the tooltip for the close button
   */
  public StaticBanner(Composite parent, int style, String message, String linkText, String linkUrl,
      String closeTooltip) {
    super(parent, style | SWT.BORDER);

    GridLayout layout = new GridLayout(3, false);
    layout.marginWidth = 10;
    layout.marginHeight = 8;
    layout.horizontalSpacing = 6;
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    // Info icon
    Label iconLabel = new Label(this, SWT.NONE);
    Image infoImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);
    iconLabel.setImage(infoImage);
    iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

    // Message + inline link
    this.messageLink = new Link(this, SWT.WRAP);
    this.messageLink.setText(buildMessageText(message, linkText, linkUrl));
    this.messageLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    this.messageLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (StringUtils.isNotBlank(linkUrl)) {
          UiUtils.openLink(linkUrl);
        }
      }
    });

    // Close button
    Label closeButton = new Label(this, SWT.NONE);
    Image closeImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE);
    closeButton.setImage(closeImage);
    closeButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
    closeButton.setToolTipText(closeTooltip);
    closeButton.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    closeButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        disposeBanner();
      }
    });

    setVisible(false);
    GridData gd = (GridData) getLayoutData();
    gd.exclude = true;
  }

  /**
   * Show the banner.
   */
  public void show() {
    if (isDisposed()) {
      return;
    }
    setVisible(true);
    GridData gd = (GridData) getLayoutData();
    gd.exclude = false;
    getParent().requestLayout();
  }

  /**
   * Hide the banner.
   */
  public void hide() {
    if (isDisposed()) {
      return;
    }
    setVisible(false);
    GridData gd = (GridData) getLayoutData();
    gd.exclude = true;
    getParent().requestLayout();
  }

  private void disposeBanner() {
    if (isDisposed()) {
      return;
    }
    Composite parent = getParent();
    dispose();
    if (parent != null && !parent.isDisposed()) {
      parent.requestLayout();
    }
  }

  private static String buildMessageText(String message, String linkText, String linkUrl) {
    String safeMessage = escapeForLink(message);
    if (StringUtils.isBlank(linkText) || StringUtils.isBlank(linkUrl)) {
      return safeMessage;
    }
    return NLS.bind(com.microsoft.copilot.eclipse.ui.i18n.Messages.chat_staticBanner_messageWithLink, safeMessage,
        escapeForLink(linkText));
  }

  private static String escapeForLink(String text) {
    if (text == null) {
      return StringUtils.EMPTY;
    }
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}

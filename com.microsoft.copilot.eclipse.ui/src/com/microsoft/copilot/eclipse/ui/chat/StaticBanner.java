// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
 * Reusable, dismissible banner shown above the chat input. Layout is a 3-column grid: severity icon, wrapping message,
 * close button; an optional second row of action links is added when {@code actions} is non-empty.
 *
 * <p>The banner is constructed hidden and layout-excluded; call {@link #show()} to reveal it. Callers must pass
 * already-localized strings.
 *
 * <p>Usage example:
 *
 * <pre>
 * var banner = new StaticBanner(parent, SWT.NONE, "You've used 75% of your monthly quota.",
 *     List.of(new BannerAction("Upgrade Plan", "https://example.com/upgrade")), "Dismiss", true);
 * banner.show();
 * </pre>
 */
public class StaticBanner extends Composite {
  private Label messageLabel;

  /**
   * Create a hidden static banner; call {@link #show()} to reveal. {@link SWT#BORDER} is always applied.
   *
   * @param parent the parent composite
   * @param style additional SWT style bits
   * @param message the message to display ({@code null} treated as empty)
   * @param actions optional action links below the message; entries with blank label or URL are skipped
   * @param closeTooltip tooltip for the close (×) button
   * @param warning {@code true} for the warning icon; {@code false} for the info icon
   */
  public StaticBanner(Composite parent, int style, String message, List<BannerAction> actions, String closeTooltip,
      boolean warning) {
    super(parent, style | SWT.BORDER);

    GridLayout layout = new GridLayout(3, false);
    layout.marginWidth = 10;
    layout.marginHeight = 8;
    layout.horizontalSpacing = 6;
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    // Severity icon
    Label iconLabel = new Label(this, SWT.NONE);
    String iconKey = warning ? ISharedImages.IMG_OBJS_WARN_TSK : ISharedImages.IMG_OBJS_INFO_TSK;
    Image iconImage = PlatformUI.getWorkbench().getSharedImages().getImage(iconKey);
    iconLabel.setImage(iconImage);
    iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

    // Wrapping message text
    this.messageLabel = new Label(this, SWT.WRAP);
    this.messageLabel.setText(StringUtils.defaultString(message));
    this.messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Close (×) button
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

    // Optional action-link row, aligned under the message column.
    List<BannerAction> safeActions = actions == null ? List.of() : actions;
    if (!safeActions.isEmpty()) {
      // Spacer to align with the icon column above.
      new Label(this, SWT.NONE).setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

      GridLayout actionLayout = new GridLayout(safeActions.size(), false);
      actionLayout.marginWidth = 0;
      actionLayout.marginHeight = 0;
      actionLayout.horizontalSpacing = 12;
      actionLayout.verticalSpacing = 0;
      GridData actionRowData = new GridData(SWT.FILL, SWT.CENTER, true, false);
      actionRowData.horizontalSpan = 2;
      Composite actionRow = new Composite(this, SWT.NONE);
      actionRow.setLayout(actionLayout);
      actionRow.setLayoutData(actionRowData);

      for (BannerAction action : safeActions) {
        addActionLink(actionRow, action);
      }
    }

    setVisible(false);
    GridData gd = (GridData) getLayoutData();
    gd.exclude = true;
  }

  /**
   * Reveal the banner and re-layout the parent. No-op if disposed.
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

  private static void addActionLink(Composite parent, BannerAction action) {
    if (action == null || StringUtils.isBlank(action.text()) || StringUtils.isBlank(action.url())) {
      return;
    }
    Link link = new Link(parent, SWT.NONE);
    link.setText("<a>" + escapeForLink(action.text()) + "</a>");
    link.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        UiUtils.openLink(action.url());
      }
    });
  }

  private static String escapeForLink(String text) {
    if (text == null) {
      return StringUtils.EMPTY;
    }
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}

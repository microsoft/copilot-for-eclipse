// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * A composite that displays an icon and a wrappable link. The component automatically resizes when the parent composite
 * is resized. The icon image is not owned by this widget and is therefore not disposed by it.
 */
public class WrappableIconLink extends Composite {

  private static final int DEFAULT_WIDTH_HINT = 400;
  private static final int DEFAULT_MARGIN = 20;

  private Composite parent;
  private int widthMargin;

  // Icon
  private Label iconLabel;
  private Image icon;

  // Link
  private Link linkControl;
  private String linkText;

  /**
   * Creates a new WrappableIconLink.
   *
   * @param parent the parent composite
   * @param icon the icon image (not owned by this widget)
   * @param linkText the text for the link (may contain HTML link tags)
   * @param widthMargin the horizontal margin to subtract when computing the link width
   */
  private WrappableIconLink(Composite parent, Image icon, String linkText, int widthMargin) {
    super(parent, SWT.NONE);
    this.widthMargin = widthMargin;
    this.parent = parent;
    this.linkText = linkText;
    this.icon = icon;
    createControls();
    setupResizeListener();
  }

  // ------------- Factory methods -------------
  /**
   * Creates a WrappableIconLink with the given icon image.
   *
   * @param parent the parent composite
   * @param icon the icon image (not owned by this widget)
   * @param linkText the text for the link (may contain HTML link tags)
   * @return the created {@link WrappableIconLink}
   */
  public static WrappableIconLink create(Composite parent, Image icon, String linkText) {
    return new WrappableIconLink(parent, icon, linkText, DEFAULT_MARGIN);
  }

  /**
   * Creates a WrappableIconLink with the given icon image and custom width margin.
   *
   * @param parent the parent composite
   * @param icon the icon image (not owned by this widget)
   * @param linkText the text for the link (may contain HTML link tags)
   * @param widthMargin the horizontal margin to subtract when computing the link width
   * @return the created {@link WrappableIconLink}
   */
  public static WrappableIconLink create(Composite parent, Image icon, String linkText, int widthMargin) {
    return new WrappableIconLink(parent, icon, linkText, widthMargin);
  }

  /**
   * Creates the icon and link controls.
   */
  private void createControls() {
    GridLayout layout = new GridLayout(2, false);
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // Create icon label
    iconLabel = new Label(this, SWT.NONE);
    iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    if (icon != null) {
      iconLabel.setImage(icon);
    }

    // Create link control
    linkControl = new Link(this, SWT.WRAP);
    linkControl.setText(linkText);
    GridData linkData = new GridData(SWT.FILL, SWT.FILL, true, true);
    linkData.widthHint = DEFAULT_WIDTH_HINT;
    linkControl.setLayoutData(linkData);
    // Add listener for opening URLs
    linkControl.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        try {
          PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
        } catch (Exception ex) {
          CopilotCore.LOGGER.error("Failed to open URL: " + e.text, ex);
        }
      }
    });
  }

  /**
   * Sets up the resize listener to dynamically adjust the link width.
   */
  private void setupResizeListener() {
    parent.addControlListener(ControlListener.controlResizedAdapter(e -> updateLinkWidth()));
  }

  /**
   * Updates the link width based on the parent's current size.
   */
  private void updateLinkWidth() {
    if (linkControl != null && !linkControl.isDisposed() && iconLabel != null && !iconLabel.isDisposed()
        && parent != null && !parent.isDisposed()) {

      int iconWidth = iconLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
      int availableWidth = parent.getSize().x - widthMargin - iconWidth - 10; // 10 for spacing and margins

      GridData linkData = new GridData(SWT.FILL, SWT.FILL, true, true);
      linkData.widthHint = Math.max(100, availableWidth); // Minimum width of 100
      linkControl.setLayoutData(linkData);

      parent.requestLayout();
    }
  }
}
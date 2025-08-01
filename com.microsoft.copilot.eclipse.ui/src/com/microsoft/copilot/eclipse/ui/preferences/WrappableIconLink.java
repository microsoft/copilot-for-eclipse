package com.microsoft.copilot.eclipse.ui.preferences;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
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
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A composite that displays an icon and a wrappable link. The component automatically
 * resizes when the parent composite is resized and handles proper image disposal.
 */
public class WrappableIconLink extends Composite {

  private static final int DEFAULT_WIDTH_HINT = 400;
  private static final int DEFAULT_MARGIN = 20;

  private Composite parent;
  private int widthMargin;

  // Icon
  private Label iconLabel;
  private String iconPath;
  private Image icon;

  // Link
  private Link linkControl;
  private String linkText;

  /**
   * Creates a new WrappableIconLink with default layout settings.
   */
  public WrappableIconLink(Composite parent, String iconPath, String linkText) {
    this(parent, iconPath, linkText, DEFAULT_MARGIN);
  }

  /**
   * Creates a new WrappableIconLink with custom width margin.
   *
   * @param parent the parent composite
   * @param iconPath the path to the icon image
   * @param linkText the text for the link (may contain HTML link tags)
   * @param widthMargin the margin to subtract from parent width when resizing
   */
  public WrappableIconLink(Composite parent, String iconPath, String linkText, int widthMargin) {
    super(parent, SWT.NONE);
    this.widthMargin = widthMargin;
    this.parent = parent;
    this.linkText = linkText;
    this.iconPath = iconPath;

    createControls();
    setupResizeListener();
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
    if (iconPath != null) {
      ImageDescriptor imageDescriptor = UiUtils.buildImageDescriptorFromPngPath(iconPath);
      if (imageDescriptor != null) {
        icon = imageDescriptor.createImage();
        iconLabel.setImage(icon);
      }
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

    // Dispose image when composite is disposed
    addDisposeListener(e -> {
      if (icon != null && !icon.isDisposed()) {
        icon.dispose();
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
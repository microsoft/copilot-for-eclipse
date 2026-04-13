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
 * A composite that displays an icon and a wrappable link. The component automatically resizes when the parent composite
 * is resized and handles proper image disposal.
 */
public class WrappableIconLink extends Composite {

  private static final int DEFAULT_WIDTH_HINT = 400;
  private static final int DEFAULT_MARGIN = 20;

  private Composite parent;
  private int widthMargin;

  // Icon
  private Label iconLabel;
  private String iconPath; // only used for customized image case
  private Image icon; // holds the created (non-shared) image if any
  private boolean isSharedImage; // true when using shared workbench image

  // Link
  private Link linkControl;
  private String linkText;

  /**
   * Creates a new WrappableIconLink with default layout settings and no icon.
   *
   * @param parent the parent composite
   * @param linkText the text for the link (may contain HTML link tags)
   */
  private WrappableIconLink(Composite parent, String iconPath, Image sharedImage, String linkText, int widthMargin) {
    super(parent, SWT.NONE);
    this.widthMargin = widthMargin;
    this.parent = parent;
    this.linkText = linkText;
    this.iconPath = iconPath;
    this.isSharedImage = sharedImage != null;
    this.icon = sharedImage;
    createControls();
    setupResizeListener();
  }

  // ------------- Factory methods -------------
  /**
   * Creates a WrappableIconLink with a shared workbench image.
   */
  public static WrappableIconLink createWithSharedImage(Composite parent, Image sharedImage, String linkText) {
    return new WrappableIconLink(parent, null, sharedImage, linkText, DEFAULT_MARGIN);
  }

  /**
   * Creates a WrappableIconLink with a shared workbench image and custom width margin.
   */
  public static WrappableIconLink createWithSharedImage(Composite parent, Image sharedImage, String linkText,
      int widthMargin) {
    return new WrappableIconLink(parent, null, sharedImage, linkText, widthMargin);
  }

  /**
   * Creates a WrappableIconLink with a customized image from the given path.
   */
  public static WrappableIconLink createWithCustomizedImage(Composite parent, String iconPath, String linkText) {
    return new WrappableIconLink(parent, iconPath, null, linkText, DEFAULT_MARGIN);
  }

  /**
   * Creates a WrappableIconLink with a customized image from the given path and custom width margin.
   */
  public static WrappableIconLink createWithCustomizedImage(Composite parent, String iconPath, String linkText,
      int widthMargin) {
    return new WrappableIconLink(parent, iconPath, null, linkText, widthMargin);
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
    if (isSharedImage && icon != null) {
      iconLabel.setImage(icon); // shared image
    } else if (iconPath != null) {
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
      if (!isSharedImage && icon != null && !icon.isDisposed()) {
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
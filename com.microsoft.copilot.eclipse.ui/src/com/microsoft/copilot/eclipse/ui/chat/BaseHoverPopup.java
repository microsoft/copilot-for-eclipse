// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Base class for hover popups that appear near an anchor widget and auto-close when the cursor leaves.
 *
 * <p>Subclasses implement {@link #populateContent(Composite)} to add their specific widgets, then call
 * {@link #openPopup(Control)} to display the popup.
 */
public abstract class BaseHoverPopup {

  protected static final String DROPDOWN_POPUP_CSS_ID = "dropdown-popup";
  protected static final int SECTION_SPACING = 8;

  private static final String POPUP_SECONDARY_TEXT_CLASS = "popup-secondary-text";
  private static final int POPUP_WIDTH = 200;
  private static final int ROW_SPACING = 2;
  private static final int BORDER_ARC = 8;
  private static final int H_MARGIN = 8;
  private static final int V_MARGIN = 8;
  private static final int POLL_INTERVAL_MS = 100;

  protected Shell shell;
  protected Control anchor;
  protected final IStylingEngine stylingEngine;
  private Runnable pollRunnable;

  /**
   * Constructor initializes the styling engine for applying CSS styles to popup widgets.
   */
  protected BaseHoverPopup() {
    this.stylingEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);
  }

  /**
   * Creates the popup shell, calls {@link #populateContent(Composite)}, and opens the popup above the anchor.
   */
  protected void openPopup(Control anchorControl) {
    this.anchor = anchorControl;
    if (isOpen()) {
      return;
    }
    stopPolling();

    Shell parentShell = anchorControl.getShell();
    shell = new Shell(parentShell, SWT.NO_TRIM | SWT.ON_TOP);

    GridLayout shellLayout = new GridLayout(1, false);
    shellLayout.marginWidth = H_MARGIN;
    shellLayout.marginHeight = V_MARGIN;
    shellLayout.verticalSpacing = ROW_SPACING;
    shell.setLayout(shellLayout);

    populateContent(shell);
    applyCssId(shell, DROPDOWN_POPUP_CSS_ID);
    addBorder(shell);

    shell.pack();
    Point size = shell.getSize();
    shell.setSize(Math.max(size.x, POPUP_WIDTH), size.y);
    positionPopup(shell, anchorControl);
    shell.open();
    startPolling();
  }

  /**
   * Subclasses add their specific content widgets to the popup shell.
   */
  protected abstract void populateContent(Composite parent);

  /**
   * Closes the popup and disposes the shell. Should be called when the cursor leaves the popup or anchor.
   */
  protected void close() {
    stopPolling();
    anchor = null;
    if (shell != null && !shell.isDisposed()) {
      shell.dispose();
      shell = null;
    }
  }

  protected boolean isOpen() {
    return shell != null && !shell.isDisposed();
  }

  /**
   * Utility method for adding a section header label with bold font and vertical spacing.
   *
   * @param parent the parent composite to add the header to
   * @param text the header text to display
   * @param verticalIndent the vertical spacing below the header
   */
  protected void addSectionHeader(Composite parent, String text, int verticalIndent) {
    Label header = new Label(parent, SWT.NONE);
    header.setText(text);
    applyCssId(header, DROPDOWN_POPUP_CSS_ID);
    Font boldFont = UiUtils.getBoldFont(header.getDisplay(), header.getFont());
    header.addDisposeListener(e -> boldFont.dispose());
    header.setFont(boldFont);
    GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
    gd.verticalIndent = verticalIndent;
    header.setLayoutData(gd);
  }

  /**
   * Utility method for adding a horizontal separator with optional top spacing.
   *
   * @param parent the parent composite to add the separator to
   * @param topSpacing the vertical spacing above the separator
   */
  protected void addSeparator(Composite parent, int topSpacing) {
    Composite separator = new Composite(parent, SWT.NONE);
    applyCssId(separator, DROPDOWN_POPUP_CSS_ID);
    GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
    gd.heightHint = 1;
    gd.verticalIndent = topSpacing;
    separator.setLayoutData(gd);
    Color sepColor = CssConstants.getSeparatorColor(parent.getDisplay());
    separator.addPaintListener(e -> {
      Rectangle r = separator.getClientArea();
      e.gc.setBackground(sepColor);
      e.gc.fillRectangle(0, 0, r.width, 1);
    });
  }

  /**
   * Utility method for creating a row composite with two columns for key-value pairs, styled for the popup.
   *
   * @param parent the parent composite to add the row to
   * @return the created row composite with a GridLayout of 2 columns and no margins or spacing
   */
  protected Composite createRowComposite(Composite parent) {
    Composite row = new Composite(parent, SWT.NONE);
    applyCssId(row, DROPDOWN_POPUP_CSS_ID);
    GridLayout gl = new GridLayout(2, false);
    gl.marginWidth = 0;
    gl.marginHeight = 0;
    gl.horizontalSpacing = 0;
    row.setLayout(gl);
    row.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    return row;
  }

  /**
   * Utility method for adding a key-value pair row to the popup, with the key left-aligned and the value right-aligned.
   *
   * @param parent the parent composite to add the row to
   * @param key the text for the key label, which will be left-aligned and take up remaining horizontal space
   * @param value the text for the value label, which will be right-aligned and take only necessary horizontal space
   * @return the value label, in case the caller wants to update its text later
   */
  protected Label addKeyValueRow(Composite parent, String key, String value) {
    Composite row = createRowComposite(parent);

    Label keyLabel = createSecondaryTextLabel(row, key);
    keyLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    Label valueLabel = createSecondaryTextLabel(row, value);
    valueLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
    return valueLabel;
  }

  /**
   * Utility method for creating a label styled as secondary text in the popup, with the appropriate CSS class and ID.
   *
   * @param parent the parent composite to add the label to
   * @param text the text to display in the label
   * @return the created label with the secondary text style applied
   */
  protected Label createSecondaryTextLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.NONE);
    label.setText(text);
    applyCssId(label, DROPDOWN_POPUP_CSS_ID);
    UiUtils.applyCssClass(label, POPUP_SECONDARY_TEXT_CLASS, stylingEngine);
    return label;
  }

  /**
   * Utility method for applying a CSS ID to a control and re-styling it with the styling engine.
   *
   * @param control the control to apply the CSS ID to
   * @param cssId the CSS ID to set on the control, which can be used in CSS selectors to style it
   */
  protected void applyCssId(Control control, String cssId) {
    control.setData(CssConstants.CSS_ID_KEY, cssId);
    if (stylingEngine != null) {
      stylingEngine.style(control);
    }
  }

  private void addBorder(Shell target) {
    Color borderColor = CssConstants.getBorderColor(target.getDisplay());
    target.addPaintListener(e -> {
      Rectangle bounds = target.getClientArea();
      e.gc.setAntialias(SWT.ON);
      e.gc.setForeground(borderColor);
      e.gc.setLineWidth(1);
      e.gc.drawRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1, BORDER_ARC, BORDER_ARC);
    });
  }

  private void positionPopup(Shell popup, Control anchorControl) {
    Point anchorLoc = anchorControl.toDisplay(0, 0);
    Point anchorSize = anchorControl.getSize();
    Point popupSize = popup.getSize();

    int x = anchorLoc.x + (anchorSize.x - popupSize.x) / 2;
    int y = anchorLoc.y - popupSize.y;

    Rectangle screenBounds = getMonitorBounds(anchorControl.getDisplay(), anchorLoc);
    x = Math.max(screenBounds.x, Math.min(x, screenBounds.x + screenBounds.width - popupSize.x));
    if (y < screenBounds.y) {
      y = anchorLoc.y + anchorSize.y;
    }
    popup.setLocation(x, y);
  }

  private static Rectangle getMonitorBounds(Display display, Point location) {
    for (Monitor monitor : display.getMonitors()) {
      if (monitor.getBounds().contains(location)) {
        return monitor.getBounds();
      }
    }
    return display.getPrimaryMonitor().getBounds();
  }

  private void startPolling() {
    Display display = getActiveDisplay();
    if (display == null) {
      return;
    }
    pollRunnable = () -> {
      if (shell == null || shell.isDisposed()) {
        return;
      }
      if (!isCursorInside(anchor) && !isCursorInside(shell)) {
        close();
      } else {
        display.timerExec(POLL_INTERVAL_MS, pollRunnable);
      }
    };
    display.timerExec(POLL_INTERVAL_MS, pollRunnable);
  }

  private void stopPolling() {
    Display display = getActiveDisplay();
    if (display != null && pollRunnable != null) {
      display.timerExec(-1, pollRunnable);
    }
    pollRunnable = null;
  }

  private Display getActiveDisplay() {
    if (shell != null && !shell.isDisposed()) {
      return shell.getDisplay();
    }
    if (anchor != null && !anchor.isDisposed()) {
      return anchor.getDisplay();
    }
    return null;
  }

  private boolean isCursorInside(Control control) {
    if (control == null || control.isDisposed()) {
      return false;
    }
    Display display = control.getDisplay();
    if (display == null || display.isDisposed()) {
      return false;
    }
    return getDisplayBounds(control).contains(display.getCursorLocation());
  }

  private static Rectangle getDisplayBounds(Control control) {
    Point location = control.toDisplay(0, 0);
    Point size = control.getSize();
    return new Rectangle(location.x, location.y, size.x, size.y);
  }
}

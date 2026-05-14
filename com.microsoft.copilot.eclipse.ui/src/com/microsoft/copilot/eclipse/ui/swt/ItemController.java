// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Controller that owns the focus / selection CSS state and the focused-background paint for a single popup item row.
 *
 * <p>Extracted from the inline state previously kept on the row composite via {@code Control.setData(...)} slots so
 * other popup rows (e.g. future inline action rows inside hover content) can reuse the exact same selection and focus
 * presentation without duplicating the CSS-id propagation and paint logic.
 *
 * <p>A controller is attached to a row composite via {@link #attach(Composite, IStylingEngine, String)}. It then:
 * <ul>
 *   <li>tracks a <em>base</em> CSS id ({@link #CSS_DEFAULT_ID} or {@link #CSS_SELECTED_ID}) reflecting whether the row
 *       represents the currently selected entry,</li>
 *   <li>tracks a <em>focused</em> flag indicating whether the row is currently highlighted (mouse hover or keyboard
 *       focus),</li>
 *   <li>propagates the resulting CSS id to every descendant control so the focused background paints behind child
 *       labels,</li>
 *   <li>paints a rounded background fill on the row while focused.</li>
 * </ul>
 */
public final class ItemController {

  /** CSS id applied to a default (non-selected, non-focused) popup item row and its descendants. */
  public static final String CSS_DEFAULT_ID = "popup-item-default";

  /** CSS id applied to the popup item row that represents the currently selected entry. */
  public static final String CSS_SELECTED_ID = "popup-item-selected";

  /** CSS id applied to descendants of a popup item row that is currently highlighted. */
  public static final String CSS_FOCUSED_ID = "popup-item-focused";

  /** Corner radius of the rounded background fill painted on focused popup item rows. */
  public static final int FOCUS_ARC = 6;

  private final Composite row;
  private final IStylingEngine styling;
  private String baseCssId;
  private boolean focused;

  private ItemController(Composite row, IStylingEngine styling, String baseCssId) {
    this.row = row;
    this.styling = styling;
    this.baseCssId = baseCssId;
  }

  /**
   * Attaches a controller to {@code row}: installs the focused-background paint listener and seeds the initial
   * (non-focused) CSS state on the row and every existing descendant.
   *
   * @param row the row composite to manage; children should already be created so the initial CSS state propagates
   *     to them
   * @param styling the workbench styling engine (may be {@code null} when CSS styling is unavailable)
   * @param baseCssId the initial base CSS id (typically {@link #CSS_DEFAULT_ID} or {@link #CSS_SELECTED_ID})
   * @return the attached controller
   */
  public static ItemController attach(Composite row, IStylingEngine styling, String baseCssId) {
    ItemController controller = new ItemController(row, styling, baseCssId);
    row.addPaintListener(e -> controller.paintFocusedBackground(e.gc));
    controller.applyState();
    return controller;
  }

  /**
   * Updates the base CSS id (default vs. selected). No-op when unchanged.
   *
   * @param newBaseCssId the new base CSS id; ignored when {@code null}
   */
  public void setBaseCssId(String newBaseCssId) {
    if (newBaseCssId == null || newBaseCssId.equals(this.baseCssId)) {
      return;
    }
    this.baseCssId = newBaseCssId;
    applyState();
  }

  /**
   * Updates the focused flag. No-op when unchanged.
   *
   * @param newFocused whether the row should be rendered as focused
   */
  public void setFocused(boolean newFocused) {
    if (newFocused == this.focused) {
      return;
    }
    this.focused = newFocused;
    applyState();
  }

  private void applyState() {
    if (row.isDisposed() || baseCssId == null) {
      return;
    }
    String descendantCssId = focused ? CSS_FOCUSED_ID : baseCssId;
    row.setRedraw(false);
    applyCssId(row, baseCssId);
    for (Control child : row.getChildren()) {
      applyCssIdRecursively(child, descendantCssId);
    }
    row.setRedraw(true);
    row.redraw();
  }

  private void paintFocusedBackground(GC gc) {
    if (row.isDisposed() || !focused) {
      return;
    }
    Rectangle bounds = row.getClientArea();
    if (bounds.width <= 0 || bounds.height <= 0) {
      return;
    }
    gc.setAntialias(SWT.ON);
    gc.setBackground(CssConstants.getPopupItemFocusBgColor(row.getDisplay()));
    gc.fillRoundRectangle(0, 0, bounds.width, bounds.height, FOCUS_ARC, FOCUS_ARC);
  }

  private void applyCssId(Control control, String cssId) {
    if (control.isDisposed()) {
      return;
    }
    if (!cssId.equals(control.getData(CssConstants.CSS_ID_KEY))) {
      control.setData(CssConstants.CSS_ID_KEY, cssId);
      if (styling != null) {
        styling.style(control);
      }
    }
  }

  private void applyCssIdRecursively(Control control, String cssId) {
    applyCssId(control, cssId);
    if (control instanceof Composite composite) {
      for (Control child : composite.getChildren()) {
        applyCssIdRecursively(child, cssId);
      }
    }
  }
}

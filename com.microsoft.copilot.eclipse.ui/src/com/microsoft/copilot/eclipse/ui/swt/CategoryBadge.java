// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * A custom-painted badge widget that displays a model picker category label with a rounded border in the category's
 * theme color.
 */
public class CategoryBadge extends Composite {

  private static final int HORIZONTAL_PADDING = 6;
  private static final int VERTICAL_PADDING = 1;

  /**
   * Creates a category badge for the given category string. Returns {@code null} if the category has no known color
   * mapping.
   *
   * @param parent the parent composite
   * @param category the raw category value from the API (e.g. "powerful")
   * @return the badge composite, or {@code null} if the category is unrecognised
   */
  public static CategoryBadge create(Composite parent, String category) {
    Color badgeColor = getCategoryColor(parent.getDisplay(), category);
    if (badgeColor == null) {
      return null;
    }
    return new CategoryBadge(parent, category, badgeColor);
  }

  private CategoryBadge(Composite parent, String category, Color badgeColor) {
    super(parent, SWT.NONE);
    setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));

    String displayText = StringUtils.capitalize(category);

    GC gc = new GC(this);
    Point textExtent = gc.textExtent(displayText);
    gc.dispose();

    int badgeWidth = textExtent.x + 2 * HORIZONTAL_PADDING;
    int badgeHeight = textExtent.y + 2 * VERTICAL_PADDING;
    GridData gd = (GridData) getLayoutData();
    gd.widthHint = badgeWidth;
    gd.heightHint = badgeHeight;

    addPaintListener(e -> {
      e.gc.setAntialias(SWT.ON);
      e.gc.setForeground(badgeColor);
      e.gc.drawRoundRectangle(0, 0, badgeWidth - 1, badgeHeight - 1,
          badgeHeight, badgeHeight);
      e.gc.drawText(displayText, HORIZONTAL_PADDING, VERTICAL_PADDING, true);
    });
  }

  private static Color getCategoryColor(Display display, String category) {
    if ("Powerful".equalsIgnoreCase(category)) {
      return CssConstants.getCategoryPowerfulColor(display);
    } else if ("Versatile".equalsIgnoreCase(category)) {
      return CssConstants.getCategoryVersatileColor(display);
    } else if ("Lightweight".equalsIgnoreCase(category)) {
      return CssConstants.getCategoryLightweightColor(display);
    }
    return null;
  }
}

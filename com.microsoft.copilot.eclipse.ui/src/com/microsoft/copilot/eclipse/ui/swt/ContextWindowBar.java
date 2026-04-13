// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

/**
 * A usage bar colored to match the {@link com.microsoft.copilot.eclipse.ui.chat.contextwindow.ContextSizeDonut} color
 * scheme:
 * <ul>
 * <li>Filled color when percentage &lt; 90%</li>
 * <li>Warning color when percentage &ge; 90%</li>
 * </ul>
 * The unused portion is rendered using the donut track color.
 */
public class ContextWindowBar extends AbstractUsageBar {

  private static final int THRESHOLD_WARNING = 90;

  private Color colorFilled;
  private Color colorWarning;
  private Color colorTrack;

  /**
   * Creates a new ContextWindowBar.
   *
   * @param parent the parent composite
   * @param style the SWT style bits
   */
  public ContextWindowBar(Composite parent, int style) {
    super(parent, style);
    colorFilled = CssConstants.getDonutFilledColor(getDisplay());
    colorWarning = CssConstants.getDonutWarningColor(getDisplay());
    colorTrack = CssConstants.getDonutTrackColor(getDisplay());
  }

  @Override
  protected Color getTrackColor() {
    return colorTrack;
  }

  @Override
  protected Color getFillColor() {
    if (getPercentage() >= THRESHOLD_WARNING) {
      return colorWarning;
    }
    return colorFilled;
  }
}

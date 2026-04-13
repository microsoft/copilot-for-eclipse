package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

/**
 * A usage bar colored for Copilot quota display:
 * <ul>
 * <li>Blue (#3574F0) when percentage &lt; 75%</li>
 * <li>Yellow (#FFB824) when 75% &le; percentage &lt; 90%</li>
 * <li>Red (#E05151) when percentage &ge; 90%</li>
 * </ul>
 * The unused portion is rendered in gray (#DFE1E5).
 */
public class CopilotUsageBar extends AbstractUsageBar {

  private static final int THRESHOLD_WARNING = 75;
  private static final int THRESHOLD_CRITICAL = 90;

  private Color colorActive;
  private Color colorApproaching;
  private Color colorExhausted;
  private Color colorRemaining;

  /**
   * Creates a new CopilotUsageBar.
   *
   * @param parent the parent composite
   * @param style the SWT style bits
   */
  public CopilotUsageBar(Composite parent, int style) {
    super(parent, style);
    colorActive = CssConstants.getUsageBarActiveColor(getDisplay());
    colorApproaching = CssConstants.getUsageBarApproachingColor(getDisplay());
    colorExhausted = CssConstants.getUsageBarExhaustedColor(getDisplay());
    colorRemaining = CssConstants.getUsageBarRemainingColor(getDisplay());
  }

  @Override
  protected Color getTrackColor() {
    return colorRemaining;
  }

  @Override
  protected Color getFillColor() {
    if (getPercentage() >= THRESHOLD_CRITICAL) {
      return colorExhausted;
    } else if (getPercentage() >= THRESHOLD_WARNING) {
      return colorApproaching;
    }
    return colorActive;
  }
}

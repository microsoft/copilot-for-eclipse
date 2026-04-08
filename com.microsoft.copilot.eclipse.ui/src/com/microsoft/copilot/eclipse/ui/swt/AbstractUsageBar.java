package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Abstract base for color-coded usage bars. Subclasses provide colors and threshold logic via {@link #getTrackColor()}
 * and {@link #getFillColor()}.
 *
 * <p>The bar renders a filled portion (left) over a track background (right), both with rounded
 * ends.
 */
public abstract class AbstractUsageBar extends Composite {

  private static final int BAR_HEIGHT = 4;

  private int percentage;
  private Canvas barCanvas;

  /**
   * Creates a new usage bar.
   *
   * @param parent the parent composite
   * @param style the SWT style bits
   */
  protected AbstractUsageBar(Composite parent, int style) {
    super(parent, style);
    this.percentage = 0;
    createControls();
  }

  private void createControls() {
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    setLayout(layout);

    barCanvas = new Canvas(this, SWT.DOUBLE_BUFFERED);
    GridData barGd = new GridData(SWT.FILL, SWT.NONE, true, false);
    barGd.heightHint = BAR_HEIGHT;
    barCanvas.setLayoutData(barGd);
    barCanvas.addPaintListener(this::paintBar);
  }

  /**
   * Sets the percentage value (0–100) and redraws the bar.
   *
   * @param percentage the percentage used, clamped to [0, 100]
   */
  public void setPercentage(int percentage) {
    this.percentage = Math.max(0, Math.min(100, percentage));
    barCanvas.redraw();
  }

  /**
   * Returns the current percentage value.
   */
  protected int getPercentage() {
    return percentage;
  }

  /**
   * Returns the background track color for the unused portion of the bar.
   */
  protected abstract Color getTrackColor();

  /**
   * Returns the fill color for the used portion of the bar based on the current percentage.
   */
  protected abstract Color getFillColor();

  private void paintBar(PaintEvent e) {
    GC gc = e.gc;
    gc.setAdvanced(true);
    gc.setAntialias(SWT.ON);

    Rectangle bounds = barCanvas.getClientArea();
    int width = bounds.width;
    int height = bounds.height;
    int arc = height;

    // Draw remaining background
    gc.setBackground(getTrackColor());
    gc.fillRoundRectangle(0, 0, width, height, arc, arc);

    // Draw filled portion
    if (percentage > 0) {
      int fillWidth = (int) Math.round(width * percentage / 100.0);
      fillWidth = Math.max(arc, Math.min(fillWidth, width));

      gc.setBackground(getFillColor());
      gc.fillRoundRectangle(0, 0, fillWidth, height, arc, arc);
    }
  }
}

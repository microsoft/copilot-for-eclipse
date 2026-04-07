package com.microsoft.copilot.eclipse.ui.chat.contextwindow;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ContextSizeInfo;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.AccessibilityUtils;

/**
 * A 16×16 donut chart that visualises the context-window token utilisation. The ring itself is 13×13 with a 2-pixel
 * stroke; the remaining pixels are margin. Hover shows a {@link ContextWindowPopup}.
 */
public class ContextSizeDonut {

  private static final int LINE_WIDTH = 2;
  private static final int RING_SIZE = 14;
  private static final int DONUT_SIZE = 16;
  private static final int FULL_SIZE = DONUT_SIZE + 2 * UiConstants.BTN_PADDING;

  private final Canvas canvas;
  private final ContextWindowPopup popup;
  private final ContextWindowService contextWindowService;

  /**
   * Creates the donut canvas as a child of {@code parent} and wires it to the given service.
   */
  public ContextSizeDonut(Composite parent, ContextWindowService contextWindowService) {
    this.contextWindowService = contextWindowService;
    this.popup = new ContextWindowPopup(contextWindowService);
    parent.addDisposeListener(e -> popup.dispose());
    canvas = new Canvas(parent, SWT.NONE);
    GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    gd.widthHint = FULL_SIZE;
    gd.heightHint = FULL_SIZE;
    canvas.setLayoutData(gd);

    canvas.addPaintListener(e -> {
      ContextSizeInfo info = contextWindowService.getState();
      if (info == null) {
        return;
      }
      int arcOffset = UiConstants.BTN_PADDING + (DONUT_SIZE - RING_SIZE) / 2 + LINE_WIDTH / 2;
      int arcSize = RING_SIZE - LINE_WIDTH;
      e.gc.setAntialias(SWT.ON);
      e.gc.setLineWidth(LINE_WIDTH);

      // Full ring background
      Color trackColor = CssConstants.getDonutTrackColor(e.display);
      e.gc.setForeground(trackColor);
      e.gc.drawArc(arcOffset, arcOffset, arcSize, arcSize, 0, 360);

      // Used portion starting from 12 o'clock (90°) going clockwise (negative angle)
      double pct = Math.min(info.utilizationPercentage(), 100.0);
      int filledAngle = (int) Math.round(pct / 100.0 * 360);
      if (filledAngle > 0) {
        Color filledColor = pct >= 90 ? CssConstants.getDonutWarningColor(e.display)
            : CssConstants.getDonutFilledColor(e.display);
        e.gc.setForeground(filledColor);
        e.gc.drawArc(arcOffset, arcOffset, arcSize, arcSize, 90, -filledAngle);
      }

    });

    canvas.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        popup.open(canvas);
      }
    });

    canvas.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.CR) {
          popup.open(canvas);
        }
      }
    });

    AccessibilityUtils.addFocusBorderToComposite(canvas);
    AccessibilityUtils.addAccessibilityNameForUiComponent(canvas, Messages.context_window_title);
    contextWindowService.bindContextSizeDonut(canvas);
    canvas.requestLayout();
  }
}

package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A label that supports word wrap and auto resize when the viewport changes.
 */
public class WrapLabel {
  private Label label;
  private GridData gridData;

  /**
   * Create a new wrap label.
   */
  public WrapLabel(Composite parent, int style) {
    label = new Label(parent, style | SWT.WRAP);
    gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
    label.setLayoutData(gridData);

    parent.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        updateWidth();
      }
    });
  }

  /**
   * Set the text of the label.
   */
  public void setText(String text) {
    label.setText(text);
  }

  /**
   * Set the text color of the label.
   */
  public void setForeground(Color color) {
    label.setForeground(color);
  }

  /**
   * Set the font of the label.
   */
  public void setFont(Font font) {
    label.setFont(font);
  }

  /**
   * Get the location of the label.
   */
  public Point getLocation() {
    return label.getLocation();
  }

  public void setHorizontalIndent(int horizontalIndent) {
    gridData.horizontalIndent = horizontalIndent;
  }

  /**
   * Set the layout data of the label.
   */
  public void setLayoutData(GridData layoutData) {
    label.setLayoutData(layoutData);
  }

  /**
   * Set the dispose listener of the label.
   */
  public void addDisposeListener(DisposeListener listener) {
    label.addDisposeListener(listener);
  }

  private void updateWidth() {
    Composite parent = label.getParent();
    int parentWidth = parent.getClientArea().width;
    if (parent.getLayout() instanceof GridLayout parentLayout) {
      // minus the margin for left and right, 5 for each.
      parentWidth -= parentLayout.marginWidth * 2;
    }
    gridData.widthHint = parentWidth - 10;
    parent.layout(true);
  }
}

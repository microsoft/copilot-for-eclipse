package com.microsoft.copilot.eclipse.ui.chat.viewers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A chat viewer that displays information about the chat.
 */
public abstract class BaseViewer extends Composite {
  public static final int ALIGNED_WIDTH = 300;
  public static int ALIGNED_MARGIN_TOP = 10;

  BaseViewer(Composite parent, int style) {
    super(parent, style);
    setLayout(new GridLayout(1, true));
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
  }

  /**
   * Build a label with an icon.
   *
   * @param parent the parent composite
   * @param labelIcon the label icon image
   * @param text the text for the label
   */
  protected void buildLabelWithIcon(Composite parent, Image labelIcon, String text) {
    Composite composite = new Composite(parent, SWT.CENTER);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
    Label icon = new Label(composite, SWT.RIGHT);
    icon.setImage(labelIcon);

    Label label = new Label(composite, SWT.CENTER);
    label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
    label.setText(text);
    label.setForeground((parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY)));
  }

  /**
   * Build a composite with a margin top.
   *
   * @param parent the parent composite
   * @param marginTop the margin top
   * @param style the style
   * @return the composite
   */
  protected Composite buildCompositeWithMarginTop(Composite parent, int marginTop, int style) {
    Composite composite = new Composite(parent, style);
    GridLayout compositelayout = new GridLayout(1, true);
    compositelayout.marginTop = marginTop;
    composite.setLayout(compositelayout);
    composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
    return composite;
  }
}

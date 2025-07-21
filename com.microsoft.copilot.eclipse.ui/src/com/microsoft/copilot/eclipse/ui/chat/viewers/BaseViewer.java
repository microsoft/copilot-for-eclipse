package com.microsoft.copilot.eclipse.ui.chat.viewers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

/**
 * A chat viewer that displays information about the chat.
 */
public abstract class BaseViewer extends Composite {
  public static final int ALIGNED_WIDTH = 250;
  public static final int ALIGNED_TITLE_HEIGHT = 14;
  public static final int ALIGNED_LABEL_WIDTH = 370;
  public static final int ALIGNED_SPACE_BETWEEN_ICON_AND_LABEL = 10;
  public static final int ALIGNED_MARGIN_TOP = 12;
  public static final int SHOW_NARROW_VIEW_HEIGHT_THRESHOLD = 500;

  BaseViewer(Composite parent, int style) {
    super(parent, style);
    setLayout(new GridLayout(1, true));
    setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
  }

  /**
   * Build a label with an icon.
   *
   * @param parent the parent composite
   * @param labelIcon the label icon image
   * @param text the text for the label
   */
  protected void buildLabelWithIcon(Composite parent, Image labelIcon, String text) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gl = new GridLayout(2, false);
    gl.horizontalSpacing = ALIGNED_SPACE_BETWEEN_ICON_AND_LABEL;
    composite.setLayout(gl);
    composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
    Label icon = new Label(composite, SWT.RIGHT);
    icon.setImage(labelIcon);

    Label label = new Label(composite, SWT.LEFT);
    label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
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

  /**
   * Build a text link with a listener.
   *
   * @param parent the parent composite
   * @param text the text for the link
   * @param gridData the layout data for the link
   * @param listener the listener to be added to the link
   */
  protected void buildTextWithLinkAndListener(Composite parent, String text, GridData gridData, Listener listener) {
    Link link = new Link(parent, SWT.CENTER);
    link.setText(text);
    link.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
    link.setLayoutData(gridData != null ? gridData : new GridData(SWT.CENTER, SWT.CENTER, true, false));
    link.addListener(SWT.Selection, listener);
  }
}

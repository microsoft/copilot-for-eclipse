// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A composite that displays a note with a bold prefix label and wrappable content label. The component automatically
 * resizes when the parent composite is resized.
 */
public class WrappableNoteLabel extends Composite {

  private static final int DEFAULT_WIDTH_HINT = 400;
  private static final int DEFAULT_MARGIN = 20;

  private Label prefixLabel;
  private Label contentLabel;
  private Composite parentToWatch;
  private int widthMargin;
  private String prefixText;
  private String contentText;
  private Font boldFont;

  /**
   * Creates a new WrappableNoteLabel with default layout settings.
   *
   * @param parent the parent composite
   * @param prefixText the bold prefix text (e.g., "Note:")
   * @param contentText the wrappable content text
   */
  public WrappableNoteLabel(Composite parent, String prefixText, String contentText) {
    this(parent, prefixText, contentText, DEFAULT_MARGIN);
  }

  /**
   * Creates a new WrappableNoteLabel with custom width margin.
   *
   * @param parent the parent composite
   * @param prefixText the bold prefix text (e.g., "Note:")
   * @param contentText the wrappable content text
   * @param widthMargin the margin to subtract from parent width when resizing
   */
  public WrappableNoteLabel(Composite parent, String prefixText, String contentText, int widthMargin) {
    super(parent, SWT.NONE);
    this.widthMargin = widthMargin;
    this.parentToWatch = parent;
    this.prefixText = prefixText;
    this.contentText = contentText;

    createControls();
    setupResizeListener();
  }

  /**
   * Creates the prefix and content labels with appropriate styling.
   */
  private void createControls() {
    GridLayout layout = new GridLayout(2, false);
    layout.marginLeft = -3;
    layout.marginBottom = 1;
    layout.horizontalSpacing = 0;
    setLayout(layout);
    setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    // Create prefix label (bold)
    prefixLabel = new Label(this, SWT.NONE);
    prefixLabel.setText(prefixText);

    // Create bold font for prefix
    FontDescriptor boldDescriptor = FontDescriptor.createFrom(prefixLabel.getFont()).setStyle(SWT.BOLD);
    boldFont = boldDescriptor.createFont(prefixLabel.getDisplay());
    prefixLabel.setFont(boldFont);

    GridData prefixData = new GridData(SWT.LEFT, SWT.TOP, false, false);
    prefixLabel.setLayoutData(prefixData);

    // Create content label (wrappable)
    contentLabel = new Label(this, SWT.WRAP);
    contentLabel.setText(contentText);

    GridData contentData = new GridData(SWT.FILL, SWT.TOP, true, false);
    contentData.widthHint = DEFAULT_WIDTH_HINT;
    contentLabel.setLayoutData(contentData);

    // Dispose font when composite is disposed
    addDisposeListener(e -> {
      if (boldFont != null && !boldFont.isDisposed()) {
        boldFont.dispose();
      }
    });
  }

  /**
   * Sets up the resize listener to dynamically adjust the content label width.
   */
  private void setupResizeListener() {
    parentToWatch.addControlListener(ControlListener.controlResizedAdapter(e -> updateContentLabelWidth()));
  }

  /**
   * Updates the content label width based on the parent's current size.
   */
  private void updateContentLabelWidth() {
    if (contentLabel != null && !contentLabel.isDisposed() && prefixLabel != null && !prefixLabel.isDisposed()
        && parentToWatch != null && !parentToWatch.isDisposed()) {

      int prefixWidth = prefixLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
      int availableWidth = parentToWatch.getSize().x - widthMargin - prefixWidth - 10; // 10 for spacing and margins

      GridData contentData = new GridData(SWT.FILL, SWT.TOP, true, false);
      contentData.widthHint = Math.max(100, availableWidth); // Minimum width of 100
      contentLabel.setLayoutData(contentData);

      parentToWatch.requestLayout();
    }
  }

  /**
   * Sets the content text of the note.
   *
   * @param text the new content text
   */
  public void setContentText(String text) {
    this.contentText = text;
    if (contentLabel != null && !contentLabel.isDisposed()) {
      contentLabel.setText(text);
    }
  }

  /**
   * Gets the content text of the note.
   *
   * @return the current content text
   */
  public String getContentText() {
    return contentText;
  }

  /**
   * Sets the prefix text of the note.
   *
   * @param text the new prefix text
   */
  public void setPrefixText(String text) {
    this.prefixText = text;
    if (prefixLabel != null && !prefixLabel.isDisposed()) {
      prefixLabel.setText(text);
    }
  }

  /**
   * Gets the prefix text of the note.
   *
   * @return the current prefix text
   */
  public String getPrefixText() {
    return prefixText;
  }

  /**
   * Gets the prefix label for direct access if needed.
   *
   * @return the prefix label widget
   */
  public Label getPrefixLabel() {
    return prefixLabel;
  }

  /**
   * Gets the content label for direct access if needed.
   *
   * @return the content label widget
   */
  public Label getContentLabel() {
    return contentLabel;
  }
}
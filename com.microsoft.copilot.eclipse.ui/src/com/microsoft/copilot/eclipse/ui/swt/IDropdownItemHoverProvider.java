package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.swt.widgets.Composite;

/**
 * Configures the hover UI shown when the user hovers over a {@link DropdownItem}.
 *
 * <p>The hover shell is a lightweight {@code SWT.NO_TRIM | SWT.ON_TOP} window positioned beside
 * the popup and dismissed when the cursor leaves the item.
 */
public interface IDropdownItemHoverProvider {

  /**
   * Configures the given hover composite for the specified item.
   *
   * <p>Implementations may populate the composite with child widgets and adjust the composite
   * itself. The composite uses a {@link org.eclipse.swt.layout.GridLayout} with one column by
   * default; add or update layout data as needed.
   *
   * @param parent the parent composite inside the hover shell
   * @param item the item being hovered
   */
  void configureHover(Composite parent, DropdownItem item);
}
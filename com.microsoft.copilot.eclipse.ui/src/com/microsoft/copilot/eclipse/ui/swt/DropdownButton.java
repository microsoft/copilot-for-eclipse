// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.utils.AccessibilityUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A custom-painted dropdown button composite.
 *
 * <p>Displays the currently selected item label and a dropdown arrow.
 * Clicking (or pressing Space / Enter / &darr;) opens a {@link DropdownPopup} showing the
 * configured {@link DropdownItemGroup}s.
 *
 * <p>Basic usage:
 * <pre>
 * DropdownButton btn = new DropdownButton(parent, SWT.NONE);
 * btn.setItemGroups(groups);
 * btn.setSelectedItemId("agent");
 * btn.setSelectionListener(id -&gt; handleSelection(id));
 * </pre>
 */
public class DropdownButton extends Composite {

  private static final int ICON_TEXT_GAP = 4;
  private static final int H_PADDING = 4;
  private static final int ARROW_AREA_WIDTH = 16;

  private static Image arrowIcon;

  private final DropdownPopup popup;
  private List<DropdownItemGroup> itemGroups;
  private String selectedItemId;
  private boolean mouseHover;

  /**
   * Creates a new dropdown button.
   *
   * @param parent the parent composite
   * @param style SWT style bits
   */
  public DropdownButton(Composite parent, int style) {
    super(parent, style | SWT.NONE);
    popup = new DropdownPopup(getShell(), this);

    if (arrowIcon == null || arrowIcon.isDisposed()) {
      arrowIcon = UiUtils.isDarkTheme()
          ? UiUtils.buildImageFromPngPath("/icons/dropdown/down_arrow_dark.png")
          : UiUtils.buildImageFromPngPath("/icons/dropdown/down_arrow.png");
      getDisplay().addListener(SWT.Dispose, e -> disposeStaticIcons());
    }

    addPaintListener(e -> paintControl(e.gc));

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        togglePopup();
      }
    });

    addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        mouseHover = true;
        redraw();
      }

      @Override
      public void mouseExit(MouseEvent e) {
        mouseHover = false;
        redraw();
      }
    });

    setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    AccessibilityUtils.addFocusBorderToComposite(this);

    // Allow keyboard activation
    addListener(SWT.KeyDown, event -> {
      if (event.keyCode == SWT.CR) {
        togglePopup();
      }
    });

    addDisposeListener(e -> popup.close());
  }

  /**
   * Sets the item groups displayed in the popup.
   *
   * @param itemGroups the groups to show
   */
  public void setItemGroups(List<DropdownItemGroup> itemGroups) {
    this.itemGroups = itemGroups;
    redraw();
  }

  /**
   * Sets the id of the currently selected item.
   *
   * <p>The button displays the label of the item with this id. If no matching item is found the
   * display text is empty.
   *
   * @param selectedItemId the selected item id
   */
  public void setSelectedItemId(String selectedItemId) {
    this.selectedItemId = selectedItemId;
    updateWidthHint();
    redraw();
  }

  /**
   * Returns the id of the currently selected item, or {@code null}.
   *
   * @return the selected item id
   */
  public String getSelectedItemId() {
    return selectedItemId;
  }

  /**
   * Sets the listener called when the user selects an item, replacing any previously set listener.
   *
   * <p>Items with an {@code onAction} Runnable do <em>not</em> trigger this listener.
   *
   * @param listener a consumer receiving the selected item id
   */
  public void setSelectionListener(Consumer<String> listener) {
    popup.setSelectionListener(listener);
  }

  /**
   * Sets the accessibility name used by screen readers.
   *
   * @param name the accessible name
   */
  public void setAccessibilityName(String name) {
    AccessibilityUtils.addAccessibilityNameForUiComponent(this, name);
  }

  private static void disposeStaticIcons() {
    if (arrowIcon != null && !arrowIcon.isDisposed()) {
      arrowIcon.dispose();
      arrowIcon = null;
    }
  }

  private void togglePopup() {
    if (popup.isOpen()) {
      popup.close();
    } else {
      openPopup();
    }
  }

  private void openPopup() {
    if (isDisposed() || itemGroups == null || itemGroups.isEmpty()) {
      return;
    }
    Rectangle bounds = getBounds();
    Point screenPos = toDisplay(0, bounds.height);
    popup.open(screenPos, itemGroups, selectedItemId, bounds.height);
  }

  private void paintControl(GC gc) {
    Rectangle bounds = getClientArea();
    Display display = getDisplay();
    DropdownItem selected = findItemById(selectedItemId);
    Image selectedIcon = getSelectedItemIcon(selected);
    Color bg = mouseHover ? CssConstants.getButtonFocusBgColor(display) : getBackground();
    gc.setBackground(bg);
    gc.fillRectangle(bounds);

    gc.setForeground(getForeground());
    String text = selected != null ? selected.getLabel() : "";
    Point textExtent = gc.textExtent(text);
    int contentHeight = getContentHeight(textExtent, selectedIcon, arrowIcon);
    int contentTop = Math.max(0, (bounds.height - contentHeight) / 2);
    int textX = H_PADDING;
    if (selectedIcon != null) {
      Rectangle selectedIconBounds = selectedIcon.getBounds();
      int iconY = contentTop + (contentHeight - selectedIconBounds.height) / 2;
      gc.drawImage(selectedIcon, textX, iconY);
      textX += selectedIconBounds.width + ICON_TEXT_GAP;
    }
    int textY = contentTop + (contentHeight - textExtent.y) / 2;
    gc.drawText(text, textX, textY, true);

    // Dropdown arrow icon
    if (arrowIcon != null && !arrowIcon.isDisposed()) {
      Rectangle arrowBounds = arrowIcon.getBounds();
      int arrowX = textX + textExtent.x;
      int arrowY = contentTop + (contentHeight - arrowBounds.height) / 2;
      gc.drawImage(arrowIcon, arrowX, arrowY);
    }
  }

  private DropdownItem findItemById(String id) {
    if (id == null || itemGroups == null) {
      return null;
    }
    for (DropdownItemGroup group : itemGroups) {
      for (DropdownItem item : group.getItems()) {
        if (!item.isSeparator() && id.equals(item.getId())) {
          return item;
        }
      }
    }
    return null;
  }

  @Override
  public Point computeSize(int widthHint, int heightHint, boolean changed) {
    GC gc = new GC(this);
    try {
      DropdownItem selected = findItemById(selectedItemId);
      String text = selected != null ? selected.getLabel() : "";
      Point textExtent = gc.textExtent(text.isEmpty() ? "M" : text);
      Image selectedIcon = getSelectedItemIcon(selected);
      int iconWidth = 0;
      if (selectedIcon != null) {
        iconWidth = selectedIcon.getBounds().width + ICON_TEXT_GAP;
      }
      Image arrow = arrowIcon;
      int arrowWidth = arrowIcon != null && !arrowIcon.isDisposed()
          ? arrowIcon.getBounds().width : ARROW_AREA_WIDTH;
      int width = H_PADDING + iconWidth + textExtent.x + arrowWidth;
      int height = getContentHeight(textExtent, selectedIcon, arrow) + 2 * UiConstants.BTN_PADDING;
      if (widthHint != SWT.DEFAULT) {
        width = Math.max(width, widthHint);
      }
      if (heightHint != SWT.DEFAULT) {
        height = Math.max(height, heightHint);
      }
      return new Point(width, height);
    } finally {
      gc.dispose();
    }
  }

  private Image getSelectedItemIcon(DropdownItem selected) {
    if (selected == null) {
      return null;
    }
    Image icon = selected.getIcon();
    return icon != null && !icon.isDisposed() ? icon : null;
  }

  private int getContentHeight(Point textExtent, Image selectedIcon, Image arrow) {
    int contentHeight = textExtent.y;
    if (selectedIcon != null) {
      contentHeight = Math.max(contentHeight, selectedIcon.getBounds().height);
    }
    if (arrow != null && !arrow.isDisposed()) {
      contentHeight = Math.max(contentHeight, arrow.getBounds().height);
    }
    return contentHeight;
  }

  private void updateWidthHint() {
    if (isDisposed()) {
      return;
    }
    if (!(getLayoutData() instanceof GridData gridData)) {
      return;
    }
    Point preferred = computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
    if (gridData.widthHint != preferred.x) {
      gridData.widthHint = preferred.x;
      requestLayout();
    }
  }
}

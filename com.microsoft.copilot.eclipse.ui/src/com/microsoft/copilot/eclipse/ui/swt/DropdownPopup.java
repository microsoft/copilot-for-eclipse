// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A generic popup shell for {@link DropdownButton}.
 *
 * <p>Renders {@link DropdownItemGroup}s with optional headers, automatic separators between groups,
 * and per-item layout of icon, label, suffix, and hover content. Supports keyboard navigation with
 * arrow keys and Enter.
 */
class DropdownPopup {

  private static final int POPUP_MARGIN = 2;
  private static final int ITEM_H_PADDING = 8;
  private static final int ITEM_V_PADDING = 4;
  private static final int SEPARATOR_V_PADDING = 2;
  private static final int ICON_TEXT_GAP = 6;
  private static final int LABEL_SUFFIX_GAP = 12;
  private static final int BORDER_ARC = 8;
  private static final int ITEM_FOCUS_ARC = 6;
  private static final int MAX_VISIBLE_ITEMS = 15;
  private static final int SHORT_POPUP_WIDTH = 230;
  private static final int LONG_POPUP_WIDTH = 300;

  private static Image checkIcon;

  private Shell shell;
  private final Shell parentShell;
  private Consumer<String> selectionListener;
  private String selectedItemId;

  private record ItemEntry(DropdownItem item, Composite composite) {}

  private final List<ItemEntry> items = new ArrayList<>();
  private int focusedIndex = -1;
  private Listener keyboardFilter;

  private Shell hoverShell;
  private ScrolledComposite scrolledComposite;
  private final IStylingEngine stylingEngine;
  private final Control anchorControl;

  private static final String POPUP_ITEM_DEFAULT_ID = "popup-item-default";
  private static final String POPUP_ITEM_FOCUSED_ID = "popup-item-focused";
  private static final String POPUP_ITEM_SELECTED_ID = "popup-item-selected";
  private static final String POPUP_SECONDARY_TEXT_CLASS = "popup-secondary-text";
  private static final String POPUP_ACTION_TEXT_CLASS = "popup-action-text";
  private static final String POPUP_ITEM_BASE_ID_KEY = "dropdownPopupItemBaseId";
  private static final String POPUP_ITEM_FOCUSED_KEY = "dropdownPopupItemFocused";

  DropdownPopup(Shell parentShell, Control anchorControl) {
    this.parentShell = parentShell;
    this.anchorControl = anchorControl;
    this.stylingEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);

    if (checkIcon == null || checkIcon.isDisposed()) {
      checkIcon = UiUtils.isDarkTheme()
          ? UiUtils.buildImageFromPngPath("/icons/dropdown/dropdown_complete_status_dark.png")
          : UiUtils.buildImageFromPngPath("/icons/dropdown/dropdown_complete_status.png");
      parentShell.getDisplay().addListener(SWT.Dispose, e -> disposeStaticIcons());
    }
  }

  void setSelectionListener(Consumer<String> listener) {
    this.selectionListener = listener;
  }

  private static void disposeStaticIcons() {
    if (checkIcon != null && !checkIcon.isDisposed()) {
      checkIcon.dispose();
      checkIcon = null;
    }
  }

  /**
   * Opens the popup at the given screen location with the provided item groups.
   *
   * @param location the screen position for the top-left of the popup
   * @param groups the item groups to display
   * @param selectedItemId the id of the currently selected item, or {@code null}
   * @param anchorHeight the height of the anchor control that triggered this popup, used to avoid overlapping it when
   *     the popup flips above
   */
  void open(Point location, List<DropdownItemGroup> groups, String selectedItemId, int anchorHeight) {
    if (shell != null && !shell.isDisposed()) {
      close();
    }
    this.selectedItemId = selectedItemId;

    shell = new Shell(parentShell, SWT.NO_TRIM | SWT.ON_TOP);
    shell.setData(CssConstants.CSS_ID_KEY, "dropdown-popup");

    final Display display = shell.getDisplay();
    // Set background explicitly to ensure the shell is opaque from the first composite,
    // rather than relying solely on async CSS styling which can cause bleed-through.
    Color popupBg = CssConstants.getPopupBgColor(display);
    shell.setBackground(popupBg);
    styleControl(shell);

    GridLayout shellLayout = new GridLayout(1, false);
    shellLayout.marginWidth = POPUP_MARGIN;
    shellLayout.marginHeight = POPUP_MARGIN;
    shellLayout.verticalSpacing = 0;
    shell.setLayout(shellLayout);

    scrolledComposite = new ScrolledComposite(shell, SWT.V_SCROLL);
    scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);
    scrolledComposite.setData(CssConstants.CSS_ID_KEY, "dropdown-popup");
    scrolledComposite.setBackground(popupBg);
    styleControl(scrolledComposite);

    Composite container = new Composite(scrolledComposite, SWT.NONE);
    container.setData(CssConstants.CSS_ID_KEY, "dropdown-popup");
    container.setBackground(popupBg);
    styleControl(container);
    GridLayout containerLayout = new GridLayout(1, false);
    containerLayout.marginWidth = 0;
    containerLayout.marginHeight = 0;
    containerLayout.verticalSpacing = 0;
    container.setLayout(containerLayout);

    scrolledComposite.setContent(container);

    items.clear();
    focusedIndex = -1;
    populateGroups(container, groups);

    Point contentSize = container.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    container.setSize(contentSize);
    scrolledComposite.setMinSize(contentSize);

    // Set keyboard focus on the currently selected item
    for (int i = 0; i < items.size(); i++) {
      if (selectedItemId != null && selectedItemId.equals(items.get(i).item().getId())) {
        focusedIndex = i;
        updateFocusBorder(focusedIndex, true);
        break;
      }
    }

    final Color borderColor = CssConstants.getBorderColor(display);
    shell.addPaintListener(e -> {
      Rectangle bounds = shell.getClientArea();
      e.gc.setForeground(borderColor);
      e.gc.setLineWidth(1);
      e.gc.drawRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1, BORDER_ARC, BORDER_ARC);
    });

    shell.addListener(SWT.Deactivate, e -> {
      if (anchorControl != null && !anchorControl.isDisposed() && isCursorInsideControl(anchorControl)) {
        return;
      }
      close();
    });

    shell.addListener(SWT.Traverse, e -> {
      if (e.detail == SWT.TRAVERSE_ESCAPE) {
        close();
        e.doit = false;
      }
    });

    keyboardFilter = e -> {
      if (shell == null || shell.isDisposed() || !shell.isVisible()) {
        return;
      }
      if (e.keyCode == SWT.ARROW_DOWN) {
        moveFocus(1);
        e.doit = false;
      } else if (e.keyCode == SWT.ARROW_UP) {
        moveFocus(-1);
        e.doit = false;
      } else if ((e.keyCode == SWT.CR || e.keyCode == SWT.LF) && focusedIndex >= 0) {
        activateFocusedItem();
        e.doit = false;
      }
    };
    display.addFilter(SWT.KeyDown, keyboardFilter);

    shell.addDisposeListener(e -> {
      if (keyboardFilter != null) {
        display.removeFilter(SWT.KeyDown, keyboardFilter);
        keyboardFilter = null;
      }
    });

    shell.pack();
    constrainHeightIfNeeded();
    adjustBounds(location, anchorHeight);
    scrollToFocusedItem();
    shell.setVisible(true);
    shell.setFocus();
  }

  private void constrainHeightIfNeeded() {
    if (items.size() <= MAX_VISIBLE_ITEMS || items.isEmpty()) {
      return;
    }
    int lastIdx = Math.min(MAX_VISIBLE_ITEMS, items.size()) - 1;
    Composite lastVisible = items.get(lastIdx).composite();
    Rectangle lastBounds = lastVisible.getBounds();
    int maxContentHeight = lastBounds.y + lastBounds.height;

    int scrollBarWidth = 0;
    if (scrolledComposite.getVerticalBar() != null) {
      scrollBarWidth = scrolledComposite.getVerticalBar().getSize().x;
    }

    Point shellSize = shell.getSize();
    int newHeight = maxContentHeight + 2 * POPUP_MARGIN;
    shell.setSize(shellSize.x + scrollBarWidth, newHeight);
  }

  private void adjustBounds(Point location, int anchorHeight) {
    Point size = shell.getSize();
    Rectangle screen = getMonitorBounds(shell.getDisplay(), location);
    int x = location.x;
    int y = location.y;
    if (x + size.x > screen.x + screen.width) {
      x = screen.x + screen.width - size.x;
    }
    if (y + size.y > screen.y + screen.height) {
      y = location.y - anchorHeight - size.y;
    }
    if (x < screen.x) {
      x = screen.x;
    }
    if (y < screen.y) {
      y = screen.y;
    }
    shell.setLocation(x, y);
  }

  private static Rectangle getMonitorBounds(Display display, Point location) {
    for (Monitor monitor : display.getMonitors()) {
      if (monitor.getBounds().contains(location)) {
        return monitor.getBounds();
      }
    }
    return display.getPrimaryMonitor().getBounds();
  }

  private void populateGroups(Composite container, List<DropdownItemGroup> groups) {
    boolean firstNonEmptyGroup = true;
    for (DropdownItemGroup group : groups) {
      if (group.getItems().isEmpty()) {
        continue;
      }
      if (!firstNonEmptyGroup) {
        addSeparator(container);
      }
      firstNonEmptyGroup = false;
      if (group.getHeader() != null) {
        addGroupHeader(container, group.getHeader());
      }
      for (DropdownItem item : group.getItems()) {
        if (item.isSeparator()) {
          addSeparator(container);
        } else {
          addItem(container, item);
        }
      }
    }
  }

  private void addGroupHeader(Composite parent, String text) {
    Composite headerComp = new Composite(parent, SWT.NONE);
    headerComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = ITEM_H_PADDING;
    layout.marginHeight = SEPARATOR_V_PADDING;
    headerComp.setLayout(layout);
    Label headerLabel = new Label(headerComp, SWT.NONE);
    headerLabel.setText(text);
    headerLabel.setData(CssConstants.CSS_ID_KEY, POPUP_ITEM_DEFAULT_ID);
    setCssClassOnly(headerLabel, POPUP_SECONDARY_TEXT_CLASS);
    headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    applyCssIdRecursively(headerComp, POPUP_ITEM_DEFAULT_ID);
  }

  private void addSeparator(Composite parent) {
    final Color separatorColor = CssConstants.getSeparatorColor(parent.getDisplay());
    Composite separator = new Composite(parent, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd.heightHint = 1;
    gd.verticalIndent = SEPARATOR_V_PADDING;
    separator.setLayoutData(gd);
    separator.addPaintListener(e -> {
      Rectangle r = separator.getClientArea();
      e.gc.setBackground(separatorColor);
      e.gc.fillRectangle(0, 0, r.width, 1);
    });
  }

  private void addItem(Composite parent, DropdownItem item) {
    final Display display = parent.getDisplay();
    final boolean isSelected = item.getId() != null && item.getId().equals(selectedItemId);
    final String itemBaseCssId = isSelected ? POPUP_ITEM_SELECTED_ID : POPUP_ITEM_DEFAULT_ID;

    Composite itemComp = new Composite(parent, SWT.NONE);
    itemComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    GridLayout itemLayout = new GridLayout(3, false);
    itemLayout.marginWidth = ITEM_H_PADDING;
    itemLayout.marginHeight = ITEM_V_PADDING;
    itemLayout.horizontalSpacing = ICON_TEXT_GAP;
    itemComp.setLayout(itemLayout);
    itemComp.addPaintListener(e -> paintFocusedItemBackground(itemComp, e.gc));

    items.add(new ItemEntry(item, itemComp));
    final int itemIndex = items.size() - 1;

    List<Control> controls = new ArrayList<>();
    controls.add(itemComp);

    // Leading icon column: show a selection indicator (checkIcon) for the selected item, or the item's icon otherwise.
    // Always rendered so that item label text aligns across all items (aligned with group headers).
    Label leadingLabel = new Label(itemComp, SWT.NONE);
    GridData leadingGd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    Image leadingIcon = resolvePopupLeadingIcon(item, isSelected);
    if (leadingIcon != null) {
      leadingLabel.setImage(leadingIcon);
    } else {
      leadingGd.widthHint = 16; // empty placeholder — matches typical 16 px icon width
    }
    leadingLabel.setLayoutData(leadingGd);
    controls.add(leadingLabel);

    // Label: item display name
    Label nameLabel = new Label(itemComp, SWT.NONE);
    nameLabel.setText(item.getLabel());
    nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    if (!item.isEnabled()) {
      nameLabel.setData(CssConstants.CSS_ID_KEY, itemBaseCssId);
      setCssClassOnly(nameLabel, POPUP_SECONDARY_TEXT_CLASS);
    } else if (item.getOnAction() != null) {
      nameLabel.setData(CssConstants.CSS_ID_KEY, itemBaseCssId);
      setCssClassOnly(nameLabel, POPUP_ACTION_TEXT_CLASS);
    }
    controls.add(nameLabel);

    // Suffix: item suffix text
    Label suffixLabel = new Label(itemComp, SWT.NONE);
    suffixLabel.setText(item.getSuffix() != null ? item.getSuffix() : "");
    GridData suffixGd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    suffixGd.horizontalIndent = LABEL_SUFFIX_GAP;
    suffixLabel.setLayoutData(suffixGd);
    suffixLabel.setData(CssConstants.CSS_ID_KEY, itemBaseCssId);
    setCssClassOnly(suffixLabel, POPUP_SECONDARY_TEXT_CLASS);
    controls.add(suffixLabel);

    setPopupItemState(itemComp, itemBaseCssId, false);

    String tooltip = item.getTooltip();
    MouseTrackAdapter hoverTracker = buildHoverTracker(item, itemComp, itemIndex);
    MouseAdapter clickListener = item.isEnabled() ? buildClickListener(item) : null;
    for (Control c : controls) {
      if (tooltip != null) {
        c.setToolTipText(tooltip);
      }
      c.addMouseTrackListener(hoverTracker);
      if (clickListener != null) {
        c.addMouseListener(clickListener);
        c.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
      }
    }
  }

  private MouseTrackAdapter buildHoverTracker(DropdownItem item, Composite itemComp, int itemIndex) {
    return new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        boolean alreadyFocused = itemIndex == focusedIndex;
        if (!itemComp.isDisposed() && item.isEnabled()) {
          if (!alreadyFocused) {
            if (focusedIndex >= 0) {
              updateFocusBorder(focusedIndex, false);
            }
            focusedIndex = itemIndex;
          }
          updateFocusBorder(focusedIndex, true);
        }
        if (item.getHoverProvider() != null
            && (!alreadyFocused || hoverShell == null || hoverShell.isDisposed())) {
          openHoverShell(item, itemComp);
        }
      }

      @Override
      public void mouseExit(MouseEvent e) {
        if (!itemComp.isDisposed() && isCursorInsideControl(itemComp)) {
          return;
        }
        if (!itemComp.isDisposed() && itemIndex == focusedIndex) {
          focusedIndex = -1;
          updateFocusBorder(itemIndex, false);
        }
        closeHoverShell();
      }
    };
  }

  private static boolean isCursorInsideControl(Control control) {
    Point cursor = control.getDisplay().getCursorLocation();
    Point loc = control.toDisplay(0, 0);
    Point size = control.getSize();
    return cursor.x >= loc.x && cursor.x < loc.x + size.x && cursor.y >= loc.y && cursor.y < loc.y + size.y;
  }

  private MouseAdapter buildClickListener(DropdownItem item) {
    return new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        closeHoverShell();
        if (item.getOnAction() != null) {
          close();
          item.getOnAction().run();
        } else {
          close();
          if (selectionListener != null && item.getId() != null) {
            selectionListener.accept(item.getId());
          }
        }
      }
    };
  }

  private Image resolvePopupLeadingIcon(DropdownItem item, boolean selected) {
    if (selected && checkIcon != null && !checkIcon.isDisposed()) {
      return checkIcon;
    }
    Image icon = item.getIcon();
    return icon != null && !icon.isDisposed() ? icon : null;
  }

  private void openHoverShell(DropdownItem item, Composite anchorItem) {
    closeHoverShell();
    if (shell == null || shell.isDisposed()) {
      return;
    }
    hoverShell = new Shell(shell, SWT.NO_TRIM | SWT.ON_TOP);
    hoverShell.setData(CssConstants.CSS_ID_KEY, "dropdown-popup");
    final Display display = hoverShell.getDisplay();
    Color popupBg = CssConstants.getPopupBgColor(display);
    hoverShell.setBackground(popupBg);
    styleControl(hoverShell);
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = ITEM_H_PADDING;
    layout.marginHeight = ITEM_V_PADDING;
    hoverShell.setLayout(layout);

    Composite hoverContent = new Composite(hoverShell, SWT.NONE);
    hoverContent.setData(CssConstants.CSS_ID_KEY, "dropdown-popup");
    hoverContent.setBackground(popupBg);
    styleControl(hoverContent);
    hoverContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout contentLayout = new GridLayout(1, false);
    contentLayout.marginWidth = 0;
    contentLayout.marginHeight = 0;
    contentLayout.marginTop = ITEM_V_PADDING;
    contentLayout.marginBottom = ITEM_V_PADDING;
    hoverContent.setLayout(contentLayout);

    item.getHoverProvider().configureHover(hoverContent, item);

    final Color borderColor = CssConstants.getBorderColor(display);
    hoverShell.addPaintListener(e -> {
      Rectangle bounds = hoverShell.getClientArea();
      e.gc.setForeground(borderColor);
      e.gc.setLineWidth(1);
      e.gc.drawRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1, BORDER_ARC, BORDER_ARC);
    });

    hoverShell.pack();
    Point hoverSize = hoverShell.getSize();
    int width = hoverSize.x <= SHORT_POPUP_WIDTH ? SHORT_POPUP_WIDTH : LONG_POPUP_WIDTH;
    if (width != hoverSize.x) {
      // Recompute size at the target width so wrapped labels lay out correctly.
      hoverSize = hoverShell.computeSize(width, SWT.DEFAULT);
      hoverSize.x = width;
      hoverShell.setSize(hoverSize);
    }

    // Position the hover shell beside the popup, aligned with the hovered item
    Rectangle popupBounds = shell.getBounds();
    Point itemLoc = display.map(anchorItem, null, 0, 0);
    Rectangle screen = getMonitorBounds(display, new Point(popupBounds.x, popupBounds.y));

    int x;
    // Prefer placing to the right of the popup; fall back to the left if no room
    if (popupBounds.x + popupBounds.width + hoverSize.x <= screen.x + screen.width) {
      x = popupBounds.x + popupBounds.width;
    } else {
      x = popupBounds.x - hoverSize.x;
    }
    int y = Math.max(screen.y, Math.min(itemLoc.y, screen.y + screen.height - hoverSize.y));
    hoverShell.setLocation(x, y);
    hoverShell.setVisible(true);
  }

  private void closeHoverShell() {
    if (hoverShell != null && !hoverShell.isDisposed()) {
      hoverShell.dispose();
    }
    hoverShell = null;
  }

  private void moveFocus(int delta) {
    if (items.isEmpty()) {
      return;
    }
    int size = items.size();
    int start = focusedIndex < 0 ? (delta > 0 ? -1 : size) : focusedIndex;
    int idx = start;
    for (int i = 0; i < size; i++) {
      idx += delta;
      if (idx < 0) {
        idx = size - 1;
      } else if (idx >= size) {
        idx = 0;
      }
      if (items.get(idx).item().isEnabled()) {
        break;
      }
    }
    if (idx == focusedIndex) {
      return;
    }
    updateFocusBorder(focusedIndex, false);
    focusedIndex = idx;
    updateFocusBorder(focusedIndex, true);
    scrollToFocusedItem();
  }

  private void scrollToFocusedItem() {
    if (scrolledComposite != null && !scrolledComposite.isDisposed() && focusedIndex >= 0
        && focusedIndex < items.size()) {
      scrolledComposite.showControl(items.get(focusedIndex).composite());
    }
  }

  private void updateFocusBorder(int index, boolean focused) {
    if (index < 0 || index >= items.size()) {
      return;
    }
    Composite itemComp = items.get(index).composite();
    if (!itemComp.isDisposed()) {
      if (focused) {
        String baseId = (String) itemComp.getData(POPUP_ITEM_BASE_ID_KEY);
        setPopupItemState(itemComp, baseId != null ? baseId : POPUP_ITEM_DEFAULT_ID, true);
      } else {
        DropdownItem item = items.get(index).item();
        String baseId = item.getId() != null && item.getId().equals(selectedItemId) ? POPUP_ITEM_SELECTED_ID
            : POPUP_ITEM_DEFAULT_ID;
        setPopupItemState(itemComp, baseId, false);
      }
    }
  }

  private void activateFocusedItem() {
    if (focusedIndex < 0 || focusedIndex >= items.size()) {
      return;
    }
    DropdownItem item = items.get(focusedIndex).item();
    if (!item.isEnabled()) {
      return;
    }
    if (item.getOnAction() != null) {
      close();
      item.getOnAction().run();
    } else {
      close();
      if (selectionListener != null && item.getId() != null) {
        selectionListener.accept(item.getId());
      }
    }
  }

  void close() {
    closeHoverShell();
    if (keyboardFilter != null) {
      Display display = shell != null && !shell.isDisposed() ? shell.getDisplay() : SwtUtils.getDisplay();
      display.removeFilter(SWT.KeyDown, keyboardFilter);
      keyboardFilter = null;
    }
    if (shell != null && !shell.isDisposed()) {
      shell.dispose();
    }
    shell = null;
  }

  boolean isOpen() {
    return shell != null && !shell.isDisposed() && shell.isVisible();
  }

  private void setPopupItemState(Composite control, String baseCssId, boolean focused) {
    String currentBaseCssId = (String) control.getData(POPUP_ITEM_BASE_ID_KEY);
    boolean currentFocused = Boolean.TRUE.equals(control.getData(POPUP_ITEM_FOCUSED_KEY));
    if (focused == currentFocused && baseCssId.equals(currentBaseCssId)) {
      return;
    }
    control.setData(POPUP_ITEM_BASE_ID_KEY, baseCssId);
    control.setData(POPUP_ITEM_FOCUSED_KEY, focused);
    control.setRedraw(false);
    applyCssId(control, baseCssId);
    for (Control child : control.getChildren()) {
      applyCssIdRecursively(child, focused ? POPUP_ITEM_FOCUSED_ID : baseCssId);
    }
    control.setRedraw(true);
  }

  private void paintFocusedItemBackground(Composite itemComp, GC gc) {
    if (!Boolean.TRUE.equals(itemComp.getData(POPUP_ITEM_FOCUSED_KEY))) {
      return;
    }
    Rectangle bounds = itemComp.getClientArea();
    if (bounds.width <= 0 || bounds.height <= 0) {
      return;
    }
    gc.setAntialias(SWT.ON);
    gc.setBackground(CssConstants.getPopupItemFocusBgColor(itemComp.getDisplay()));
    gc.fillRoundRectangle(0, 0, bounds.width, bounds.height, ITEM_FOCUS_ARC, ITEM_FOCUS_ARC);
  }

  private void applyCssId(Control control, String cssId) {
    if (!cssId.equals(control.getData(CssConstants.CSS_ID_KEY))) {
      control.setData(CssConstants.CSS_ID_KEY, cssId);
      styleControl(control);
    }
  }

  private void applyCssIdRecursively(Control control, String cssId) {
    applyCssId(control, cssId);
    if (control instanceof Composite composite) {
      for (Control child : composite.getChildren()) {
        applyCssIdRecursively(child, cssId);
      }
    }
  }

  private void styleControl(Control control) {
    if (stylingEngine != null) {
      stylingEngine.style(control);
    }
  }

  private void setCssClassOnly(Control control, String className) {
    if (stylingEngine != null) {
      stylingEngine.setClassname(control, className);
    } else {
      control.setData(CssConstants.CSS_CLASS_NAME_KEY, className);
    }
  }
}

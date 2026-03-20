package com.microsoft.copilot.eclipse.ui.swt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A named group of {@link DropdownItem}s displayed in a {@link DropdownButton} popup.
 *
 * <p>Groups are automatically separated from each other by horizontal dividers. An optional
 * header label can be shown above the group's items.
 *
 * <p>Example:
 * <pre>
 * DropdownItemGroup standard = DropdownItemGroup.of("Standard Models", standardItems);
 * DropdownItemGroup premium  = DropdownItemGroup.of("Premium Models", premiumItems);
 * </pre>
 */
public final class DropdownItemGroup {

  private final String header;
  private final List<DropdownItem> items;

  private DropdownItemGroup(String header, List<DropdownItem> items) {
    this.header = header;
    this.items = Collections.unmodifiableList(new ArrayList<>(items));
  }

  /**
   * Creates a group with an optional header label.
   *
   * @param header the header text, or {@code null} for no header
   * @param items the items in this group
   * @return a new {@link DropdownItemGroup}
   */
  public static DropdownItemGroup of(String header, List<DropdownItem> items) {
    return new DropdownItemGroup(header, items);
  }

  /**
   * Creates a group without a header.
   *
   * @param items the items in this group
   * @return a new {@link DropdownItemGroup}
   */
  public static DropdownItemGroup of(List<DropdownItem> items) {
    return new DropdownItemGroup(null, items);
  }

  /**
   * Returns the optional header label text, or {@code null} if none.
   *
   * @return the header
   */
  public String getHeader() {
    return header;
  }

  /**
   * Returns the unmodifiable list of items in this group.
   *
   * @return the items
   */
  public List<DropdownItem> getItems() {
    return items;
  }
}

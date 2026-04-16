// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.swt;

import org.eclipse.swt.graphics.Image;

/**
 * An immutable item in a {@link DropdownButton} popup.
 *
 * <p>Each item has an optional leading icon, a display label, and an optional right-aligned
 * suffix. Items can carry optional behavior:
 * <ul>
 *   <li>{@code onAction} – fires on click instead of a selection event (e.g. "Manage...",
 *       "Configure..."). When set, the global selection listener is <em>not</em> called.</li>
 *   <li>{@code hoverProvider} – configures the lightweight shell shown near the cursor when the
 *       user hovers over the item.</li>
 * </ul>
 *
 * <p>Use {@link #separator()} to create a visual divider between items within a group.
 *
 * <p>Build instances with the inner {@link Builder}:
 * <pre>
 * DropdownItem item = new DropdownItem.Builder()
 *     .id("manage")
 *     .label("Manage Models...")
 *     .onAction(() -&gt; openPreferences())
 *     .build();
 * </pre>
 */
public final class DropdownItem {

  private final String id;
  private final String label;
  private final Image icon;
  private final String suffix;
  private final String tooltip;
  private final boolean enabled;
  private final boolean separator;
  private final Runnable onAction;
  private final IDropdownItemHoverProvider hoverProvider;

  private static final DropdownItem SEPARATOR = new Builder().isSeparator(true).build();

  private DropdownItem(Builder builder) {
    this.id = builder.id;
    this.label = builder.label;
    this.icon = builder.icon;
    this.suffix = builder.suffix;
    this.tooltip = builder.tooltip;
    this.enabled = builder.enabled;
    this.separator = builder.separator;
    this.onAction = builder.onAction;
    this.hoverProvider = builder.hoverProvider;
  }

  /**
   * Returns a sentinel item that renders as a horizontal visual separator. Separator items are not
   * interactive and are skipped during keyboard navigation.
   *
   * @return the separator sentinel item
   */
  public static DropdownItem separator() {
    return SEPARATOR;
  }

  /**
   * Returns the unique identifier used for selection tracking, or {@code null}.
   *
   * @return the item id
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the display label text.
   *
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Returns the optional leading icon, or {@code null}.
   *
   * @return the icon
   */
  public Image getIcon() {
    return icon;
  }

  /**
   * Returns the optional right-aligned suffix text, or {@code null}.
   *
   * @return the suffix
   */
  public String getSuffix() {
    return suffix;
  }

  /**
   * Returns the native tooltip text, or {@code null}.
   *
   * @return the tooltip
   */
  public String getTooltip() {
    return tooltip;
  }

  /**
   * Returns whether this item is interactive (default: {@code true}).
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Returns whether this is a visual separator item.
   *
   * @return true if this is a separator
   */
  public boolean isSeparator() {
    return separator;
  }

  /**
   * Returns the action {@link Runnable} to invoke on click, or {@code null}.
   *
   * <p>When non-null, triggering this item fires the runnable and does <em>not</em> notify the
   * global selection listener. Use for action items that open a dialog or panel rather than
   * changing the selection.
   *
   * @return the action runnable
   */
  public Runnable getOnAction() {
    return onAction;
  }

  /**
   * Returns the hover provider, or {@code null}.
   *
   * <p>When non-null, hovering over the item opens a lightweight shell near the cursor whose
   * contents and container can be configured by the provider.
   *
   * @return the hover provider
   */
  public IDropdownItemHoverProvider getHoverProvider() {
    return hoverProvider;
  }

  /**
   * Builder for {@link DropdownItem}.
   */
  public static final class Builder {

    private String id;
    private String label = "";
    private Image icon;
    private String suffix;
    private String tooltip;
    private boolean enabled = true;
    private boolean separator;
    private Runnable onAction;
    private IDropdownItemHoverProvider hoverProvider;

    /**
     * Sets the unique identifier for selection tracking.
     *
     * @param id the item id
     * @return this builder
     */
    public Builder id(String id) {
      this.id = id;
      return this;
    }

    /**
     * Sets the display label text.
     *
     * @param label the label
     * @return this builder
     */
    public Builder label(String label) {
      this.label = label != null ? label : "";
      return this;
    }

    /**
     * Sets the optional leading icon displayed to the left of the label.
     *
     * @param icon the icon image
     * @return this builder
     */
    public Builder icon(Image icon) {
      this.icon = icon;
      return this;
    }

    /**
     * Sets the optional right-aligned suffix text shown in a muted color.
     *
     * @param suffix the suffix text
     * @return this builder
     */
    public Builder suffix(String suffix) {
      this.suffix = suffix;
      return this;
    }

    /**
     * Sets the native tooltip text shown on hover.
     *
     * @param tooltip the tooltip text
     * @return this builder
     */
    public Builder tooltip(String tooltip) {
      this.tooltip = tooltip;
      return this;
    }

    /**
     * Sets whether the item is interactive (default: {@code true}).
     *
     * @param enabled true if interactive
     * @return this builder
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the action runnable fired on click.
     *
     * <p>When set, the global selection listener is <em>not</em> called.
     *
     * @param onAction the action to run on click
     * @return this builder
     */
    public Builder onAction(Runnable onAction) {
      this.onAction = onAction;
      return this;
    }

    /**
     * Sets the hover provider invoked when the cursor enters this item.
     *
     * @param provider the hover provider
     * @return this builder
     */
    public Builder hoverProvider(IDropdownItemHoverProvider provider) {
      this.hoverProvider = provider;
      return this;
    }

    Builder isSeparator(boolean separator) {
      this.separator = separator;
      return this;
    }

    /**
     * Builds and returns the {@link DropdownItem}.
     *
     * @return the built item
     */
    public DropdownItem build() {
      return new DropdownItem(this);
    }
  }
}

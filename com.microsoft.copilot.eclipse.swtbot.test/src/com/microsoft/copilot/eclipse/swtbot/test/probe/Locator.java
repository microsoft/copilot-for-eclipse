/*******************************************************************************
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 *******************************************************************************/
package com.microsoft.copilot.eclipse.swtbot.test.probe;

import java.util.List;

/**
 * A widget locator expressed as a small discriminated union.
 *
 * <p>SWT/SWTBot does not use XPath; instead we select widgets by their SWTBot finder
 * (label, button, tree path, etc.). The {@link #by} field selects the finder and the
 * remaining fields carry its arguments.</p>
 *
 * <p>Supported {@code by} values:</p>
 * <ul>
 *   <li>{@code viewId} — uses {@link #id} as an Eclipse view id.</li>
 *   <li>{@code label} — uses {@link #text} to find a label widget.</li>
 *   <li>{@code button} — uses {@link #text} to find a button.</li>
 *   <li>{@code buttonWithTooltip} — uses {@link #tooltip} to find a button by its tooltip text.</li>
 *   <li>{@code widgetClass} — walks the shell tree and matches on
 *       {@code Widget.getClass().getSimpleName()} equal to {@link #value}.
 *       Fallback for widgets we don't own (can't tag with an SWTBot id).</li>
 *   <li>{@code widgetId} — preferred identification: widget whose
 *       {@code setData("org.eclipse.swtbot.widget.key", ...)} equals {@link #value}.
 *       This is the SWTBot-blessed convention (equivalent to {@code withId}).</li>
 *   <li>{@code text} — text field; optional {@link #index} (default 0).</li>
 *   <li>{@code tree} — active view tree; uses {@link #labels} as node path.</li>
 *   <li>{@code styledText} — first StyledText on the active view/editor.</li>
 *   <li>{@code cssId} — widget whose {@code CssConstants.CSS_ID_KEY} data equals {@link #value}.</li>
 *   <li>{@code cssClass} — widget whose {@code CssConstants.CSS_CLASS_NAME_KEY} data
 *       contains {@link #value} (values are space-separated class lists).</li>
 * </ul>
 */
public class Locator {
  public String by;
  public String id;
  public String text;
  public Integer index;
  public List<String> labels;
  /** Value argument for {@code cssId} / {@code cssClass} locators. */
  public String value;
  /** Tooltip argument for {@code buttonWithTooltip} locator. */
  public String tooltip;
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.contextwindow;

import java.text.MessageFormat;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ContextSizeInfo;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.ContextWindowBar;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Popup shell that displays context window token usage breakdown. Layout and color follow
 * {@code ModelHoverContentProvider}.
 */
class ContextWindowPopup {

  private static final String DROPDOWN_POPUP_CSS_ID = "dropdown-popup";
  private static final String POPUP_SECONDARY_TEXT_CLASS = "popup-secondary-text";

  private static final int POPUP_WIDTH = 200;
  private static final int SECTION_SPACING = 8;
  private static final int ROW_SPACING = 2;
  private static final int BORDER_ARC = 8;
  private static final int H_MARGIN = 8;
  private static final int V_MARGIN = 8;
  private static final int POLL_INTERVAL_MS = 100;

  private Shell shell;
  private Control anchor;
  private Runnable pollRunnable;
  private final ContextWindowService contextWindowService;
  private final IStylingEngine stylingEngine;

  // Updatable labels
  private Label tokenUsageLabel;
  private Label percentageLabel;
  private ContextWindowBar progressBar;
  private Label systemInstructionsValue;
  private Label toolDefinitionsValue;
  private Label messagesValue;
  private Label attachedFilesValue;
  private Label toolResultsValue;

  ContextWindowPopup(ContextWindowService service) {
    this.contextWindowService = service;
    this.stylingEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);
    this.contextWindowService.bindContextWindowPopup(this);
  }

  public void open(Control anchor) {
    this.anchor = anchor;
    if (isOpen()) {
      return;
    }
    stopPolling();
    ContextSizeInfo info = contextWindowService.getState();
    if (info == null) {
      return;
    }

    Shell parentShell = anchor.getShell();
    shell = new Shell(parentShell, SWT.NO_TRIM | SWT.ON_TOP);

    GridLayout shellLayout = new GridLayout(1, false);
    shellLayout.marginWidth = H_MARGIN;
    shellLayout.marginHeight = V_MARGIN;
    shellLayout.verticalSpacing = ROW_SPACING;
    shell.setLayout(shellLayout);

    buildContent(shell, info);
    applyCssId(shell, DROPDOWN_POPUP_CSS_ID);
    addBorder(shell);

    shell.pack();
    Point size = shell.getSize();
    shell.setSize(Math.max(size.x, POPUP_WIDTH), size.y);
    positionPopup(shell, anchor);
    shell.open();
    startPolling();
  }

  public void dispose() {
    close();
    contextWindowService.unbindContextWindowPopup(this);
  }

  /**
   * Closes the popup.
   */
  private void close() {
    stopPolling();
    anchor = null;
    if (shell != null && !shell.isDisposed()) {
      shell.dispose();
      shell = null;
    }
  }

  private boolean isOpen() {
    return shell != null && !shell.isDisposed();
  }

  private void startPolling() {
    Display display = getActiveDisplay();
    if (display == null) {
      return;
    }
    pollRunnable = () -> {
      if (shell == null || shell.isDisposed()) {
        return;
      }
      if (!isCursorInside(anchor) && !isCursorInside(shell)) {
        close();
      } else {
        display.timerExec(POLL_INTERVAL_MS, pollRunnable);
      }
    };
    display.timerExec(POLL_INTERVAL_MS, pollRunnable);
  }

  private void stopPolling() {
    Display display = getActiveDisplay();
    if (display != null && pollRunnable != null) {
      display.timerExec(-1, pollRunnable);
    }
    pollRunnable = null;
  }

  private Display getActiveDisplay() {
    if (shell != null && !shell.isDisposed()) {
      return shell.getDisplay();
    }
    if (anchor != null && !anchor.isDisposed()) {
      return anchor.getDisplay();
    }
    return null;
  }

  private boolean isCursorInside(Control control) {
    if (control == null || control.isDisposed()) {
      return false;
    }
    Display display = control.getDisplay();
    if (display == null || display.isDisposed()) {
      return false;
    }
    return getDisplayBounds(control).contains(display.getCursorLocation());
  }

  private static Rectangle getDisplayBounds(Control control) {
    Point location = control.toDisplay(0, 0);
    Point size = control.getSize();
    return new Rectangle(location.x, location.y, size.x, size.y);
  }

  public void onContextSizeInfoChanged(ContextSizeInfo info) {
    if (info == null) {
      close();
      return;
    }
    updateLabels(info);
  }

  private void buildContent(Composite parent, ContextSizeInfo info) {
    addSectionHeader(parent, Messages.context_window_title, 0);

    Composite tokenRow = createRowComposite(parent);
    tokenUsageLabel = createSecondaryTextLabel(tokenRow, formatTokenRow(info));
    tokenUsageLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    percentageLabel = createSecondaryTextLabel(tokenRow, formatPercentage(info.utilizationPercentage()));
    percentageLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));

    progressBar = new ContextWindowBar(parent, SWT.NONE);
    progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    progressBar.setPercentage((int) Math.round(info.utilizationPercentage()));

    addSeparator(parent, SECTION_SPACING);

    addSectionHeader(parent, Messages.context_window_system, SECTION_SPACING);
    systemInstructionsValue = addKeyValueRow(parent, Messages.context_window_system_instructions,
        percentageOf(info.systemPromptTokens(), info.totalTokenLimit()));
    toolDefinitionsValue = addKeyValueRow(parent, Messages.context_window_tool_definitions,
        percentageOf(info.toolDefinitionTokens(), info.totalTokenLimit()));

    addSeparator(parent, SECTION_SPACING);

    addSectionHeader(parent, Messages.context_window_user_context, SECTION_SPACING);
    messagesValue = addKeyValueRow(parent, Messages.context_window_messages,
        percentageOf(info.userMessagesTokens() + info.assistantMessagesTokens(), info.totalTokenLimit()));
    attachedFilesValue = addKeyValueRow(parent, Messages.context_window_files,
        percentageOf(info.attachedFilesTokens(), info.totalTokenLimit()));
    toolResultsValue = addKeyValueRow(parent, Messages.context_window_tool_results,
        percentageOf(info.toolResultsTokens(), info.totalTokenLimit()));
  }

  private void updateLabels(ContextSizeInfo info) {
    if (shell == null || shell.isDisposed()) {
      return;
    }
    setLabelText(tokenUsageLabel, formatTokenRow(info));
    setLabelText(percentageLabel, formatPercentage(info.utilizationPercentage()));
    setLabelText(systemInstructionsValue, percentageOf(info.systemPromptTokens(), info.totalTokenLimit()));
    setLabelText(toolDefinitionsValue, percentageOf(info.toolDefinitionTokens(), info.totalTokenLimit()));
    setLabelText(messagesValue,
        percentageOf(info.userMessagesTokens() + info.assistantMessagesTokens(), info.totalTokenLimit()));
    setLabelText(attachedFilesValue, percentageOf(info.attachedFilesTokens(), info.totalTokenLimit()));
    setLabelText(toolResultsValue, percentageOf(info.toolResultsTokens(), info.totalTokenLimit()));
    if (progressBar != null && !progressBar.isDisposed()) {
      progressBar.setPercentage((int) Math.round(info.utilizationPercentage()));
    }
    shell.requestLayout();
  }

  private void addSectionHeader(Composite parent, String text, int verticalIndent) {
    Label header = new Label(parent, SWT.NONE);
    header.setText(text);
    applyCssId(header, DROPDOWN_POPUP_CSS_ID);
    Font boldFont = UiUtils.getBoldFont(header.getDisplay(), header.getFont());
    header.addDisposeListener(e -> boldFont.dispose());
    header.setFont(boldFont);
    GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
    gd.verticalIndent = verticalIndent;
    header.setLayoutData(gd);
  }

  private void addSeparator(Composite parent, int topSpacing) {
    Composite separator = new Composite(parent, SWT.NONE);
    applyCssId(separator, DROPDOWN_POPUP_CSS_ID);
    GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
    gd.heightHint = 1;
    gd.verticalIndent = topSpacing;
    separator.setLayoutData(gd);
    Color sepColor = CssConstants.getSeparatorColor(parent.getDisplay());
    separator.addPaintListener(e -> {
      Rectangle r = separator.getClientArea();
      e.gc.setBackground(sepColor);
      e.gc.fillRectangle(0, 0, r.width, 1);
    });
  }

  private Composite createRowComposite(Composite parent) {
    Composite row = new Composite(parent, SWT.NONE);
    applyCssId(row, DROPDOWN_POPUP_CSS_ID);
    GridLayout gl = new GridLayout(2, false);
    gl.marginWidth = 0;
    gl.marginHeight = 0;
    gl.horizontalSpacing = 0;
    row.setLayout(gl);
    row.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    return row;
  }

  private Label addKeyValueRow(Composite parent, String key, String value) {
    Composite row = createRowComposite(parent);

    Label keyLabel = createSecondaryTextLabel(row, key);
    keyLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    Label valueLabel = createSecondaryTextLabel(row, value);
    valueLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.NONE, false, false));
    return valueLabel;
  }

  private Label createSecondaryTextLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.NONE);
    label.setText(text);
    applyCssId(label, DROPDOWN_POPUP_CSS_ID);
    UiUtils.applyCssClass(label, POPUP_SECONDARY_TEXT_CLASS, stylingEngine);
    return label;
  }

  private void addBorder(Shell target) {
    Color borderColor = CssConstants.getBorderColor(target.getDisplay());
    target.addPaintListener(e -> {
      Rectangle bounds = target.getClientArea();
      e.gc.setAntialias(SWT.ON);
      e.gc.setForeground(borderColor);
      e.gc.setLineWidth(1);
      e.gc.drawRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1, BORDER_ARC, BORDER_ARC);
    });
  }

  private void positionPopup(Shell popup, Control anchor) {
    Point anchorLoc = anchor.toDisplay(0, 0);
    Point anchorSize = anchor.getSize();
    Point popupSize = popup.getSize();

    // Position above the anchor, centered horizontally
    int x = anchorLoc.x + (anchorSize.x - popupSize.x) / 2;
    int y = anchorLoc.y - popupSize.y;

    // Clamp to screen containing the anchor
    Rectangle screenBounds = getMonitorBounds(anchor.getDisplay(), anchorLoc);
    x = Math.max(screenBounds.x, Math.min(x, screenBounds.x + screenBounds.width - popupSize.x));
    if (y < screenBounds.y) {
      y = anchorLoc.y + anchorSize.y;
    }
    popup.setLocation(x, y);
  }

  private static Rectangle getMonitorBounds(Display display, Point location) {
    for (Monitor monitor : display.getMonitors()) {
      if (monitor.getBounds().contains(location)) {
        return monitor.getBounds();
      }
    }
    return display.getPrimaryMonitor().getBounds();
  }

  private static void setLabelText(Label label, String text) {
    if (label != null && !label.isDisposed()) {
      label.setText(text);
    }
  }

  private static String formatTokens(int count) {
    if (count >= 1000) {
      double k = count / 1000.0;
      return String.format("%.1fK", k);
    }
    return String.valueOf(count);
  }

  private static String formatPercentage(double pct) {
    if (pct == 0) {
      return "0%";
    }
    return String.format("%.1f%%", pct);
  }

  private static String formatTokenRow(ContextSizeInfo info) {
    return MessageFormat.format(Messages.context_window_tokens, formatTokens(info.totalUsedTokens()),
        formatTokens(info.totalTokenLimit()));
  }

  private static String percentageOf(int tokens, int totalLimit) {
    if (totalLimit <= 0) {
      return "0%";
    }
    double pct = (double) tokens / totalLimit * 100;
    return formatPercentage(pct);
  }

  private void applyCssId(Control control, String cssId) {
    control.setData(CssConstants.CSS_ID_KEY, cssId);
    if (stylingEngine != null) {
      stylingEngine.style(control);
    }
  }

}

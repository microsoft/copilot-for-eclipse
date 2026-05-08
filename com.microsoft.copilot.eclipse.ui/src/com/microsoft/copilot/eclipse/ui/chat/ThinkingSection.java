// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * One titled section inside a {@link ThinkingBlock}: gutter (dot beside the title, vertical line beside the body) +
 * content column (bold title, then markdown body). The parent diffs parsed sections against live ones and calls
 * {@link #setBody(String)} during streaming.
 */
final class ThinkingSection extends Composite {
  private static final String SECONDARY_TEXT_CSS_CLASS = "text-secondary";
  private static final int GUTTER_WIDTH = 12;
  private static final int DOT_DIAMETER = 4;

  private final String title;
  private final IStylingEngine stylingEngine;
  private ChatMarkupViewer bodyViewer;
  private String body = "";

  public ThinkingSection(Composite parent, String title, IStylingEngine stylingEngine) {
    super(parent, SWT.NONE);
    this.title = title;
    this.stylingEngine = stylingEngine;

    GridLayout layout = new GridLayout(2, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 6;
    layout.verticalSpacing = 2;
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    if (StringUtils.isNotBlank(title)) {
      createTitleRow(title);
    }
  }

  /** Title this section was constructed with, or {@code null} for a leading untitled section. */
  public String getTitle() {
    return title;
  }

  /** Set or update the body markdown. Lazily creates the body viewer (and gutter line) on first non-empty call. */
  public void setBody(String newBody) {
    if (Objects.equals(body, newBody)) {
      return;
    }
    body = newBody;
    if (newBody == null || newBody.isEmpty()) {
      // Append-only streaming never empties a body once it has content; nothing to do.
      return;
    }
    if (bodyViewer == null || bodyViewer.getTextWidget().isDisposed()) {
      bodyViewer = createBodyViewer(newBody);
    } else {
      bodyViewer.setMarkup(newBody);
      bodyViewer.getTextWidget().requestLayout();
    }
  }

  private void createTitleRow(String titleText) {
    Canvas dot = new Canvas(this, SWT.NONE);
    GridData dotData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
    dotData.widthHint = GUTTER_WIDTH;
    dotData.heightHint = DOT_DIAMETER + 4;
    dot.setLayoutData(dotData);
    dot.addPaintListener(e -> {
      e.gc.setAntialias(SWT.ON);
      e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
      Rectangle area = ((Canvas) e.widget).getClientArea();
      int cx = (area.width - DOT_DIAMETER) / 2;
      int cy = (area.height - DOT_DIAMETER) / 2;
      e.gc.fillOval(cx, cy, DOT_DIAMETER, DOT_DIAMETER);
    });

    ChatMarkupViewer titleView = new ChatMarkupViewer(this, SWT.LEFT | SWT.WRAP);
    StyledText titleWidget = titleView.getTextWidget();
    titleWidget.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    titleWidget.setEditable(false);
    titleWidget.setMargins(0, 0, 0, 0);
    UiUtils.applyCssClass(titleWidget, SECONDARY_TEXT_CSS_CLASS, stylingEngine);
    titleView.setMarkup("**" + titleText + "**");
  }

  private ChatMarkupViewer createBodyViewer(String markup) {
    Canvas line = new Canvas(this, SWT.NONE);
    GridData lineData = new GridData(SWT.CENTER, SWT.FILL, false, false);
    lineData.widthHint = GUTTER_WIDTH;
    lineData.heightHint = 5;
    line.setLayoutData(lineData);
    line.addPaintListener(e -> {
      e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
      Rectangle area = ((Canvas) e.widget).getClientArea();
      int cx = (area.width - 1) / 2;
      e.gc.fillRectangle(cx, 0, 1, area.height);
    });

    ChatMarkupViewer viewer = new ChatMarkupViewer(this, SWT.MULTI | SWT.WRAP);
    StyledText text = viewer.getTextWidget();
    text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    text.setEditable(false);
    text.setMargins(0, 0, 0, 0);
    UiUtils.applyCssClass(text, SECONDARY_TEXT_CSS_CLASS, stylingEngine);
    viewer.setMarkup(markup);
    return viewer;
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.swt.SpinnerAnimator;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A collapsible "Thinking" banner shown above an assistant turn's reply while the model emits thinking deltas.
 *
 * <p>Lifecycle:
 * <ol>
 * <li>Streaming: shows a rotating spinner, the "Thinking..." text, and the body area expanded.
 * <li>Complete: spinner is replaced with the completed-status icon, the text is replaced with a server-generated title
 * (or a fallback), and the body collapses by default. The user can toggle the body open/closed with the chevron icon or
 * by clicking the title.
 * </ol>
 */
public class ThinkingBlock extends Composite {
  private static final String SECONDARY_TEXT_CSS_CLASS = "text-secondary";

  // Header (icon + title + chevron)
  private Composite header;
  private GridLayout headerLayout;
  private Label iconLabel;
  private ChatMarkupViewer titleViewer;
  private Label chevronLabel;

  private Composite body;
  private final List<SectionView> sectionViews = new ArrayList<>();

  private final StringBuilder textBuffer = new StringBuilder();
  private boolean expanded = true;
  private boolean complete;

  private SpinnerAnimator spinner;
  private Image completedIcon;
  private Image downArrowImage;
  private Image rightArrowImage;

  private final IStylingEngine stylingEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);

  /**
   * Create the widget.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public ThinkingBlock(Composite parent, int style) {
    super(parent, style);
    GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 2;
    layout.marginWidth = 0;
    layout.verticalSpacing = 4;
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    createHeader();
    createBody();

    addDisposeListener(e -> handleDispose());

    setTitleText(Messages.thinking_inProgressTitle);
    spinner = new SpinnerAnimator(iconLabel);
    spinner.start();
    updateChevron();
  }

  private void createHeader() {
    header = new Composite(this, SWT.NONE);
    headerLayout = new GridLayout(4, false);
    headerLayout.marginHeight = 0;
    headerLayout.marginWidth = 0;
    // Match AgentStatusLabel's icon-to-text spacing for visual consistency.
    headerLayout.horizontalSpacing = 2;
    header.setLayout(headerLayout);
    header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    iconLabel = new Label(header, SWT.NONE);
    iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    // Title: no grab so the chevron can sit immediately after it; widthHint is recomputed on
    // resize so SWT.WRAP kicks in when the title would otherwise overflow.
    titleViewer = new ChatMarkupViewer(header, SWT.LEFT | SWT.WRAP);
    StyledText titleText = titleViewer.getTextWidget();
    GridData titleData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    titleText.setLayoutData(titleData);
    titleText.setEditable(false);
    // Strip StyledText's intrinsic margins so the text sits flush with the adjacent labels.
    titleText.setMargins(0, 0, 0, 0);
    UiUtils.applyCssClass(titleText, SECONDARY_TEXT_CSS_CLASS, stylingEngine);

    chevronLabel = new Label(header, SWT.NONE);
    chevronLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    // Filler absorbs any remaining horizontal space so the chevron sits flush to the title.
    Label filler = new Label(header, SWT.NONE);
    filler.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Cursor handCursor = getDisplay().getSystemCursor(SWT.CURSOR_HAND);
    header.setCursor(handCursor);
    titleText.setCursor(handCursor);
    chevronLabel.setCursor(handCursor);

    // Constrain the title's width so SWT.WRAP can take effect once the parent is narrower than
    // the natural single-line width of the markup.
    header.addListener(SWT.Resize, e -> updateTitleWidthHint());

    MouseAdapter toggleListener = new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        toggleExpanded();
      }
    };
    titleText.addMouseListener(toggleListener);
    chevronLabel.addMouseListener(toggleListener);
  }

  private void updateTitleWidthHint() {
    if (titleViewer == null || header == null || header.isDisposed()) {
      return;
    }
    StyledText titleText = titleViewer.getTextWidget();
    if (titleText.isDisposed()) {
      return;
    }
    int headerWidth = header.getClientArea().width;
    if (headerWidth <= 0) {
      return;
    }
    int iconWidth = iconLabel != null && !iconLabel.isDisposed() ? iconLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x
        : 0;
    int chevronWidth = chevronLabel != null && !chevronLabel.isDisposed()
        ? chevronLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x
        : 0;
    int spacing = headerLayout.horizontalSpacing * (headerLayout.numColumns - 1);
    int available = headerWidth - iconWidth - chevronWidth - spacing - headerLayout.marginWidth * 2;
    if (available <= 0) {
      return;
    }
    GridData titleData = (GridData) titleText.getLayoutData();
    int natural = titleText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    int newHint = Math.min(natural, available);
    if (newHint != titleData.widthHint) {
      titleData.widthHint = newHint;
      header.requestLayout();
    }
  }

  private void createBody() {
    body = new Composite(this, SWT.NONE);
    GridLayout bodyLayout = new GridLayout(1, false);
    bodyLayout.marginHeight = 4;
    bodyLayout.marginLeft = 8;
    bodyLayout.marginWidth = 0;
    // Spacing between consecutive sections; matches copilot-xcode's VStack(spacing: 8).
    bodyLayout.verticalSpacing = 8;
    body.setLayout(bodyLayout);
    body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
  }

  /**
   * Append a thinking delta. Empty/blank fragments are ignored. Safe to call repeatedly while the block is in the
   * streaming state.
   *
   * @param fragment delta text to append (may be {@code null})
   */
  public void appendText(String fragment) {
    if (fragment == null || fragment.isEmpty()) {
      return;
    }
    textBuffer.append(fragment);
    rebuildSections();
    requestLayout();
  }

  /**
   * Re-parse the accumulated thinking text into sections and reconcile the section views.
   *
   * <p>To avoid recreating widgets on every streaming delta, we only do a full rebuild when the
   * structural shape changes (section count, or any title text). Otherwise we just push the latest
   * body markup into existing section viewers (typically only the trailing one has changed).
   */
  private void rebuildSections() {
    if (body == null || body.isDisposed()) {
      return;
    }
    List<ThinkingSection> parsed = parseSections(textBuffer.toString());

    boolean shapeChanged = parsed.size() != sectionViews.size();
    if (!shapeChanged) {
      for (int i = 0; i < parsed.size(); i++) {
        if (!Objects.equals(parsed.get(i).title(), sectionViews.get(i).getTitle())) {
          shapeChanged = true;
          break;
        }
      }
    }

    if (shapeChanged) {
      for (SectionView sv : sectionViews) {
        sv.dispose();
      }
      sectionViews.clear();
      for (ThinkingSection s : parsed) {
        SectionView sv = new SectionView(body, s.title());
        sv.setBody(s.body());
        sectionViews.add(sv);
      }
      body.layout(true, true);
    } else {
      for (int i = 0; i < parsed.size(); i++) {
        sectionViews.get(i).setBody(parsed.get(i).body());
      }
    }
  }

  /**
   * Parses thinking text into title-paired sections.
   *
   * <p>Each "title-only" line (a line that, when trimmed, is exactly {@code **Title**}) starts a
   * new section. All lines that follow up to the next title (or end of text) become that
   * section's body. Lines before any title go into a leading section with a {@code null} title.
   *
   * <p>While streaming, if the last line of the buffer starts with {@code **} but has not yet
   * received its closing {@code **}, that line is treated as a still-streaming title and held
   * back from the body so the partial markup is not briefly rendered as inline bold text.
   *
   * <p>Mirrors {@code MessageThinking.parseSections} in copilot-xcode.
   */
  static List<ThinkingSection> parseSections(String raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    List<ThinkingSection> sections = new ArrayList<>();
    String currentTitle = null;
    StringBuilder currentBody = new StringBuilder();

    // -1 limit preserves trailing empty strings, matching Swift's components(separatedBy:).
    String[] lines = raw.split("\n", -1);
    int endIndex = lines.length;
    if (endIndex > 0 && isStreamingTitlePrefix(lines[endIndex - 1])) {
      // Skip the trailing partial title; it will become a proper title once its closing `**`
      // arrives in a later streaming delta.
      endIndex--;
    }

    for (int i = 0; i < endIndex; i++) {
      String line = lines[i];
      String trimmed = line.trim();
      if (trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length() > 4) {
        String inner = trimmed.substring(2, trimmed.length() - 2);
        if (!inner.isEmpty() && !inner.contains("*")) {
          flushSection(sections, currentTitle, currentBody);
          currentTitle = inner;
          currentBody.setLength(0);
          continue;
        }
      }
      // Preserve original line breaks so markdown structure (lists, paragraphs) survives.
      currentBody.append(line).append('\n');
    }
    flushSection(sections, currentTitle, currentBody);
    return sections;
  }

  /**
   * Returns {@code true} when the given line looks like the opening half of a still-streaming
   * title: it starts with {@code **} but contains no other {@code **} pair to close it yet.
   */
  private static boolean isStreamingTitlePrefix(String line) {
    String trimmed = line.trim();
    if (!trimmed.startsWith("**")) {
      return false;
    }
    int count = 0;
    int idx = 0;
    while ((idx = trimmed.indexOf("**", idx)) != -1) {
      count++;
      idx += 2;
    }
    // Exactly one occurrence means we have only the opening `**` so far.
    return count == 1;
  }

  private static void flushSection(List<ThinkingSection> sections, String title, StringBuilder body) {
    String trimmedBody = body.toString().strip();
    if (title != null || !trimmedBody.isEmpty()) {
      sections.add(new ThinkingSection(title, trimmedBody));
    }
  }

  /** A parsed thinking section: an optional title and its body markdown. */
  static record ThinkingSection(String title, String body) {
  }

  /**
   * Returns whether this block has been sealed.
   *
   * @return {@code true} once {@link #markComplete(String)} has been called
   */
  public boolean isComplete() {
    return complete;
  }

  /**
   * Mark the thinking block as complete: stops the spinner, swaps to the completed icon, replaces the title with the
   * supplied text (or a fallback when blank), and collapses the body by default.
   *
   * @param title the title to display, or {@code null}/blank for the default fallback
   */
  public void markComplete(String title) {
    if (complete) {
      // Title may arrive after sealing; just refresh the text.
      setTitleText(StringUtils.isNotBlank(title) ? title : Messages.thinking_defaultTitle);
      return;
    }
    complete = true;

    if (spinner != null) {
      spinner.stop();
    }

    if (completedIcon == null || completedIcon.isDisposed()) {
      completedIcon = UiUtils.buildImageFromPngPath("/icons/complete_status.png");
    }
    if (!iconLabel.isDisposed()) {
      iconLabel.setImage(completedIcon);
      iconLabel.requestLayout();
    }

    setTitleText(StringUtils.isNotBlank(title) ? title : Messages.thinking_defaultTitle);

    setExpanded(false);
  }

  /**
   * Update the title shown in the banner without changing completion state.
   *
   * @param title the title to display
   */
  public void setTitle(String title) {
    if (StringUtils.isBlank(title)) {
      return;
    }
    setTitleText(title);
  }

  private void setTitleText(String text) {
    if (titleViewer == null || titleViewer.getTextWidget().isDisposed()) {
      return;
    }
    titleViewer.setMarkup(text == null ? "" : text);
    updateTitleWidthHint();
    titleViewer.getTextWidget().requestLayout();
  }

  private void toggleExpanded() {
    setExpanded(!expanded);
  }

  private void setExpanded(boolean newExpanded) {
    this.expanded = newExpanded;
    if (body != null && !body.isDisposed()) {
      GridData data = (GridData) body.getLayoutData();
      data.exclude = !expanded;
      body.setVisible(expanded);
    }
    updateChevron();
    requestLayout();
  }

  private void updateChevron() {
    if (chevronLabel == null || chevronLabel.isDisposed()) {
      return;
    }
    if (expanded) {
      if (downArrowImage == null || downArrowImage.isDisposed()) {
        downArrowImage = UiUtils.buildImageFromPngPath("/icons/chat/down_arrow.png");
      }
      header.setToolTipText(Messages.thinking_collapseTooltip);
      chevronLabel.setImage(downArrowImage);
      chevronLabel.setToolTipText(Messages.thinking_collapseTooltip);
      if (titleViewer != null && !titleViewer.getTextWidget().isDisposed()) {
        titleViewer.getTextWidget().setToolTipText(Messages.thinking_collapseTooltip);
      }
    } else {
      if (rightArrowImage == null || rightArrowImage.isDisposed()) {
        rightArrowImage = UiUtils.buildImageFromPngPath("/icons/chat/right_arrow.png");
      }
      header.setToolTipText(Messages.thinking_expandTooltip);
      chevronLabel.setImage(rightArrowImage);
      chevronLabel.setToolTipText(Messages.thinking_expandTooltip);
      if (titleViewer != null && !titleViewer.getTextWidget().isDisposed()) {
        titleViewer.getTextWidget().setToolTipText(Messages.thinking_expandTooltip);
      }
    }
    chevronLabel.requestLayout();
  }

  private void handleDispose() {
    // The SpinnerAnimator hooks the iconLabel's dispose listener, so its frame image is freed
    // automatically; only the standalone icons we allocated here need explicit disposal.
    if (completedIcon != null && !completedIcon.isDisposed()) {
      completedIcon.dispose();
      completedIcon = null;
    }
    if (downArrowImage != null && !downArrowImage.isDisposed()) {
      downArrowImage.dispose();
      downArrowImage = null;
    }
    if (rightArrowImage != null && !rightArrowImage.isDisposed()) {
      rightArrowImage.dispose();
      rightArrowImage = null;
    }
  }

  /**
   * One row in the thinking body: a full-height left vertical bar, with a leading dash + bold
   * title on its right (top), and the section's markdown body directly underneath the title.
   */
  private final class SectionView {
    // Width of the bar column on the left of the section.
    private static final int BAR_WIDTH = 1;

    private final Composite container;
    private final ChatMarkupViewer titleViewer;
    private final ChatMarkupViewer bodyViewer;
    private final String title;
    private String currentBodyMarkup;

    SectionView(Composite parent, String title) {
      this.title = title;

      // Outer: [bar | content], bar spans the full height of the section.
      container = new Composite(parent, SWT.NONE);
      GridLayout containerLayout = new GridLayout(2, false);
      containerLayout.marginHeight = 0;
      containerLayout.marginWidth = 0;
      containerLayout.horizontalSpacing = 8;
      container.setLayout(containerLayout);
      container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      Label leftBar = new Label(container, SWT.SEPARATOR | SWT.VERTICAL);
      GridData barData = new GridData(SWT.LEFT, SWT.FILL, false, true);
      barData.widthHint = BAR_WIDTH;
      leftBar.setLayoutData(barData);

      // Content: [title] over [body]. verticalSpacing=0 so the title sits flush against the body
      // (no extra gap between the dash/title row and the markdown body).
      GridLayout contentLayout = new GridLayout(1, false);
      contentLayout.marginHeight = 0;
      contentLayout.marginWidth = 0;
      contentLayout.verticalSpacing = 0;
      Composite content = new Composite(container, SWT.NONE);
      content.setLayout(contentLayout);
      content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      // Title row: render the dash and the title together as a single bold markdown string so
      // they share font metrics and alignment. Only created when there is a title; otherwise
      // the body is the sole child of the content composite.
      if (title != null && !title.isEmpty()) {
        titleViewer = new ChatMarkupViewer(content, SWT.LEFT | SWT.WRAP);
        StyledText titleText = titleViewer.getTextWidget();
        titleText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        titleText.setEditable(false);
        // Strip StyledText's intrinsic margins so the title aligns flush with the bar.
        titleText.setMargins(0, 0, 0, 0);
        UiUtils.applyCssClass(titleText, SECONDARY_TEXT_CSS_CLASS, stylingEngine);
        titleViewer.setMarkup("**- " + title + "**");
      } else {
        titleViewer = null;
      }

      bodyViewer = new ChatMarkupViewer(content, SWT.MULTI | SWT.WRAP);
      StyledText bodyText = bodyViewer.getTextWidget();
      bodyText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      bodyText.setEditable(false);
      bodyText.setMargins(0, 0, 0, 0);
      UiUtils.applyCssClass(bodyText, SECONDARY_TEXT_CSS_CLASS, stylingEngine);
    }

    String getTitle() {
      return title;
    }

    void setBody(String body) {
      String safeBody = body == null ? "" : body;
      if (safeBody.equals(currentBodyMarkup)) {
        return;
      }
      currentBodyMarkup = safeBody;
      if (!bodyViewer.getTextWidget().isDisposed()) {
        bodyViewer.setMarkup(safeBody);
      }
      // Hide the body row when there's no body content so it doesn't reserve vertical space
      // (otherwise an empty StyledText still adds a small gap below the title).
      GridData bodyData = (GridData) bodyViewer.getTextWidget().getLayoutData();
      boolean hasBody = !safeBody.isEmpty();
      bodyData.exclude = !hasBody;
      bodyViewer.getTextWidget().setVisible(hasBody);
    }

    void dispose() {
      if (!container.isDisposed()) {
        container.dispose();
      }
    }
  }
}

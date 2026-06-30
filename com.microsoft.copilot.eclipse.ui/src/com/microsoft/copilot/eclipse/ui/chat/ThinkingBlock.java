// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.swt.SpinnerAnimator;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Collapsible "Thinking" banner shown above an assistant turn while the model emits thinking stream.
 *
 * <p>Pure view: callers drive the visual state via {@link #showCompleted()} and {@link #showCancelled()}.
 * The owning turn widget is responsible for cancellation events and title fetching.
 */
public class ThinkingBlock extends Composite {
  private static final String SECONDARY_TEXT_CSS_CLASS = "text-secondary";
  private static final Pattern TITLE_PATTERN =
      Pattern.compile("(?:^|\\n)\\*\\*([^*\\r\\n]+?)\\*\\*(?=\\r?\\n|$)");

  private static final int STREAMING_MAX_HEIGHT = 180;

  private Composite header;
  private Label iconLabel;
  private Label titleLabel;
  private Label chevronLabel;

  /** Scrollable wrapper around {@link #body}; used only during streaming. Disposed on finalized expand. */
  private ScrolledComposite bodyScroller;
  /** Body container holding one {@link ThinkingSection} per parsed section. */
  private Composite body;
  private final List<ThinkingSection> sections = new ArrayList<>();
  private final StringBuilder textBuffer = new StringBuilder();
  private boolean expanded = true;
  /** Auto-scroll to bottom during streaming. Turned off on any user scroll interaction. */
  private boolean autoScroll = true;

  /**
   * Lifecycle of the block. Transitions are always forward: STREAMING → (SEALED →)? → COMPLETED|CANCELLED.
   * SEALED means {@code sealThinking()} has fired and a title fetch is in flight; new thinking stream fragments must
   * start a new block.
   */
  private enum State { STREAMING, SEALED, COMPLETED, CANCELLED }

  private final String thinkingId = UUID.randomUUID().toString();
  private State state = State.STREAMING;
  private SpinnerAnimator spinner;
  private Image cancelledIcon;
  private Image downArrowImage;
  private Image rightArrowImage;

  private final IStylingEngine stylingEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);

  /** Construct an empty thinking block; the spinner starts immediately. */
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

    setTitle(Messages.thinking_title);
    spinner = new SpinnerAnimator(iconLabel);
    spinner.start();
    updateChevron();
  }

  /** Append a thinking stream fragment. Null/empty fragments are ignored. */
  public void appendText(String fragment) {
    if (fragment == null || fragment.isEmpty()) {
      return;
    }
    textBuffer.append(fragment);
    refreshBody();
    requestLayout();
  }

  /** Finalize as completed: hide spinner icon and transition to COMPLETED state. */
  public void showCompleted() {
    if (isFinalized()) {
      return;
    }
    if (!iconLabel.isDisposed()) {
      iconLabel.setImage(null);
      ((GridData) iconLabel.getLayoutData()).exclude = true;
      iconLabel.setVisible(false);
      iconLabel.requestLayout();
    }
    state = State.COMPLETED;
  }

  /**
   * Cancel the thinking block. If still streaming, shows the cancel icon and collapses. If already sealed (thinking
   * content finished, title fetch in flight), simply finalizes as completed since thinking itself was not interrupted.
   * No-op if already finalized.
   */
  public boolean showCancelled() {
    if (isFinalized()) {
      return false;
    }
    if (state == State.SEALED) {
      // Thinking content already finished; just finalize without the cancel icon.
      showCompleted();
      return false;
    }
    stopSpinner();
    if (cancelledIcon == null || cancelledIcon.isDisposed()) {
      cancelledIcon = UiUtils.buildImageFromPngPath("/icons/cancel_status.png");
    }
    if (!iconLabel.isDisposed()) {
      iconLabel.setImage(cancelledIcon);
    }
    setExpanded(false);
    unwrapBodyFromScroller();
    state = State.CANCELLED;
    return true;
  }

  /**
   * Mark the block as sealed: the owning widget has requested a title and any further thinking stream fragments must
   * land in a new block. Stops the spinner and collapses the block while the owning widget handles any subsequent
   * title updates. No-op once the block has been finalized or already sealed.
   */
  public void markSealed() {
    if (state != State.STREAMING) {
      return;
    }
    state = State.SEALED;
    stopSpinner();
    setExpanded(false);
    unwrapBodyFromScroller();
  }

  /** True only while new thinking stream fragments should still be appended to this block. */
  public boolean isAcceptingThinkStream() {
    return state == State.STREAMING;
  }

  /** True once the block has been completed or cancelled (spinner stopped, final title shown). */
  public boolean isFinalized() {
    return state == State.COMPLETED || state == State.CANCELLED;
  }

  /** The unique ID for this thinking block, shared with the persistence layer. */
  public String getThinkingId() {
    return thinkingId;
  }

  /** The full accumulated thinking text streamed so far. */
  public String getAccumulatedText() {
    return textBuffer.toString();
  }

  /** Non-blank {@code **Title**} strings extracted from the accumulated thinking text. */
  public String[] getExtractedTitles() {
    // Reuse the already-parsed section list rather than re-scanning the buffer.
    return sections.stream()
        .map(ThinkingSection::getTitle)
        .filter(StringUtils::isNotBlank)
        .toArray(String[]::new);
  }

  private void stopSpinner() {
    if (spinner != null) {
      spinner.stop();
      spinner = null;
    }
  }

  private void createHeader() {
    header = new Composite(this, SWT.NONE);
    GridLayout headerLayout = new GridLayout(4, false);
    headerLayout.marginHeight = 0;
    headerLayout.marginWidth = 0;
    // Match AgentStatusLabel's icon-to-text spacing for visual consistency.
    headerLayout.horizontalSpacing = 2;
    header.setLayout(headerLayout);
    header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    iconLabel = new Label(header, SWT.NONE);
    iconLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    titleLabel = new Label(header, SWT.LEFT | SWT.WRAP);
    titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    UiUtils.applyCssClass(titleLabel, SECONDARY_TEXT_CSS_CLASS, stylingEngine);

    ChatServiceManager chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
    if (chatServiceManager != null) {
      chatServiceManager.getChatFontService().registerControl(titleLabel);
    }

    chevronLabel = new Label(header, SWT.NONE);
    chevronLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    // Filler absorbs any remaining horizontal space so the chevron sits flush to the title.
    Label filler = new Label(header, SWT.NONE);
    filler.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Cursor handCursor = getDisplay().getSystemCursor(SWT.CURSOR_HAND);
    header.setCursor(handCursor);
    titleLabel.setCursor(handCursor);
    chevronLabel.setCursor(handCursor);

    // Constrain the title's width so SWT.WRAP can take effect when the header is narrower than
    // the natural single-line width of the title.
    header.addListener(SWT.Resize, e -> updateTitleWidthHint());

    MouseAdapter toggleListener = new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        toggleExpanded();
      }
    };
    // Attach to every header child (and the header itself) so the entire area that shows the hand
    // cursor is actually clickable. iconLabel is intentionally excluded: it hosts the live spinner
    // animation (and the cancel icon afterwards), and a clickable spinner is an odd affordance.
    header.addMouseListener(toggleListener);
    titleLabel.addMouseListener(toggleListener);
    chevronLabel.addMouseListener(toggleListener);
    filler.addMouseListener(toggleListener);
  }

  private void createBody() {
    bodyScroller = new ScrolledComposite(this, SWT.V_SCROLL);
    bodyScroller.setExpandHorizontal(true);
    bodyScroller.setExpandVertical(true);
    bodyScroller.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    bodyScroller.setAlwaysShowScrollBars(false);

    body = new Composite(bodyScroller, SWT.NONE);
    GridLayout bodyLayout = new GridLayout(1, false);
    bodyLayout.marginHeight = 4;
    bodyLayout.marginLeft = 4;
    bodyLayout.marginWidth = 0;
    bodyLayout.verticalSpacing = 6;
    body.setLayout(bodyLayout);

    bodyScroller.setContent(body);

    // Any user scroll interaction disables auto-scroll unconditionally.
    Runnable disableAutoScroll = () -> autoScroll = false;
    bodyScroller.getVerticalBar().addListener(SWT.Selection, e -> disableAutoScroll.run());
    body.addListener(SWT.MouseVerticalWheel, e -> disableAutoScroll.run());
  }

  /** Parsed (title?, body) tuple. */
  private record ParsedSection(String title, String body) {
  }

  private void refreshBody() {
    if (body == null || body.isDisposed()) {
      return;
    }
    List<ParsedSection> parsed;
    try {
      parsed = parseSections(textBuffer.toString());
    } catch (RuntimeException e) {
      // Fallback: render the entire text as a single untitled section rather than crashing the chat view.
      CopilotCore.LOGGER.error("Failed to parse thinking sections", e);
      parsed = List.of(new ParsedSection(null, textBuffer.toString()));
    }

    // Sections are append-only: titles never change and new ones are appended at the tail. Update existing
    // bodies in place and create widgets only for newly parsed sections.
    int reusable = Math.min(parsed.size(), sections.size());
    for (int i = 0; i < reusable; i++) {
      // Defensive fallback if a title at the same index ever differs: rebuild from this index.
      if (!Objects.equals(parsed.get(i).title, sections.get(i).getTitle())) {
        for (int j = sections.size() - 1; j >= i; j--) {
          ThinkingSection s = sections.remove(j);
          if (!s.isDisposed()) {
            s.dispose();
          }
        }
        reusable = i;
        break;
      }
      sections.get(i).setBody(parsed.get(i).body);
    }
    for (int i = reusable; i < parsed.size(); i++) {
      ThinkingSection s = new ThinkingSection(body, parsed.get(i).title, stylingEngine);
      s.setBody(parsed.get(i).body);
      sections.add(s);
    }

    body.requestLayout();
    updateScrollerDuringStreaming();
  }

  /** Resize the scroller to fit content (up to max height) and auto-scroll to bottom if enabled. */
  private void updateScrollerDuringStreaming() {
    if (bodyScroller == null || bodyScroller.isDisposed()) {
      return;
    }
    int contentWidth = bodyScroller.getClientArea().width;
    if (contentWidth <= 0) {
      contentWidth = SWT.DEFAULT;
    }
    int contentHeight = body.computeSize(contentWidth, SWT.DEFAULT).y;
    bodyScroller.setMinSize(contentWidth, contentHeight);

    // Grow with content up to max; avoids blank space when content is small.
    GridData scrollerData = (GridData) bodyScroller.getLayoutData();
    int newHint = Math.min(contentHeight, STREAMING_MAX_HEIGHT);
    if (scrollerData.heightHint != newHint) {
      scrollerData.heightHint = newHint;
    }

    if (state == State.STREAMING && autoScroll) {
      bodyScroller.setOrigin(0, contentHeight);
    } else if (state == State.STREAMING && !autoScroll) {
      // Re-enable auto-scroll if user scrolled back to the bottom.
      int scrollPos = bodyScroller.getOrigin().y;
      int viewportHeight = bodyScroller.getClientArea().height;
      if (scrollPos + viewportHeight >= contentHeight - 10) {
        autoScroll = true;
      }
    }
  }

  /**
   * Split {@code raw} on standalone {@code **Title**} lines (terminator: newline or end of buffer). Text before any
   * title becomes a leading section with {@code title == null}.
   */
  private static List<ParsedSection> parseSections(String raw) {
    List<ParsedSection> result = new ArrayList<>();
    if (raw == null || raw.isEmpty()) {
      return result;
    }
    Matcher matcher = TITLE_PATTERN.matcher(raw);
    String currentTitle = null;
    int cursor = 0;
    while (matcher.find()) {
      // Preserve the body's original whitespace (e.g. leading indentation for code blocks); only strip the
      // trailing newline(s) that visually separate the body from the upcoming title delimiter.
      String body = cursor <= matcher.start()
          ? stripTrailingNewlines(raw.substring(cursor, matcher.start()))
          : "";
      if (currentTitle != null || !body.isBlank()) {
        result.add(new ParsedSection(currentTitle, body));
      }
      currentTitle = matcher.group(1).trim();
      cursor = matcher.end();
      // Swallow the trailing newline(s) after the title so they don't show up at the top of the body.
      while (cursor < raw.length() && (raw.charAt(cursor) == '\n' || raw.charAt(cursor) == '\r')) {
        cursor++;
      }
    }
    // Tail has no following title delimiter; only trim trailing newlines so leading indentation survives.
    String tail = stripTrailingNewlines(raw.substring(cursor));
    if (currentTitle != null || !tail.isBlank()) {
      result.add(new ParsedSection(currentTitle, tail));
    }
    return result;
  }

  private static String stripTrailingNewlines(String s) {
    int end = s.length();
    while (end > 0) {
      char c = s.charAt(end - 1);
      if (c != '\n' && c != '\r') {
        break;
      }
      end--;
    }
    return s.substring(0, end);
  }

  /** Update the header title text. */
  public void setTitle(String text) {
    if (titleLabel == null || titleLabel.isDisposed()) {
      return;
    }
    titleLabel.setText(text == null ? "" : text);
    updateTitleWidthHint();
    titleLabel.requestLayout();
  }

  private void updateTitleWidthHint() {
    if (titleLabel == null || titleLabel.isDisposed() || header == null || header.isDisposed()) {
      return;
    }
    int headerWidth = header.getClientArea().width;
    if (headerWidth <= 0) {
      return;
    }
    GridLayout layout = (GridLayout) header.getLayout();
    int iconWidth = iconLabel != null && !iconLabel.isDisposed() && iconLabel.isVisible()
        ? iconLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x : 0;
    int chevronWidth = chevronLabel != null && !chevronLabel.isDisposed()
        ? chevronLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x : 0;
    int spacing = layout.horizontalSpacing * (layout.numColumns - 1);
    int available = headerWidth - iconWidth - chevronWidth - spacing - layout.marginWidth * 2;
    if (available <= 0) {
      return;
    }
    GridData titleData = (GridData) titleLabel.getLayoutData();
    int natural = titleLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
    int newHint = Math.min(natural, available);
    if (newHint != titleData.widthHint) {
      titleData.widthHint = newHint;
      header.requestLayout();
    }
  }

  private void toggleExpanded() {
    setExpanded(!expanded);
  }

  private void setExpanded(boolean newExpanded) {
    this.expanded = newExpanded;

    Composite bodyContainer = bodyScroller != null ? bodyScroller : body;
    if (bodyContainer != null && !bodyContainer.isDisposed()) {
      GridData data = (GridData) bodyContainer.getLayoutData();
      data.exclude = !expanded;
      bodyContainer.setVisible(expanded);
    }
    updateChevron();
    requestLayout();
    // Refresh the enclosing scroller so the revealed/hidden body height is reachable.
    refreshEnclosingScroller();
  }

  /** Move body from ScrolledComposite to be a direct child of this block. */
  private void unwrapBodyFromScroller() {
    if (bodyScroller == null || bodyScroller.isDisposed() || body == null || body.isDisposed()) {
      return;
    }
    bodyScroller.setContent(null);
    body.setParent(this);
    GridData bodyData = new GridData(SWT.FILL, SWT.FILL, true, false);
    bodyData.exclude = !expanded;
    body.setLayoutData(bodyData);
    body.setVisible(expanded);
    bodyScroller.dispose();
    bodyScroller = null;
  }

  private void updateChevron() {
    if (chevronLabel == null || chevronLabel.isDisposed()) {
      return;
    }
    Image image;
    String tooltip;
    if (expanded) {
      if (downArrowImage == null || downArrowImage.isDisposed()) {
        downArrowImage = UiUtils.buildImageFromPngPath("/icons/chat/down_arrow.png");
      }
      image = downArrowImage;
      tooltip = Messages.thinking_collapseTooltip;
    } else {
      if (rightArrowImage == null || rightArrowImage.isDisposed()) {
        rightArrowImage = UiUtils.buildImageFromPngPath("/icons/chat/right_arrow.png");
      }
      image = rightArrowImage;
      tooltip = Messages.thinking_expandTooltip;
    }
    header.setToolTipText(tooltip);
    chevronLabel.setImage(image);
    chevronLabel.setToolTipText(tooltip);
    if (titleLabel != null && !titleLabel.isDisposed()) {
      titleLabel.setToolTipText(tooltip);
    }
  }

  private void refreshEnclosingScroller() {
    Composite p = getParent();
    while (p != null && !p.isDisposed()) {
      if (p instanceof ChatContentViewer) {
        ((ChatContentViewer) p).refreshLayoutFull();
        return;
      }
      p = p.getParent();
    }
  }

  private void handleDispose() {
    if (cancelledIcon != null && !cancelledIcon.isDisposed()) {
      cancelledIcon.dispose();
      cancelledIcon = null;
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

}

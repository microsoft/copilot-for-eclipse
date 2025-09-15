package com.microsoft.copilot.eclipse.ui.chat.viewers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.persistence.ConversationXmlData;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.chat.ConversationUtils;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A composite that displays a scrollable list of chat history conversations.
 */
public class ChatHistoryViewer extends Composite {
  private ScrolledComposite chatHistoryComposite;
  private Composite chatHistoryListContent;
  private Image chatHistoryBackIcon;
  private IEventBroker eventBroker;

  /**
   * Create the chat history viewer.
   *
   * @param parent the parent composite
   * @param style the style
   * @param conversations the list of conversations to display
   * @param currentConversationId the current conversation id to determine current conversation
   */
  public ChatHistoryViewer(Composite parent, int style, List<ConversationXmlData> conversations,
      String currentConversationId) {
    super(parent, style);
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);

    // Assign CSS id for styling
    this.setData(CssConstants.CSS_ID_KEY, "chat-history-viewer");

    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    setLayout(layout);

    createChatHistoryComposite(conversations, currentConversationId);
  }

  /**
   * Create the chat history composite with scrollable list.
   */
  private void createChatHistoryComposite(List<ConversationXmlData> conversations, String currentConversationId) {
    createBackLabel(this);

    // Create scrollable composite for chat history list only
    chatHistoryComposite = new ScrolledComposite(this, SWT.V_SCROLL | SWT.H_SCROLL);
    chatHistoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    chatHistoryComposite.setExpandHorizontal(true);
    chatHistoryComposite.setExpandVertical(true);

    chatHistoryListContent = new Composite(chatHistoryComposite, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.verticalSpacing = 0;
    chatHistoryListContent.setLayout(layout);

    createChatHistoryList(chatHistoryListContent, conversations, currentConversationId);

    chatHistoryComposite.setContent(chatHistoryListContent);
    // Ensure content size tracks the available width and enables vertical scrolling when needed
    chatHistoryComposite.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        if (chatHistoryComposite == null || chatHistoryComposite.isDisposed() || chatHistoryListContent == null
            || chatHistoryListContent.isDisposed()) {
          return;
        }
        int width = chatHistoryComposite.getClientArea().width;
        chatHistoryComposite.setMinSize(chatHistoryListContent.computeSize(width, SWT.DEFAULT));
      }
    });
    // Initialize min size based on current client area
    int initWidth = chatHistoryComposite.getClientArea().width;
    chatHistoryComposite.setMinSize(chatHistoryListContent.computeSize(initWidth, SWT.DEFAULT));
  }

  private void createBackLabel(Composite parent) {
    GridLayout gl = new GridLayout(2, false);
    gl.horizontalSpacing = 2;
    gl.marginWidth = 0;
    gl.marginTop = 3;
    Composite backLabelComposite = new Composite(parent, SWT.NONE);
    backLabelComposite.setLayout(gl);
    backLabelComposite.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));

    // Use helper method to add back click listener
    addBackClickListener(backLabelComposite);

    // Ensure proper vertical centering
    GridData backLabelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    backLabelComposite.setLayoutData(backLabelData);
    backLabelComposite.addDisposeListener(e -> {
      if (chatHistoryBackIcon != null && !chatHistoryBackIcon.isDisposed()) {
        chatHistoryBackIcon.dispose();
      }
    });

    Label icon = new Label(backLabelComposite, SWT.NONE);
    this.chatHistoryBackIcon = UiUtils.buildImageFromPngPath(
        UiUtils.isDarkTheme() ? "/icons/chat/back_arrow_grey.png" : "/icons/chat/back_arrow.png");
    icon.setImage(chatHistoryBackIcon);
    icon.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
    // Center the icon vertically
    icon.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    addBackClickListener(icon);

    Label label = new Label(backLabelComposite, SWT.LEFT);
    // Center the text vertically and don't grab excess vertical space
    label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    label.setText(Messages.chat_historyView_backButton);
    label.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
    addBackClickListener(label);

    // Create a separator composite with border
    Composite separator = new Composite(parent, SWT.NONE);
    separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    separator.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        int borderWidth = 1;
        Color borderColor = CssConstants.getTopBannerBorderColor(getDisplay());
        gc.setForeground(borderColor);
        gc.setLineWidth(borderWidth);
        int width = separator.getClientArea().width;
        gc.drawLine(0, 1, Math.max(0, width - 1), 1); // Draw line at y=1 for some padding
      }
    });
    GridData separatorData = new GridData(SWT.FILL, SWT.TOP, true, false);
    separatorData.heightHint = 2;
    separator.setLayoutData(separatorData);
  }

  private void createChatHistoryList(Composite parent, List<ConversationXmlData> conversations,
      String currentConversationId) {
    boolean hasValidCurrentId = StringUtils.isNotBlank(currentConversationId);
    boolean hasCurrentConversation = hasValidCurrentId && conversations.stream()
        .anyMatch(conversation -> StringUtils.equals(conversation.getConversationId(), currentConversationId));

    // Add "New Chat" item at the top if no current conversation exists
    if (!hasCurrentConversation) {
      createConversationItem(parent, Messages.chat_topBanner_chatHistoryItem_newChat, true,
          Messages.chat_topBanner_chatHistoryItem_newChatTime_Now, null, false, false);
    }

    String previousDateStr = null;
    boolean isFirstDateStrInList = true;

    for (int i = 0; i < conversations.size(); i++) {
      ConversationXmlData conversation = conversations.get(i);
      boolean isCurrentConversation = hasValidCurrentId
          && StringUtils.equals(conversation.getConversationId(), currentConversationId);

      String title = getDisplayTitle(conversation);
      String dateStr = getDateString(conversation);
      boolean isFirstInDateGroup = !dateStr.equals(previousDateStr);
      String displayDateStr = isFirstInDateGroup ? dateStr : "";

      // Calculate border and margin requirements
      boolean needsUpperBorder = isFirstInDateGroup && !isFirstDateStrInList && !dateStr.isEmpty();
      boolean needsTopMargin = needsUpperBorder;
      createConversationItem(parent, title, isCurrentConversation, displayDateStr, conversation, needsUpperBorder,
          needsTopMargin);

      if (isFirstInDateGroup && !dateStr.isEmpty()) {
        isFirstDateStrInList = false;
      }
      previousDateStr = dateStr;
    }
  }

  private String getDisplayTitle(ConversationXmlData conversation) {
    String title = conversation.getTitle();
    return StringUtils.isNotBlank(title) ? title
        : Messages.chat_topBanner_chatHistoryItem_untitledConversation_placeholder;
  }

  private String getDateString(ConversationXmlData conversation) {
    Instant dateToFormat = conversation.getLastMessageDate() != null ? conversation.getLastMessageDate()
        : conversation.getCreationDate();
    return dateToFormat != null ? formatRelativeDateTime(dateToFormat) : "";
  }

  /**
   * Create a conversation item in the chat history list, with optional upper border and margins.
   */
  private void createConversationItem(Composite parent, String title, boolean isCurrent, String dateStr,
      ConversationXmlData conversation, boolean needsUpperBorder, boolean needsTopMargin) {
    // Two columns: [leftStack(title + optional "(Current)")] [date]
    GridLayout conversationLayout = new GridLayout(2, false);
    conversationLayout.marginWidth = 5;
    conversationLayout.marginHeight = 5;
    conversationLayout.horizontalSpacing = 5;
    Composite conversationItem = new Composite(parent, SWT.NONE);
    conversationItem.setLayout(conversationLayout);
    setConversationItemCssClass(conversationItem, isCurrent, false);

    GridData conversationItemData = new GridData(SWT.FILL, SWT.TOP, true, false);
    conversationItemData.horizontalIndent = 5;

    // Add margins above border for better visual spacing
    if (needsTopMargin) {
      conversationItemData.verticalIndent = 6;
    }

    conversationItem.setLayoutData(conversationItemData);
    conversationItem.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
    // Let children inherit background from this row for uniform color
    conversationItem.setBackgroundMode(SWT.INHERIT_DEFAULT);

    // Add upper border line for non-Today groups
    if (needsUpperBorder) {
      conversationItem.addPaintListener(new PaintListener() {
        @Override
        public void paintControl(PaintEvent e) {
          GC gc = e.gc;
          Color borderColor = CssConstants.getTopBannerBorderColor(getDisplay());
          gc.setForeground(borderColor);
          gc.setLineWidth(1);
          int width = conversationItem.getClientArea().width;
          gc.drawLine(0, 0, width, 0); // Draw line at y=0 (top edge)
        }
      });
    }

    // Left stack: [title][(Current)]
    GridLayout leftLayout = new GridLayout(2, false);
    leftLayout.marginHeight = 0;
    leftLayout.marginWidth = 0;
    leftLayout.horizontalSpacing = 5;
    Composite leftStack = new Composite(conversationItem, SWT.NONE);
    leftStack.setLayout(leftLayout);
    leftStack.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Title label
    Label titleLabel = new Label(leftStack, SWT.NONE);
    titleLabel.setText(title);
    titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    // Store original title for truncation logic
    final String originalTitle = title;

    // Optional "(Current)" label
    Label currentLabel = new Label(leftStack, SWT.NONE);
    if (isCurrent) {
      currentLabel.setText(Messages.chat_topBanner_chatHistoryItem_currentConversation_label);
      applyCssClass(currentLabel, "chat-history-item-current-label");
      currentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    } else {
      GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
      gd.exclude = true;
      currentLabel.setLayoutData(gd);
      currentLabel.setVisible(false);
    }

    // Date label (right-aligned)
    Label dateLabel = new Label(conversationItem, SWT.NONE);
    dateLabel.setText(dateStr);
    GridData dateData = new GridData(SWT.END, SWT.CENTER, false, false);
    dateLabel.setLayoutData(dateData);
    applyCssClass(dateLabel, "chat-history-item-date-label");

    // Add resize listener to handle title text truncation
    addTitleTruncationListener(conversationItem, titleLabel, originalTitle, currentLabel, dateLabel, isCurrent);

    // Add listeners
    addConversationClickListeners(conversationItem, leftStack, titleLabel, currentLabel, dateLabel, conversation);
    addConversationHoverEffects(conversationItem, leftStack, titleLabel, currentLabel, dateLabel, isCurrent);

    // On Windows, draw a border around the current item using a PaintListener (SWT can't do this via CSS)
    if (PlatformUtils.isWindows() && isCurrent) {
      addCurrentItemBorder(conversationItem);
    }
  }

  private void setConversationItemCssClass(Composite conversationItem, boolean isCurrent, boolean isHover) {
    StringBuilder sb = new StringBuilder("chat-history-item");
    if (isCurrent) {
      sb.append(" chat-history-item-current");
    }
    if (isHover) {
      sb.append(" chat-history-item-hover");
    }
    applyCssClass(conversationItem, sb.toString());
  }

  private void applyCssClass(Control control, String classnames) {
    IStylingEngine engine = PlatformUI.getWorkbench().getService(IStylingEngine.class);
    if (engine != null) {
      engine.setClassname(control, classnames);
      engine.style(control);
    } else {
      control.setData(CssConstants.CSS_CLASS_NAME_KEY, classnames);
    }
  }

  /**
   * Helper method to add back click listeners to a widget.
   */
  private void addBackClickListener(org.eclipse.swt.widgets.Widget widget) {
    widget.addListener(SWT.MouseDown, event -> {
      if (eventBroker != null) {
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_HISTORY_BACK_CLICKED, null);
      }
    });
  }

  /**
   * Helper method to add conversation click listeners to all conversation item widgets.
   */
  private void addConversationClickListeners(Composite conversationItem, Composite leftStack, Label titleLabel,
      Label currentLabel, Label dateLabel, ConversationXmlData conversation) {
    Listener clickListener = event -> {
      // Confirm switching conversations (may prompt about unhandled file changes)
      if (!ConversationUtils.confirmSwitchChat()) {
        return; // user cancelled
      }
      if (eventBroker != null) {
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_HISTORY_CONVERSATION_SELECTED, conversation);
      }
    };

    conversationItem.addListener(SWT.MouseDown, clickListener);
    leftStack.addListener(SWT.MouseDown, clickListener);
    titleLabel.addListener(SWT.MouseDown, clickListener);
    currentLabel.addListener(SWT.MouseDown, clickListener);
    dateLabel.addListener(SWT.MouseDown, clickListener);
  }

  /**
   * Helper method to add hover effects to all conversation item widgets.
   */
  private void addConversationHoverEffects(Composite conversationItem, Composite leftStack, Label titleLabel,
      Label currentLabel, Label dateLabel, boolean isCurrent) {
    Listener enterListener = event -> setConversationItemCssClass(conversationItem, isCurrent, true);
    Listener exitListener = event -> setConversationItemCssClass(conversationItem, isCurrent, false);

    conversationItem.addListener(SWT.MouseEnter, enterListener);
    conversationItem.addListener(SWT.MouseExit, exitListener);
    leftStack.addListener(SWT.MouseEnter, enterListener);
    leftStack.addListener(SWT.MouseExit, exitListener);
    titleLabel.addListener(SWT.MouseEnter, enterListener);
    titleLabel.addListener(SWT.MouseExit, exitListener);
    currentLabel.addListener(SWT.MouseEnter, enterListener);
    currentLabel.addListener(SWT.MouseExit, exitListener);
    dateLabel.addListener(SWT.MouseEnter, enterListener);
    dateLabel.addListener(SWT.MouseExit, exitListener);
  }

  // Draw a 1px border around the current conversation item on Windows using SWT painting
  private void addCurrentItemBorder(Composite conversationItem) {
    final Color borderColor = CssConstants.getWindowsChatHistoryCurrentItemBorderColor(conversationItem.getDisplay());

    conversationItem.addDisposeListener(e -> {
      if (borderColor != null && !borderColor.isDisposed()) {
        borderColor.dispose();
      }
    });

    conversationItem.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        e.gc.setForeground(borderColor);
        e.gc.setLineWidth(1);
        int w = Math.max(0, e.width - 1);
        int h = Math.max(0, e.height - 1);
        // Draw the rectangle border within the client area
        e.gc.drawRectangle(0, 0, w, h);
      }
    });
  }

  /**
   * Converts an Instant to a relative date string with time. Examples: "Today", "Yesterday", "2 days ago", "1 week ago"
   *
   * @param instant the instant to format
   * @return formatted relative date string with time, or empty string if instant is null
   */
  private static String formatRelativeDateTime(Instant instant) {
    if (instant == null) {
      return "";
    }

    LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    LocalDate messageDate = dateTime.toLocalDate();
    LocalDate today = LocalDate.now();

    long daysDifference = ChronoUnit.DAYS.between(messageDate, today);

    if (daysDifference == 0) {
      return Messages.chat_historyView_dateFormat_today;
    } else if (daysDifference == 1) {
      return Messages.chat_historyView_dateFormat_yesterday;
    } else if (daysDifference < 7) {
      return Messages.chat_historyView_dateFormat_daysAgo.replace("{0}", Long.toString(daysDifference));
    } else if (daysDifference < 14) {
      return Messages.chat_historyView_dateFormat_oneWeekAgo;
    } else {
      long weeksDifference = daysDifference / 7;
      return Messages.chat_historyView_dateFormat_weeksAgo.replace("{0}", Long.toString(weeksDifference));
    }
  }

  /**
   * Add a resize listener to handle title text truncation when the available width is insufficient.
   */
  private void addTitleTruncationListener(Composite conversationItem, Label titleLabel, String originalTitle,
      Label currentLabel, Label dateLabel, boolean isCurrent) {
    conversationItem.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        if (conversationItem.isDisposed() || titleLabel.isDisposed()) {
          return;
        }

        updateTitleText(conversationItem, titleLabel, originalTitle, currentLabel, dateLabel, isCurrent);
      }
    });

    // Initial call to set the text properly on creation
    conversationItem.getDisplay().asyncExec(() -> {
      if (!conversationItem.isDisposed() && !titleLabel.isDisposed()) {
        updateTitleText(conversationItem, titleLabel, originalTitle, currentLabel, dateLabel, isCurrent);
      }
    });
  }

  /**
   * Update the title text based on available width, truncating with "..." if necessary.
   */
  private void updateTitleText(Composite conversationItem, Label titleLabel, String originalTitle, Label currentLabel,
      Label dateLabel, boolean isCurrent) {
    if (originalTitle == null || originalTitle.isEmpty()) {
      return;
    }

    GC gc = new GC(titleLabel);
    try {
      // Calculate available width for the title
      int conversationItemWidth = conversationItem.getClientArea().width;
      int margins = 10; // Layout margins (5 on each side)
      int horizontalSpacing = 5; // Spacing between title and current label
      int dateWidth = gc.textExtent(dateLabel.getText()).x;

      int currentLabelWidth = 0;
      if (isCurrent && !currentLabel.isDisposed() && currentLabel.getVisible()) {
        currentLabelWidth = gc.textExtent(currentLabel.getText()).x + horizontalSpacing;
      }

      int availableWidth = conversationItemWidth - margins - dateWidth - currentLabelWidth - 20;

      if (availableWidth <= 0) {
        titleLabel.setText(Messages.chat_historyView_textTruncation_ellipsis);
        return;
      }

      // Check if original title fits
      int originalWidth = gc.textExtent(originalTitle).x;
      if (originalWidth <= availableWidth) {
        titleLabel.setText(originalTitle);
        return;
      }

      // Truncate with ellipsis
      String ellipsis = Messages.chat_historyView_textTruncation_ellipsis;
      int ellipsisWidth = gc.textExtent(ellipsis).x;
      int maxTextWidth = availableWidth - ellipsisWidth;

      if (maxTextWidth <= 0) {
        titleLabel.setText(ellipsis);
        return;
      }

      // Binary search to find the maximum number of characters that fit
      int left = 0;
      int right = originalTitle.length();
      String bestFit = ellipsis;

      while (left <= right) {
        int mid = (left + right) / 2;
        String testText = originalTitle.substring(0, mid);
        int testWidth = gc.textExtent(testText).x;

        if (testWidth <= maxTextWidth) {
          bestFit = testText + ellipsis;
          left = mid + 1;
        } else {
          right = mid - 1;
        }
      }

      titleLabel.setText(bestFit);

    } finally {
      gc.dispose();
    }
  }
}

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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.persistence.ConversationPersistenceManager;
import com.microsoft.copilot.eclipse.core.persistence.ConversationXmlData;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ConversationUtils;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A composite that displays a scrolLabel list of chat history conversations.
 */
public class ChatHistoryViewer extends Composite {
  private ScrolledComposite chatHistoryComposite;
  private Composite chatHistoryListContent;
  private Image chatHistoryBackIcon;
  private IEventBroker eventBroker;
  private String currentConversationId;
  private IStylingEngine stylingEngine;
  private final Cursor handCursor;
  private int labelAvaliableWidthWithoutCurrentAndDateLabel;

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

    // Cache frequently accessed objects once
    this.handCursor = Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND);
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    this.stylingEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);
    this.currentConversationId = currentConversationId;
    this.labelAvaliableWidthWithoutCurrentAndDateLabel = 0;

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
   * Create the chat history composite with scrolLabel list.
   */
  private void createChatHistoryComposite(List<ConversationXmlData> conversations, String currentConversationId) {
    createBackLabel(this);

    // Create scrolLabel composite for chat history list only
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
    // Ensure content size tracks the avaiLabel width and enables vertical scrolling when needed
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
    backLabelComposite.setCursor(handCursor);

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
    icon.setCursor(handCursor);
    // Center the icon vertically
    icon.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    addBackClickListener(icon);

    Label label = new Label(backLabelComposite, SWT.LEFT);
    // Center the text vertically and don't grab excess vertical space
    label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    label.setText(Messages.chat_historyView_backButton);
    label.setCursor(handCursor);
    addBackClickListener(label);

    // Create a separator composite with border
    Composite separator = new Composite(parent, SWT.NONE);
    separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    final Color borderColor = CssConstants.getTopBannerBorderColor(Display.getCurrent());
    separator.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        gc.setForeground(borderColor);
        gc.setLineWidth(1);
        int width = separator.getClientArea().width;
        gc.drawLine(0, 1, Math.max(0, width - 1), 1);
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
    // Three columns: [leftStack(title + optional "(Current)")] [date] [actionsComposite]
    GridLayout conversationLayout = new GridLayout(3, false);
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
    conversationItem.setCursor(handCursor);

    final Color borderColor = needsUpperBorder ? CssConstants.getTopBannerBorderColor(Display.getCurrent()) : null;

    // Add upper border line for non-Today groups
    if (needsUpperBorder && borderColor != null) {
      conversationItem.addPaintListener(new PaintListener() {
        @Override
        public void paintControl(PaintEvent e) {
          GC gc = e.gc;
          gc.setForeground(borderColor);
          gc.setLineWidth(1);
          int width = conversationItem.getClientArea().width;
          gc.drawLine(0, 0, width, 0);
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

    // Title label/editor composite - this will switch between label and text editor with enter icon
    GridLayout titleLayout = new GridLayout(2, false); // 2 columns for text editor + enter icon
    titleLayout.marginHeight = 0;
    titleLayout.marginWidth = 0;
    Composite titleComposite = new Composite(leftStack, SWT.NONE);
    titleComposite.setLayout(titleLayout);
    titleComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    // Title label (initially visible)
    Label titleLabel = new Label(titleComposite, SWT.NONE);
    titleLabel.setText(title);
    // Store original title for truncation restoration
    titleLabel.setData("originalTitle", title);
    GridData titleLabelData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    titleLabelData.horizontalSpan = 2; // Span both columns when label is visible
    titleLabel.setLayoutData(titleLabelData);

    // Title text editor (initially hidden)
    Text titleEditor = new Text(titleComposite, SWT.SINGLE | SWT.BORDER);
    titleEditor.setText(title);
    GridData editorData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    editorData.exclude = true;
    titleEditor.setLayoutData(editorData);
    titleEditor.setVisible(false);

    // Enter icon (initially hidden, shown only when editor is visible)
    Label enterIcon = new Label(titleComposite, SWT.NONE);
    Image enterImage = UiUtils.buildImageFromPngPath("/icons/chat/enter.png");
    enterIcon.setImage(enterImage);
    enterIcon.setToolTipText(Messages.chat_historyView_enterIcon_tooltip);
    GridData enterIconData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
    enterIconData.exclude = true;
    enterIcon.setLayoutData(enterIconData);
    enterIcon.setVisible(false);

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

    // Actions composite (initially hidden, shown on hover)
    Composite actionsComposite = createActionsComposite(conversationItem, conversation, titleLabel, titleEditor,
        titleComposite, enterIcon);

    // Add resize listener to handle title text truncation
    addTitleTruncationListener(conversationItem, titleLabel, currentLabel, dateLabel, isCurrent);

    Listener sharedClickListener = createConversationClickListener(conversation);
    HoverListeners hoverListeners = createConversationHoverListeners(conversationItem, isCurrent, actionsComposite,
        titleComposite, conversation);

    // Apply listeners to all relevant controls at once
    Control[] clickableControls = { conversationItem, leftStack, titleLabel, currentLabel, dateLabel };
    for (Control control : clickableControls) {
      control.addListener(SWT.MouseDown, sharedClickListener);
      control.addListener(SWT.MouseEnter, hoverListeners.enterListener);
      control.addListener(SWT.MouseExit, hoverListeners.exitListener);
    }

    // Add hover listeners to actions composite and its children
    actionsComposite.addListener(SWT.MouseEnter, hoverListeners.enterListener);
    actionsComposite.addListener(SWT.MouseExit, hoverListeners.exitListener);
    for (Control child : actionsComposite.getChildren()) {
      child.addListener(SWT.MouseEnter, hoverListeners.enterListener);
      child.addListener(SWT.MouseExit, hoverListeners.exitListener);
    }

    // On Windows, draw a border around the current item using a PaintListener (SWT can't do this via CSS)
    if (PlatformUtils.isWindows() && isCurrent) {
      addCurrentItemBorder(conversationItem);
    }

    conversationItem.addDisposeListener(e -> {
      if (enterImage != null && !enterImage.isDisposed()) {
        enterImage.dispose();
      }
    });
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
    if (stylingEngine != null) {
      stylingEngine.setClassname(control, classnames);
      stylingEngine.style(control);
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

  private Listener createConversationClickListener(ConversationXmlData conversation) {
    return event -> {
      if (!ConversationUtils.confirmSwitchChat()) {
        return;
      }
      if (eventBroker != null) {
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_HISTORY_CONVERSATION_SELECTED, conversation);
      }
    };
  }

  // Helper class to group hover listeners
  private static class HoverListeners {
    final Listener enterListener;
    final Listener exitListener;

    HoverListeners(Listener enter, Listener exit) {
      this.enterListener = enter;
      this.exitListener = exit;
    }
  }

  // Create reusable hover listeners
  private HoverListeners createConversationHoverListeners(Composite conversationItem, boolean isCurrent,
      Composite actionsComposite, Composite titleComposite, ConversationXmlData conversation) {

    Listener enterListener = event -> {
      setConversationItemCssClass(conversationItem, isCurrent, true);

      if (conversation != null && !isInEditMode(titleComposite)) {
        GridData actionsData = (GridData) actionsComposite.getLayoutData();
        actionsData.exclude = false;
        actionsComposite.setVisible(true);
        conversationItem.requestLayout();
      }
    };

    Listener exitListener = event -> {
      setConversationItemCssClass(conversationItem, isCurrent, false);
      GridData actionsData = (GridData) actionsComposite.getLayoutData();
      actionsData.exclude = true;
      actionsComposite.setVisible(false);
      conversationItem.requestLayout();
    };

    return new HoverListeners(enterListener, exitListener);
  }

  // Extract edit mode check to reduce code duplication
  private boolean isInEditMode(Composite titleComposite) {
    Control[] titleChildren = titleComposite.getChildren();
    if (titleChildren.length > 1) {
      Text titleEditor = (Text) titleChildren[1];
      return titleEditor.getVisible();
    }
    return false;
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
    } else if (daysDifference < 30) {
      long weeksDifference = daysDifference / 7;
      return Messages.chat_historyView_dateFormat_weeksAgo.replace("{0}", Long.toString(weeksDifference));
    }

    long monthsDifference = ChronoUnit.MONTHS.between(messageDate, today);
    if (monthsDifference == 1) {
      return Messages.chat_historyView_dateFormat_oneMonthAgo;
    } else {
      return Messages.chat_historyView_dateFormat_monthsAgo.replace("{0}", Long.toString(monthsDifference));
    }
  }

  /**
   * Add a resize listener to handle title text truncation when the avaiLabel width is insufficient.
   */
  private void addTitleTruncationListener(Composite conversationItem, Label titleLabel, Label currentLabel,
      Label dateLabel, boolean isCurrent) {
    conversationItem.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        if (conversationItem.isDisposed() || titleLabel.isDisposed()) {
          return;
        }

        updateTitleText(conversationItem, titleLabel, currentLabel, dateLabel, isCurrent);
      }
    });

    // Initial call to set the text properly on creation
    conversationItem.getDisplay().asyncExec(() -> {
      if (!conversationItem.isDisposed() && !titleLabel.isDisposed()) {
        updateTitleText(conversationItem, titleLabel, currentLabel, dateLabel, isCurrent);
      }
    });
  }

  // Optimize title truncation by caching measurements
  private void updateTitleText(Composite conversationItem, Label titleLabel, Label currentLabel, Label dateLabel,
      boolean isCurrent) {

    if (conversationItem.isDisposed() || titleLabel.isDisposed()) {
      return;
    }

    GC gc = new GC(titleLabel);
    try {
      int avaiLabelWidth = calculateAvaiLabelWidth(conversationItem, dateLabel, currentLabel, isCurrent, gc);

      if (avaiLabelWidth <= 0) {
        titleLabel.setText(Messages.chat_historyView_textTruncation_ellipsis);
        return;
      }

      String originalTitle = (String) titleLabel.getData("originalTitle");
      if (originalTitle == null) {
        originalTitle = titleLabel.getText();
        titleLabel.setData("originalTitle", originalTitle); // Cache for future use
      }

      // Early return if title fits
      int originalWidth = gc.textExtent(originalTitle).x;
      if (originalWidth <= avaiLabelWidth) {
        if (!titleLabel.getText().equals(originalTitle)) {
          titleLabel.setText(originalTitle);
        }
        return;
      }

      // Optimize truncation logic
      String ellipsis = Messages.chat_historyView_textTruncation_ellipsis;
      int ellipsisWidth = gc.textExtent(ellipsis).x;
      int maxTextWidth = avaiLabelWidth - ellipsisWidth;

      if (maxTextWidth <= 0) {
        titleLabel.setText(ellipsis);
        return;
      }

      // Use more efficient character-by-character approach for short titles
      if (originalTitle.length() < 16) {
        String truncated = truncateByCharacter(gc, originalTitle, maxTextWidth);
        titleLabel.setText(truncated + ellipsis);
      } else {
        // Use binary search for longer titles
        String truncated = truncateByBinarySearch(gc, originalTitle, maxTextWidth);
        titleLabel.setText(truncated + ellipsis);
      }

    } finally {
      gc.dispose();
    }
  }

  /**
   * Calculate the avaiLabel width for the title text based on the conversation item layout.
   */
  private int calculateAvaiLabelWidth(Composite conversationItem, Label dateLabel, Label currentLabel,
      boolean isCurrent, GC gc) {

    boolean isEmptyDateLabel = gc.textExtent(dateLabel.getText()).x == 0;

    // For non-current conversations with empty date labels, use cached or calculate basic width
    if (!isCurrent && isEmptyDateLabel) {
      if (labelAvaliableWidthWithoutCurrentAndDateLabel == 0) {
        return calculateBasicAvaiLabelWidth(conversationItem);
      } else {
        return labelAvaliableWidthWithoutCurrentAndDateLabel;
      }
    }

    // Calculate avaiLabel width considering all elements
    return calculateFullAvaiLabelWidth(conversationItem, dateLabel, currentLabel, isCurrent, gc);
  }

  /**
   * Calculate basic avaiLabel width without considering date and current labels.
   */
  private int calculateBasicAvaiLabelWidth(Composite conversationItem) {
    int conversationItemWidth = conversationItem.getClientArea().width;
    int margins = 10;
    int extraPadding = 20;
    return conversationItemWidth - margins - extraPadding;
  }

  /**
   * Calculate avaiLabel width considering date label and current label.
   */
  private int calculateFullAvaiLabelWidth(Composite conversationItem, Label dateLabel, Label currentLabel,
      boolean isCurrent, GC gc) {

    int conversationItemWidth = conversationItem.getClientArea().width;
    int margins = 10;
    int horizontalSpacing = 5;
    int extraPadding = 20;

    // Measure date label width
    int dateWidth = gc.textExtent(dateLabel.getText()).x;

    // Measure current label width if applicable
    int currentLabelWidth = 0;
    if (isCurrent && !currentLabel.isDisposed() && currentLabel.getVisible()) {
      currentLabelWidth = gc.textExtent(currentLabel.getText()).x + horizontalSpacing;
    }

    return conversationItemWidth - margins - dateWidth - currentLabelWidth - extraPadding;
  }

  // Helper methods for text truncation
  private String truncateByCharacter(GC gc, String text, int maxWidth) {
    for (int i = text.length(); i > 0; i--) {
      String substring = text.substring(0, i);
      if (gc.textExtent(substring).x <= maxWidth) {
        return substring;
      }
    }
    return "";
  }

  private String truncateByBinarySearch(GC gc, String text, int maxWidth) {
    int left = 0;
    int right = text.length();
    String bestFit = "";

    while (left <= right) {
      int mid = (left + right) / 2;
      String testText = text.substring(0, mid);
      int testWidth = gc.textExtent(testText).x;

      if (testWidth <= maxWidth) {
        bestFit = testText;
        left = mid + 1;
      } else {
        right = mid - 1;
      }
    }
    return bestFit;
  }

  /**
   * Create the actions composite with edit and delete icons for a conversation item.
   */
  private Composite createActionsComposite(Composite parent, ConversationXmlData conversation, Label titleLabel,
      Text titleEditor, Composite titleComposite, Label enterIcon) {
    GridLayout actionsLayout = new GridLayout(2, false);
    actionsLayout.marginHeight = 0;
    actionsLayout.marginWidth = 0;
    actionsLayout.horizontalSpacing = 5;
    Composite actionsComposite = new Composite(parent, SWT.NONE);
    actionsComposite.setLayout(actionsLayout);

    GridData actionsData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    actionsData.exclude = true;
    actionsComposite.setLayoutData(actionsData);
    actionsComposite.setVisible(false);

    // Edit icon
    Label editIcon = new Label(actionsComposite, SWT.NONE);
    Image editImage = UiUtils.buildImageFromPngPath("/icons/chat/chat_history_edit.png");
    editIcon.setImage(editImage);
    editIcon.setToolTipText(Messages.chat_historyView_editIcon_tooltip);
    editIcon.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
    GridData editIconData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
    editIcon.setLayoutData(editIconData);

    // Delete icon - only show if this is not the current conversation
    Label deleteIcon = null;
    Image deleteImage = null;
    boolean isCurrentConversation = conversation != null
        && StringUtils.equals(conversation.getConversationId(), currentConversationId);

    if (!isCurrentConversation) {
      deleteIcon = new Label(actionsComposite, SWT.NONE);
      deleteImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE);
      deleteIcon.setImage(deleteImage);
      deleteIcon.setToolTipText(Messages.chat_historyView_deleteIcon_tooltip);
      deleteIcon.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
      GridData deleteIconData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
      deleteIcon.setLayoutData(deleteIconData);
    } else {
      // Adjust layout to only have 1 column when delete icon is not shown
      actionsLayout.numColumns = 1;
    }

    // Edit icon click listener
    editIcon.addListener(SWT.MouseDown, event -> {
      if (conversation == null) {
        return; // Skip for "New Chat" item
      }

      event.doit = false; // Prevent event propagation

      // Switch to title editor mode
      switchTitleLabelModes(titleLabel, titleEditor, titleComposite, actionsComposite, parent, true);
    });

    // Delete icon click listener (only if delete icon exists)
    if (deleteIcon != null) {
      deleteIcon.addListener(SWT.MouseDown, event -> {
        if (conversation == null) {
          return; // Skip for "New Chat" item
        }

        event.doit = false; // Prevent event propagation

        // Delete conversation
        deleteConversation(conversation, parent);
      });
    }

    // Enter icon click listener - setup once here
    enterIcon.addListener(SWT.MouseDown, event -> {
      if (conversation == null) {
        return; // Skip for "New Chat" item
      }

      // Apply title changes using the common method
      applyTitleChanges(titleEditor, titleLabel, titleComposite, actionsComposite, parent, conversation);
    });

    // Title editor listeners for handling edit completion
    setupTitleEditor(titleEditor, titleLabel, titleComposite, actionsComposite, parent, conversation);

    actionsComposite.addDisposeListener(e -> {
      if (editImage != null && !editImage.isDisposed()) {
        editImage.dispose();
      }
    });

    return actionsComposite;
  }

  /**
   * Common method to apply title changes from the editor to the label and persistence. Used by both Enter key handler
   * and enter icon click handler.
   */
  private void applyTitleChanges(Text titleEditor, Label titleLabel, Composite titleComposite,
      Composite actionsComposite, Composite parent, ConversationXmlData conversation) {
    String newTitle = titleEditor.getText().trim();
    if (!newTitle.isEmpty() && conversation != null) {
      // Update title in persistence
      try {
        ConversationPersistenceManager persistenceManager = CopilotUi.getPlugin().getChatServiceManager()
            .getPersistenceManager();
        persistenceManager.updateConversationTitle(conversation.getConversationId(), newTitle);

        // Update the label text and stored original title
        titleLabel.setText(newTitle);
        titleLabel.setData("originalTitle", newTitle);
        titleLabel.requestLayout();

        // Update the conversation object for consistency
        conversation.setTitle(newTitle);

        // Send event to chat view if this is the current conversation
        if (eventBroker != null && StringUtils.equals(conversation.getConversationId(), currentConversationId)) {
          eventBroker.post(CopilotEventConstants.TOPIC_CHAT_CONVERSATION_TITLE_UPDATED, newTitle);
        }

        // Switch back to view mode
        switchTitleLabelModes(titleLabel, titleEditor, titleComposite, actionsComposite, parent, false);

      } catch (Exception e) {
        // On error, revert to original title
        titleEditor.setText(titleLabel.getText());
        switchTitleLabelModes(titleLabel, titleEditor, titleComposite, actionsComposite, parent, false);
      }
    } else {
      // Invalid title, revert
      titleEditor.setText(titleLabel.getText());
      switchTitleLabelModes(titleLabel, titleEditor, titleComposite, actionsComposite, parent, false);
    }
  }

  /**
   * Switch from title label to title editor mode.
   */
  private void switchTitleLabelModes(Label titleLabel, Text titleEditor, Composite titleComposite,
      Composite actionsComposite, Composite parent, boolean isEditMode) {
    // Hide title label, show editor
    GridData labelData = (GridData) titleLabel.getLayoutData();
    labelData.exclude = isEditMode ? true : false;
    titleLabel.setVisible(isEditMode ? false : true);

    GridData editorData = (GridData) titleEditor.getLayoutData();
    editorData.exclude = isEditMode ? false : true;
    titleEditor.setVisible(isEditMode ? true : false);

    // Show enter icon when editor is visible
    Label enterIcon = (Label) titleComposite.getChildren()[2]; // Enter icon is the third child
    GridData enterIconData = (GridData) enterIcon.getLayoutData();
    enterIconData.exclude = isEditMode ? false : true;
    enterIcon.setVisible(isEditMode ? true : false);

    // Change titleComposite layout to grab horizontal space when editor is visible
    GridData titleCompositeData = (GridData) titleComposite.getLayoutData();
    titleCompositeData.horizontalAlignment = SWT.FILL;
    titleCompositeData.grabExcessHorizontalSpace = isEditMode ? true : false;

    // Hide actions composite and exclude it from layout
    GridData actionsData = (GridData) actionsComposite.getLayoutData();
    actionsData.exclude = true;
    actionsComposite.setVisible(false);

    if (isEditMode) {
      titleEditor.setFocus();
      titleEditor.selectAll();
    }

    titleComposite.requestLayout();
  }

  /**
   * Setup title editor listeners for handling edit completion.
   */
  private void setupTitleEditor(Text titleEditor, Label titleLabel, Composite titleComposite,
      Composite actionsComposite, Composite parent, ConversationXmlData conversation) {
    // Handle Enter key to save changes
    titleEditor.addListener(SWT.KeyDown, event -> {
      if (event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR) {
        // Apply title changes using the common method
        applyTitleChanges(titleEditor, titleLabel, titleComposite, actionsComposite, parent, conversation);
      } else if (event.keyCode == SWT.ESC) {
        // Cancel editing on Escape
        titleEditor.setText(titleLabel.getText());
        switchTitleLabelModes(titleLabel, titleEditor, titleComposite, actionsComposite, parent, false);
      }
    });

    // Handle focus lost to save changes
    titleEditor.addListener(SWT.FocusOut, event -> {
      titleEditor.setText(titleLabel.getText());
      switchTitleLabelModes(titleLabel, titleEditor, titleComposite, actionsComposite, parent, false);
    });
  }

  /**
   * Delete a conversation after user confirmation.
   */
  private void deleteConversation(ConversationXmlData conversation, Composite parent) {
    // Show confirmation dialog
    boolean confirmed = MessageDialog.openConfirm(getShell(), "Delete Conversation",
        "Are you sure you want to delete this conversation? This action cannot be undone.");

    if (confirmed) {
      try {
        ConversationPersistenceManager persistenceManager = CopilotUi.getPlugin().getChatServiceManager()
            .getPersistenceManager();
        persistenceManager.removeConversationById(conversation.getConversationId());

        // Remove the conversation item from UI
        parent.dispose();

        // Refresh the parent layout
        if (chatHistoryListContent != null && !chatHistoryListContent.isDisposed()) {
          chatHistoryListContent.requestLayout();
          chatHistoryComposite.setMinSize(chatHistoryListContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        }

      } catch (Exception e) {
        // Show error dialog
        MessageDialog.openError(getShell(), "Error", "Failed to delete conversation: " + e.getMessage());
      }
    }
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
}

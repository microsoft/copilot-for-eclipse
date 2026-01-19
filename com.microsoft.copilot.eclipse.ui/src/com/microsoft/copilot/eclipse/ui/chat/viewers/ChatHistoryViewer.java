package com.microsoft.copilot.eclipse.ui.chat.viewers;

import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
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
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A composite that displays a scrolLabel list of chat history conversations.
 */
public class ChatHistoryViewer extends Composite {
  private ScrolledComposite chatHistoryComposite;
  private Composite chatHistoryListContent;
  private Image backImage;
  private Image enterImage;
  private Image editImage;
  private IEventBroker eventBroker;
  private String currentConversationId;
  private IStylingEngine stylingEngine;
  private ChatFontService chatFontService;
  private final Cursor handCursor;

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
    this.chatFontService = CopilotUi.getPlugin().getChatServiceManager().getChatFontService();
    this.currentConversationId = currentConversationId;
    this.backImage = UiUtils.buildImageFromPngPath(
        UiUtils.isDarkTheme() ? "/icons/chat/back_arrow_grey.png" : "/icons/chat/back_arrow.png");
    this.enterImage = UiUtils.buildImageFromPngPath("/icons/chat/enter.png");
    this.editImage = UiUtils.buildImageFromPngPath("/icons/chat/chat_history_edit.png");

    // Assign CSS id for styling
    this.setData(CssConstants.CSS_ID_KEY, "chat-history-viewer");

    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    setLayout(layout);

    createChatHistoryComposite(conversations, currentConversationId);
    this.addDisposeListener(e -> {
      if (backImage != null && !backImage.isDisposed()) {
        backImage.dispose();
      }
      if (enterImage != null && !enterImage.isDisposed()) {
        enterImage.dispose();
      }
      if (editImage != null && !editImage.isDisposed()) {
        editImage.dispose();
      }
    });
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
    gl.marginWidth = 0;
    gl.marginHeight = 5;
    Composite backLabelComposite = new Composite(parent, SWT.NONE);
    backLabelComposite.setLayout(gl);
    backLabelComposite.setCursor(handCursor);

    // Use helper method to add back click listener
    addBackClickListener(backLabelComposite);

    // Ensure proper vertical centering
    GridData backLabelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    backLabelComposite.setLayoutData(backLabelData);

    Label icon = new Label(backLabelComposite, SWT.NONE);
    icon.setImage(backImage);
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
    chatFontService.registerControl(label);

    backLabelComposite.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        gc.setForeground(CssConstants.getTopBannerBorderColor(getDisplay()));
        gc.setLineWidth(1);
        gc.drawLine(0, e.height - 1, e.width, e.height - 1);
      }
    });
  }

  private void createChatHistoryList(Composite parent, List<ConversationXmlData> conversations,
      String currentConversationId) {
    boolean hasValidCurrentId = StringUtils.isNotBlank(currentConversationId);
    boolean hasCurrentConversation = hasValidCurrentId && conversations.stream()
        .anyMatch(conversation -> StringUtils.equals(conversation.getConversationId(), currentConversationId));

    // Add "New Chat" item at the top if no current conversation exists
    if (!hasCurrentConversation) {
      createConversationItem(parent, Messages.chat_topBanner_chatHistoryItem_newChat, true,
          Messages.chat_topBanner_chatHistoryItem_newChatTime_Now, null, false);
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
      createConversationItem(parent, title, isCurrentConversation, displayDateStr, conversation, needsUpperBorder);

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
    return dateToFormat != null ? UiUtils.formatRelativeDateTime(dateToFormat) : "";
  }

  /**
   * Create a conversation item in the chat history list, with optional upper border and margins.
   */
  private void createConversationItem(Composite parent, String title, boolean isCurrent, String dateStr,
      ConversationXmlData conversation, boolean needsUpperBorder) {
    // Three columns: [leftStack(title + optional "(Current)")] [date] [actionsComposite]
    if (needsUpperBorder) {
      Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
      separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }
    GridLayout conversationLayout = new GridLayout(3, false);
    conversationLayout.marginWidth = 5;
    conversationLayout.marginHeight = 1;
    Composite conversationItem = new Composite(parent, SWT.NONE);
    conversationItem.setLayout(conversationLayout);
    setConversationItemCssClass(conversationItem, isCurrent, false);

    GridData conversationItemData = new GridData(SWT.FILL, SWT.TOP, true, false);
    conversationItemData.horizontalIndent = 5;

    conversationItem.setLayoutData(conversationItemData);
    conversationItem.setCursor(handCursor);

    // Left stack: [title][(Current) (actions)]
    GridLayout leftLayout = new GridLayout(2, false);
    leftLayout.marginHeight = 0;
    leftLayout.marginWidth = 0;
    Composite leftStack = new Composite(conversationItem, SWT.NONE);
    leftStack.setLayout(leftLayout);
    leftStack.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    GridLayout titleLayout = new GridLayout(2, false);
    titleLayout.marginHeight = 0;
    titleLayout.marginWidth = 0;
    Composite titleComposite = new Composite(leftStack, SWT.NONE);
    titleComposite.setLayout(titleLayout);
    titleComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    // Title label (initially visible)
    CLabel titleLabel = new CLabel(titleComposite, SWT.NONE);
    titleLabel.setText(title);
    GridData titleLabelData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    titleLabel.setLayoutData(titleLabelData);
    chatFontService.registerControl(titleLabel);

    // Optional "(Current)" label
    Label currentLabel = null;
    if (isCurrent) {
      currentLabel = new Label(titleComposite, SWT.NONE);
      currentLabel.setText(Messages.chat_topBanner_chatHistoryItem_currentConversation_label);
      currentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
      applyCssClass(currentLabel, "chat-history-item-current-label");
      chatFontService.registerControl(currentLabel);
    }

    // Title text editor (initially hidden)
    Text titleEditor = new Text(leftStack, SWT.SINGLE | SWT.BORDER);
    titleEditor.setText(title);
    GridData editorData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    editorData.exclude = true;
    titleEditor.setLayoutData(editorData);
    titleEditor.setVisible(false);
    chatFontService.registerControl(titleEditor);

    // Enter icon (initially hidden, shown only when editor is visible)
    Label enterIcon = new Label(leftStack, SWT.NONE);
    enterIcon.setImage(enterImage);
    enterIcon.setToolTipText(Messages.chat_historyView_enterIcon_tooltip);
    GridData enterIconData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
    enterIconData.exclude = true;
    enterIcon.setLayoutData(enterIconData);
    enterIcon.setVisible(false);

    // Date label (right-aligned)
    Label dateLabel = new Label(conversationItem, SWT.NONE);
    dateLabel.setText(dateStr);
    GridData dateData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    dateLabel.setLayoutData(dateData);
    applyCssClass(dateLabel, "chat-history-item-date-label");
    chatFontService.registerControl(dateLabel);

    // Actions composite (initially hidden, shown on hover)
    Composite actionsComposite = createActionsComposite(conversationItem, conversation, titleLabel, titleEditor,
        titleComposite, enterIcon);

    Listener sharedClickListener = createConversationClickListener(conversation);
    HoverListeners hoverListeners = createConversationHoverListeners(conversationItem, isCurrent, actionsComposite,
        leftStack, conversation);

    // Apply listeners to all relevant controls at once
    Control[] clickableControls = { conversationItem, leftStack, titleComposite, titleLabel, currentLabel, dateLabel };
    for (Control control : clickableControls) {
      if (control != null) {
        control.addListener(SWT.MouseDown, sharedClickListener);
        control.addListener(SWT.MouseEnter, hoverListeners.enterListener);
        control.addListener(SWT.MouseExit, hoverListeners.exitListener);
      }
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
      if (!ConversationUtils.confirmEndChat()) {
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
        setActionsAreaVisible(conversationItem, actionsComposite, true);
      }
    };

    Listener exitListener = event -> {
      setConversationItemCssClass(conversationItem, isCurrent, false);
      if (conversation != null) {
        setActionsAreaVisible(conversationItem, actionsComposite, false);
      }
    };

    return new HoverListeners(enterListener, exitListener);
  }

  /**
   * Sets the visibility of the actions area.
   *
   * @param conversationItem the parent conversation item for layout
   * @param actionsComposite the actions composite to show/hide
   * @param visible true to show the actions area, false to hide it
   */
  private void setActionsAreaVisible(Composite conversationItem, Composite actionsComposite, boolean visible) {
    if (actionsComposite.isVisible() != visible) {
      GridData layoutData = (GridData) actionsComposite.getLayoutData();
      layoutData.exclude = !visible;
      actionsComposite.setVisible(visible);
      conversationItem.getDisplay().asyncExec(() -> {
        if (!conversationItem.isDisposed()) {
          conversationItem.requestLayout();
        }
      });
    }
  }

  // Extract edit mode check to reduce code duplication
  private boolean isInEditMode(Composite titleComposite) {
    for (Control child : titleComposite.getChildren()) {
      if (child.isDisposed()) {
        continue;
      }
      if (child instanceof Text titleEditor) {
        return titleEditor.getVisible();
      }
    }
    return false;
  }

  /**
   * Create the actions composite with edit and delete icons for a conversation item.
   */
  private Composite createActionsComposite(Composite parent, ConversationXmlData conversation, CLabel titleLabel,
      Text titleEditor, Composite titleComposite, Label enterIcon) {
    GridLayout actionsLayout = new GridLayout(2, false);
    actionsLayout.marginHeight = 0;
    actionsLayout.marginWidth = 0;
    Composite actionsComposite = new Composite(parent, SWT.NONE);
    actionsComposite.setLayout(actionsLayout);

    GridData actionsData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    actionsData.exclude = true;
    actionsComposite.setLayoutData(actionsData);
    actionsComposite.setVisible(false);

    // Edit icon
    Label editIcon = new Label(actionsComposite, SWT.NONE);
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
      deleteImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE);
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
      switchTitleLabelModes(titleComposite, titleEditor, actionsComposite, true);
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
      applyTitleChanges(titleEditor, titleLabel, titleComposite, actionsComposite, conversation);
    });

    // Title editor listeners for handling edit completion
    setupTitleEditor(titleEditor, titleLabel, titleComposite, actionsComposite, conversation);

    return actionsComposite;
  }

  /**
   * Common method to apply title changes from the editor to the label and persistence. Used by both Enter key handler
   * and enter icon click handler.
   */
  private void applyTitleChanges(Text titleEditor, CLabel titleLabel, Composite titleComposite,
      Composite actionsComposite, ConversationXmlData conversation) {
    String newTitle = titleEditor.getText().trim();
    if (!newTitle.isEmpty() && conversation != null) {
      // Update title in persistence
      try {
        ConversationPersistenceManager persistenceManager = CopilotUi.getPlugin().getChatServiceManager()
            .getPersistenceManager();
        persistenceManager.updateConversationTitle(conversation.getConversationId(), newTitle);

        // Update the label text and stored original title
        titleLabel.setText(newTitle);
        titleLabel.requestLayout();

        // Update the conversation object for consistency
        conversation.setTitle(newTitle);

        // Send event to chat view if this is the current conversation
        if (eventBroker != null && StringUtils.equals(conversation.getConversationId(), currentConversationId)) {
          eventBroker.post(CopilotEventConstants.TOPIC_CHAT_CONVERSATION_TITLE_UPDATED, newTitle);
        }

        // Switch back to view mode
        switchTitleLabelModes(titleComposite, titleEditor, actionsComposite, false);

      } catch (Exception e) {
        // On error, revert to original title
        titleEditor.setText(titleLabel.getText());
        switchTitleLabelModes(titleComposite, titleEditor, actionsComposite, false);
      }
    } else {
      // Invalid title, revert
      titleEditor.setText(titleLabel.getText());
      switchTitleLabelModes(titleComposite, titleEditor, actionsComposite, false);
    }
  }

  /**
   * Switch from title label to title editor mode.
   */
  private void switchTitleLabelModes(Composite titleComposite, Text titleEditor, Composite actionsComposite,
      boolean isEditMode) {
    GridData labelData = (GridData) titleComposite.getLayoutData();
    labelData.exclude = isEditMode;
    titleComposite.setVisible(!isEditMode);

    GridData editorData = (GridData) titleEditor.getLayoutData();
    editorData.exclude = !isEditMode;
    titleEditor.setVisible(isEditMode);

    // Show enter icon when editor is visible. Enter icon is the last child
    Composite leftStack = titleComposite.getParent();
    if (leftStack != null && !leftStack.isDisposed()) {
      Label enterIcon = (Label) leftStack.getChildren()[leftStack.getChildren().length - 1];
      GridData enterIconData = (GridData) enterIcon.getLayoutData();
      enterIconData.exclude = !isEditMode;
      enterIcon.setVisible(isEditMode);

      // Change leftStack layout to grab horizontal space when editor is visible
      GridData leftStackData = (GridData) leftStack.getLayoutData();
      leftStackData.horizontalAlignment = isEditMode ? SWT.FILL : SWT.LEFT;
    }

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
  private void setupTitleEditor(Text titleEditor, CLabel titleLabel, Composite titleComposite,
      Composite actionsComposite, ConversationXmlData conversation) {
    // Handle Enter key to save changes
    titleEditor.addListener(SWT.KeyDown, event -> {
      if (event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR) {
        // Apply title changes using the common method
        applyTitleChanges(titleEditor, titleLabel, titleComposite, actionsComposite, conversation);
      } else if (event.keyCode == SWT.ESC) {
        // Cancel editing on Escape
        titleEditor.setText(titleLabel.getText());
        switchTitleLabelModes(titleComposite, titleEditor, actionsComposite, false);
      }
    });

    // Handle focus lost to save changes
    titleEditor.addListener(SWT.FocusOut, event -> {
      titleEditor.setText(titleLabel.getText());
      switchTitleLabelModes(titleComposite, titleEditor, actionsComposite, false);
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

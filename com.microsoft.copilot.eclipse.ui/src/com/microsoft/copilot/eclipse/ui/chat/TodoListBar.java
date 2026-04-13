package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.lsp.protocol.TodoItem;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.TodoListService;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * TodoListBar displays a collapsible list of todo items in the chat view.
 */
public class TodoListBar extends Composite {
  /** Minimum number of todos required to show the widget. */
  public static final int MIN_TODOS_TO_SHOW = 3;

  /** Maximum number of todo items to display. */
  public static final int MAX_VISIBLE_ITEMS = 4;

  private TodoListService todoListService;
  private ChatFontService chatFontService;
  private boolean isExpanded = false;
  private boolean userManuallyExpanded = false;
  private List<TodoItem> currentTodos = List.of();

  private TodoListTitleBar titleBar;
  private TodoListContent todoListContent;

  // Shared status images
  private Image completedImage;
  private Image inProgressImage;
  private Image notStartedImage;

  /** Constructor. */
  public TodoListBar(Composite parent, int style) {
    super(parent, style | SWT.BORDER);
    this.todoListService = CopilotUi.getPlugin().getChatServiceManager().getTodoListService();
    this.chatFontService = CopilotUi.getPlugin().getChatServiceManager().getChatFontService();
    loadStatusImages();
    this.addDisposeListener(e -> disposeStatusImages());
  }

  private void disposeStatusImages() {
    if (completedImage != null && !completedImage.isDisposed()) {
      completedImage.dispose();
    }
    if (inProgressImage != null && !inProgressImage.isDisposed()) {
      inProgressImage.dispose();
    }
    if (notStartedImage != null && !notStartedImage.isDisposed()) {
      notStartedImage.dispose();
    }
  }

  private void loadStatusImages() {
    boolean isDarkTheme = UiUtils.isDarkTheme();
    if (isDarkTheme) {
      completedImage = UiUtils.buildImageFromPngPath("/icons/chat/todos_finish_dark.png");
      inProgressImage = UiUtils.buildImageFromPngPath("/icons/chat/todos_running_dark.png");
      notStartedImage = UiUtils.buildImageFromPngPath("/icons/chat/todos_waiting_dark.png");
    } else {
      completedImage = UiUtils.buildImageFromPngPath("/icons/chat/todos_finish.png");
      inProgressImage = UiUtils.buildImageFromPngPath("/icons/chat/todos_running.png");
      notStartedImage = UiUtils.buildImageFromPngPath("/icons/chat/todos_waiting.png");
    }
  }

  Image getStatusImage(String status) {
    if (TodoItem.Status.COMPLETED.getValue().equals(status)) {
      return completedImage;
    } else if (TodoItem.Status.IN_PROGRESS.getValue().equals(status)) {
      return inProgressImage;
    } else {
      return notStartedImage;
    }
  }

  /**
   * Builds the todo list bar with the given todos.
   */
  public void buildTodoListBar(List<TodoItem> todos) {
    if (todos == null || isDisposed()) {
      return;
    }

    this.currentTodos = todos;

    // Check if all todos are incomplete (not-started) - if so, reset manual expansion
    boolean allIncomplete = todos.stream().allMatch(TodoItem::isNotStarted);
    if (allIncomplete) {
      userManuallyExpanded = false;
    }

    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.verticalSpacing = 0;
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    // Dispose and recreate title bar
    if (titleBar != null && !titleBar.isDisposed()) {
      titleBar.dispose();
    }
    titleBar = new TodoListTitleBar(this, SWT.NONE);
    titleBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    // Dispose and recreate content
    if (todoListContent != null && !todoListContent.isDisposed()) {
      todoListContent.dispose();
    }
    todoListContent = new TodoListContent(this, SWT.NONE);

    // Auto-collapse if there are in-progress or completed tasks AND user hasn't manually expanded
    boolean hasInProgressTask = todos.stream().anyMatch(TodoItem::isInProgress);
    boolean hasCompletedTask = todos.stream().anyMatch(TodoItem::isCompleted);
    if ((hasInProgressTask || hasCompletedTask) && isExpanded && !userManuallyExpanded) {
      isExpanded = false;
      updateContentVisibility();
    }

    updateClearButtonState();
    requestLayout();
  }

  private void toggleExpanded() {
    isExpanded = !isExpanded;
    userManuallyExpanded = true;
    updateContentVisibility();
    if (titleBar != null && !titleBar.isDisposed()) {
      titleBar.updateDisplay();
    }
    requestLayout();
  }

  private void updateContentVisibility() {
    if (todoListContent != null && !todoListContent.isDisposed()) {
      GridData layoutData = (GridData) todoListContent.getLayoutData();
      layoutData.exclude = !isExpanded;
      todoListContent.setVisible(isExpanded);
    }
  }

  /**
   * Update the state of the Clear button based on the current todos and request status.
   */
  public void updateClearButtonState() {
    if (titleBar != null && !titleBar.isDisposed()) {
      titleBar.updateClearButtonState();
    }
  }

  @Override
  public void dispose() {
    if (isDisposed()) {
      return;
    }

    if (titleBar != null && !titleBar.isDisposed()) {
      titleBar.dispose();
      titleBar = null;
    }
    if (todoListContent != null && !todoListContent.isDisposed()) {
      todoListContent.dispose();
      todoListContent = null;
    }
    super.dispose();
  }

  class TodoListTitleBar extends Composite {
    private Label expandIcon;
    private Image downArrowImage;
    private Image rightArrowImage;
    private Label statusIcon;
    private CLabel titleLabel;
    private Button clearButton;
    private Image clearEnabledImage;
    private Image clearDisabledImage;

    public TodoListTitleBar(Composite parent, int style) {
      super(parent, style);
      GridLayout layout = new GridLayout(4, false);
      layout.marginWidth = 4;
      layout.marginHeight = 4;
      layout.horizontalSpacing = 4;
      layout.verticalSpacing = 0;
      setLayout(layout);
      setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

      createExpandIcon();
      createStatusIcon();
      createTitleLabel();
      createClearButton();

      // Add click listeners to toggle expansion
      MouseAdapter clickListener = new MouseAdapter() {
        @Override
        public void mouseUp(MouseEvent e) {
          toggleExpanded();
        }
      };
      expandIcon.addMouseListener(clickListener);
      statusIcon.addMouseListener(clickListener);
      titleLabel.addMouseListener(clickListener);
      addMouseListener(clickListener);

      updateDisplay();

      addDisposeListener(e -> disposeImages());
    }

    private void createExpandIcon() {
      expandIcon = new Label(this, SWT.NONE);
      expandIcon.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      expandIcon.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    }

    private void createStatusIcon() {
      statusIcon = new Label(this, SWT.NONE);
      statusIcon.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      statusIcon.setVisible(!isExpanded);
      statusIcon.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    }

    private void createTitleLabel() {
      titleLabel = new CLabel(this, SWT.NONE);
      titleLabel.setMargins(0, 0, 0, 0);
      titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      titleLabel.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
      titleLabel.setData(CssConstants.CSS_ID_KEY, "todo-list-title");
      chatFontService.registerControl(titleLabel);
    }

    private void createClearButton() {
      clearButton = UiUtils.createIconButton(this, SWT.PUSH | SWT.FLAT);
      loadClearButtonImages();
      clearButton.setImage(clearEnabledImage);
      clearButton.setToolTipText(Messages.todoList_clearButton);
      clearButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

      clearButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          // Check if button should be disabled (visual-only disable to preserve tooltip)
          if (todoListService.shouldDisableClearButton()) {
            return;
          }
          ChatView chatView = todoListService.getChatView();
          String conversationId = chatView != null ? chatView.getConversationId() : null;
          todoListService.clearTodos(conversationId);
          // Move focus back to input text viewer to remove focus border from button
          if (chatView != null) {
            chatView.setFocus();
          }
        }
      });
    }

    private void loadClearButtonImages() {
      clearEnabledImage = UiUtils.buildImageFromPngPath("/icons/chat/clear_todo.png");
      clearDisabledImage = UiUtils.buildImageFromPngPath("/icons/chat/clear_todo_disable.png");
    }

    public void updateDisplay() {
      updateExpandIcon();
      updateStatusIcon();
      updateTitleText();
      requestLayout();
    }

    private void updateStatusIcon() {
      if (statusIcon == null || statusIcon.isDisposed()) {
        return;
      }

      // Show status icon only when collapsed
      GridData statusData = (GridData) statusIcon.getLayoutData();
      statusData.exclude = isExpanded;
      statusIcon.setVisible(!isExpanded);

      if (!isExpanded && currentTodos != null && !currentTodos.isEmpty()) {
        TodoItem inProgress = currentTodos.stream().filter(TodoItem::isInProgress).findFirst().orElse(null);
        TodoItem notStarted = currentTodos.stream().filter(TodoItem::isNotStarted).findFirst().orElse(null);
        TodoItem todoToShow = inProgress != null ? inProgress : notStarted;

        if (todoToShow != null) {
          statusIcon.setImage(getStatusImage(todoToShow.getStatus()));
        } else {
          // All completed
          statusIcon.setImage(completedImage);
        }
      }
    }

    private void updateExpandIcon() {
      if (expandIcon == null || expandIcon.isDisposed()) {
        return;
      }

      if (isExpanded) {
        if (downArrowImage == null) {
          downArrowImage = UiUtils.buildImageFromPngPath("/icons/chat/down_arrow.png");
        }
        expandIcon.setImage(downArrowImage);
        setToolTipText(Messages.todoList_collapseTooltip);
      } else {
        if (rightArrowImage == null) {
          rightArrowImage = UiUtils.buildImageFromPngPath("/icons/chat/right_arrow.png");
        }
        expandIcon.setImage(rightArrowImage);
        setToolTipText(Messages.todoList_expandTooltip);
      }
      expandIcon.setToolTipText(getToolTipText());
      titleLabel.setToolTipText(getToolTipText());
    }

    private void updateTitleText() {
      if (titleLabel == null || titleLabel.isDisposed() || currentTodos == null) {
        return;
      }

      int completedCount = (int) currentTodos.stream().filter(TodoItem::isCompleted).count();
      int totalCount = currentTodos.size();
      boolean hasInProgress = currentTodos.stream().anyMatch(TodoItem::isInProgress);
      int currentTaskNumber = hasInProgress ? completedCount + 1 : Math.max(1, completedCount);

      if (isExpanded) {
        titleLabel.setText(NLS.bind(Messages.todoList_titleWithCount, currentTaskNumber, totalCount));
      } else {
        TodoItem inProgress = currentTodos.stream().filter(TodoItem::isInProgress).findFirst().orElse(null);
        TodoItem notStarted = currentTodos.stream()
            .filter(TodoItem::isNotStarted).findFirst().orElse(null);
        TodoItem todoToShow = inProgress != null ? inProgress : notStarted;

        if (todoToShow != null) {
          String title = todoToShow.getTitle();
          titleLabel.setText(title + " (" + currentTaskNumber + "/" + totalCount + ")");
        } else if (completedCount == totalCount && totalCount > 0) {
          titleLabel.setText(NLS.bind(Messages.todoList_titleWithCount, totalCount, totalCount));
        } else {
          titleLabel.setText(NLS.bind(Messages.todoList_titleWithCount, currentTaskNumber, totalCount));
        }
      }
    }

    public void updateClearButtonState() {
      if (clearButton == null || clearButton.isDisposed()) {
        return;
      }

      // Note: We don't use setEnabled(false) because disabled buttons in SWT
      // don't receive mouse events, causing tooltip to fallback to parent's tooltip.
      // Instead, we use visual-only disable (image + tooltip + cursor) and check state in click handler.
      boolean shouldDisable = todoListService.shouldDisableClearButton();

      if (shouldDisable) {
        if (clearDisabledImage != null) {
          clearButton.setImage(clearDisabledImage);
        }
        clearButton.setToolTipText(Messages.todoList_clearButtonDisabled);
        clearButton.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
      } else {
        if (clearEnabledImage != null) {
          clearButton.setImage(clearEnabledImage);
        }
        clearButton.setToolTipText(Messages.todoList_clearButton);
        clearButton.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
      }
    }

    private void disposeImages() {
      if (downArrowImage != null && !downArrowImage.isDisposed()) {
        downArrowImage.dispose();
        downArrowImage = null;
      }
      if (rightArrowImage != null && !rightArrowImage.isDisposed()) {
        rightArrowImage.dispose();
        rightArrowImage = null;
      }
      if (clearEnabledImage != null && !clearEnabledImage.isDisposed()) {
        clearEnabledImage.dispose();
        clearEnabledImage = null;
      }
      if (clearDisabledImage != null && !clearDisabledImage.isDisposed()) {
        clearDisabledImage.dispose();
        clearDisabledImage = null;
      }
    }
  }

  class TodoListContent extends Composite {
    private ScrolledComposite scrolledComposite;
    private Composite contentArea;
    private List<Composite> todoRows = new ArrayList<>();

    public TodoListContent(Composite parent, int style) {
      super(parent, style);
      GridLayout layout = new GridLayout(1, false);
      layout.marginWidth = 0;
      layout.marginHeight = 0;
      layout.verticalSpacing = 0;
      setLayout(layout);

      GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
      layoutData.exclude = !isExpanded;
      setLayoutData(layoutData);
      setVisible(isExpanded);

      int todoCount = currentTodos != null ? currentTodos.size() : 0;

      if (todoCount > MAX_VISIBLE_ITEMS) {
        scrolledComposite = new ScrolledComposite(this, SWT.V_SCROLL);
        GridLayout scrollLayout = new GridLayout(1, false);
        scrollLayout.marginWidth = 0;
        scrollLayout.marginHeight = 0;
        scrolledComposite.setLayout(scrollLayout);
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);

        contentArea = new Composite(scrolledComposite, SWT.NONE);
        GridLayout contentLayout = new GridLayout(1, false);
        contentLayout.marginWidth = 8;
        contentLayout.marginHeight = 4;
        contentLayout.verticalSpacing = 2;
        contentArea.setLayout(contentLayout);

        scrolledComposite.setContent(contentArea);
      } else {
        scrolledComposite = null;
        contentArea = new Composite(this, SWT.NONE);
        GridLayout contentLayout = new GridLayout(1, false);
        contentLayout.marginWidth = 8;
        contentLayout.marginHeight = 4;
        contentLayout.verticalSpacing = 2;
        contentArea.setLayout(contentLayout);
        contentArea.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      }

      renderTodoItems();

      if (scrolledComposite != null && !todoRows.isEmpty()) {
        Composite firstRow = todoRows.get(0);
        int singleRowHeight = firstRow.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 2;
        int scrollHeight = singleRowHeight * MAX_VISIBLE_ITEMS;
        scrolledComposite.setMinHeight(contentArea.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        GridData scrollData = new GridData(SWT.FILL, SWT.FILL, true, false);
        scrollData.heightHint = scrollHeight;
        scrolledComposite.setLayoutData(scrollData);
      }
    }

    private void renderTodoItems() {
      if (currentTodos == null || currentTodos.isEmpty()) {
        return;
      }

      for (TodoItem todo : currentTodos) {
        Composite row = createTodoItemRow(todo);
        todoRows.add(row);
      }
    }

    private Composite createTodoItemRow(TodoItem todo) {
      GridLayout rowLayout = new GridLayout(2, false);
      rowLayout.marginWidth = 0;
      rowLayout.marginHeight = 1;
      rowLayout.horizontalSpacing = 8;
      Composite row = new Composite(contentArea, SWT.NONE);
      row.setLayout(rowLayout);
      row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

      CLabel statusIcon = new CLabel(row, SWT.NONE);
      Image statusImage = getStatusImage(todo.getStatus());
      if (statusImage != null) {
        statusIcon.setImage(statusImage);
      }
      statusIcon.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

      CLabel itemTitleLabel = new CLabel(row, SWT.NONE);
      itemTitleLabel.setText(todo.getTitle());
      itemTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      chatFontService.registerControl(itemTitleLabel);

      if (todo.getDescription() != null && !todo.getDescription().trim().isEmpty()) {
        itemTitleLabel.setToolTipText(todo.getDescription());
      }

      if (todo.isCompleted()) {
        itemTitleLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        itemTitleLabel.addPaintListener(e -> {
          String text = itemTitleLabel.getText();
          if (text == null || text.isEmpty()) {
            return;
          }
          Point textSize = e.gc.textExtent(text);
          int contentWidth = itemTitleLabel.getClientArea().width;
          int lineLength = Math.min(textSize.x, contentWidth - 4);
          int y = (itemTitleLabel.getClientArea().height - textSize.y) / 2 + textSize.y / 2;
          e.gc.setLineWidth(1);
          e.gc.drawLine(2, y, 2 + lineLength, y);
        });
      }

      return row;
    }
  }
}

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ChatEventsManager;
import com.microsoft.copilot.eclipse.core.chat.ChatProgressListener;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.TodoItem;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.TodoListBar;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Service for managing the Todo List in the chat view. This service manages the state of todo items and their display
 * in the TodoListBar.
 */
public class TodoListService extends ChatBaseService implements ChatProgressListener {
  private IObservableValue<List<TodoItem>> todosObservable;

  private TodoListBar todoListBar;
  private ISideEffect todosSideEffect;
  private ChatView boundChatView;
  private volatile boolean requestInProgress = false;

  /**
   * Constructor.
   */
  public TodoListService(CopilotLanguageServerConnection lsConnection) {
    super(lsConnection, null);

    // Register as ChatProgressListener to track request state
    ChatEventsManager eventsManager = CopilotCore.getPlugin().getChatEventsManager();
    if (eventsManager != null) {
      eventsManager.addChatProgressListener(this);
    }

    ensureRealm(() -> {
      todosObservable = new WritableValue<>(new ArrayList<>(), List.class);
    });
  }

  /**
   * Bind the TodoListBar to the given ChatView.
   */
  public void bindTodoListBar(ChatView chatView) {
    this.boundChatView = chatView;

    ensureRealm(() -> {
      unbindTodoListBar();
      todosSideEffect = ISideEffect.create(() -> todosObservable.getValue(), (List<TodoItem> todoList) -> {
        // Only show widget if there are enough todos
        boolean shouldShow = todoList != null && todoList.size() >= TodoListBar.MIN_TODOS_TO_SHOW;

        if (!shouldShow) {
          disposeTodoListBar();
        } else {
          if (this.todoListBar == null || this.todoListBar.isDisposed()) {
            this.todoListBar = new TodoListBar(chatView.getActionBar(), SWT.NONE);
          }
          // Always position TodoListBar at the very top
          this.todoListBar.moveAbove(null);
          this.todoListBar.buildTodoListBar(todoList);
        }
      });
    });
  }

  /**
   * Unbind the TodoListBar from the ChatView.
   */
  public void unbindTodoListBar() {
    ensureRealm(() -> {
      if (todosSideEffect != null) {
        todosSideEffect.dispose();
        todosSideEffect = null;
      }

      disposeTodoListBar();

      todosObservable.setValue(new ArrayList<>());
    });
  }

  /**
   * Set the list of todo items.
   *
   * @param todoList the list of todo items
   */
  public void setTodoList(List<TodoItem> todoList) {
    ensureRealm(() -> {
      todosObservable.setValue(todoList != null ? new ArrayList<>(todoList) : new ArrayList<>());
    });
  }

  /**
   * Get the current list of todo items.
   */
  public List<TodoItem> getTodoList() {
    List<TodoItem> result = new ArrayList<>();
    ensureRealm(() -> {
      List<TodoItem> todos = todosObservable.getValue();
      if (todos != null) {
        result.addAll(todos);
      }
    });
    return result;
  }

  public TodoListBar getTodoListBar() {
    return todoListBar;
  }

  public ChatView getChatView() {
    return boundChatView;
  }

  /**
   * Clear all todos. This clears the local state and persists the empty list to conversation history.
   *
   * @param conversationId the conversation ID to persist the cleared todos to, can be null
   */
  public void clearTodos(String conversationId) {
    ensureRealm(() -> {
      todosObservable.setValue(new ArrayList<>());
      disposeTodoListBar();
    });

    // Persist the cleared todos to conversation history
    if (conversationId != null && !conversationId.isEmpty()) {
      ChatServiceManager chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
      if (chatServiceManager != null && chatServiceManager.getPersistenceManager() != null) {
        chatServiceManager.getPersistenceManager().updateTodoList(conversationId, new ArrayList<>());
      }
    }
  }

  public boolean isRequestInProgress() {
    return requestInProgress;
  }

  /**
   * Refresh the TodoListBar's clear button state. Call this when the request state changes (e.g., after cancel).
   */
  public void refreshClearButtonState() {
    if (todoListBar != null && !todoListBar.isDisposed()) {
      todoListBar.updateClearButtonState();
    }
  }

  /**
   * Determine if the Clear button should be disabled.
   */
  public boolean shouldDisableClearButton() {
    if (!isRequestInProgress()) {
      return false;
    }
    List<TodoItem> todos = todosObservable.getValue();
    if (todos == null || todos.isEmpty()) {
      return false;
    }
    return todos.stream().anyMatch(TodoItem::isInProgress);
  }

  @Override
  public void onChatProgress(ChatProgressValue progress) {
    if (progress == null) {
      return;
    }

    if (progress.getKind() == WorkDoneProgressKind.begin) {
      requestInProgress = true;
      SwtUtils.invokeOnDisplayThreadAsync(this::refreshClearButtonState);
    } else if (progress.getKind() == WorkDoneProgressKind.end) {
      requestInProgress = false;
      SwtUtils.invokeOnDisplayThreadAsync(this::refreshClearButtonState);
    }
  }

  /**
   * Dispose the TodoListBar if it exists.
   */
  public void disposeTodoListBar() {
    if (todoListBar != null && !todoListBar.isDisposed()) {
      Composite parent = todoListBar.getParent();
      todoListBar.dispose();
      todoListBar = null;
      if (parent != null && !parent.isDisposed()) {
        parent.requestLayout();
      }
    } else {
      todoListBar = null;
    }
  }

  /**
   * Dispose this service and unregister listeners.
   */
  public void dispose() {
    ChatEventsManager eventsManager = CopilotCore.getPlugin().getChatEventsManager();
    if (eventsManager != null) {
      eventsManager.removeChatProgressListener(this);
    }
  }

}

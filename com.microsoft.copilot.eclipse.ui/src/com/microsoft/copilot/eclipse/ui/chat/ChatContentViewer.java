// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentRound;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.TodoItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ToolSpecificData;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.TodoListService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Widget to display chat content.
 */
public class ChatContentViewer extends ScrolledComposite {

  private static final int SCROLL_THRESHOLD = 100;

  private ChatServiceManager serviceManager;

  private Composite cmpContent;

  private Map<String, BaseTurnWidget> turns;
  private Composite errorWidget;

  private BaseTurnWidget latestUserTurn;
  private BaseTurnWidget latestCopilotTurn;
  private BaseTurnWidget latestTurnWidget;
  // Auto-scroll state management
  private boolean autoScrollEnabled;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public ChatContentViewer(Composite parent, int style, ChatServiceManager serviceManager) {
    super(parent, style | SWT.V_SCROLL);
    this.setExpandHorizontal(true);
    this.setExpandVertical(true);
    this.setLayout(new GridLayout(1, true));
    this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    this.setData(CssConstants.CSS_ID_KEY, "chat-content-viewer");

    this.cmpContent = new Composite(this, SWT.NONE);
    GridLayout gl = new GridLayout(1, true);
    gl.marginHeight = 0;
    gl.marginWidth = 0;
    this.cmpContent.setLayout(gl);
    this.cmpContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    this.setContent(this.cmpContent);

    this.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        refreshScrollerLayout();
      }
    });

    // Listen for user scroll events to manage auto-scroll behavior
    ScrollBar verticalBar = this.getVerticalBar();
    if (verticalBar != null) {
      verticalBar.addListener(SWT.Selection, event -> {
        int selection = verticalBar.getSelection();
        int maximum = verticalBar.getMaximum();
        int thumb = verticalBar.getThumb();

        // If scrolled to bottom, keep auto-scroll enabled
        // Otherwise disable it (user is viewing history)
        int threshold = SCROLL_THRESHOLD;
        int maxScrollPosition = maximum - thumb;
        boolean isAtBottom = selection >= (maxScrollPosition - threshold);
        autoScrollEnabled = isAtBottom;
      });
    }

    this.turns = new HashMap<>();

    this.serviceManager = serviceManager;

    this.autoScrollEnabled = true;
  }

  /**
   * Should be called when user sends a message.
   */
  public void startNewTurn(String workDoneToken, String message) {
    BaseTurnWidget turnWidget = getLatestOrCreateNewTurnWidget(workDoneToken, false, true);
    turnWidget.appendMessage(message);
    turnWidget.notifyTurnEnd();

    refreshScrollerLayout();
    scrollToLatestUserTurn();
    // Reset auto-scroll for new conversation turn
    autoScrollEnabled = true;

  }

  /**
   * Create a new turn.
   */
  public BaseTurnWidget getLatestOrCreateNewTurnWidget(String workDoneToken, boolean isCopilot,
      boolean forceCreateNewTurn) {
    AtomicReference<BaseTurnWidget> ref = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      BaseTurnWidget turnWidget;
      boolean reuseLatestTurn = !forceCreateNewTurn && latestTurnWidget != null
          && latestTurnWidget.isCopilot == isCopilot;

      if (reuseLatestTurn) {
        // Reuse existing turn widget if the sender type matches
        turnWidget = latestTurnWidget;
      } else if (isCopilot) {
        // Create new Copilot turn widget
        turnWidget = new CopilotTurnWidget(cmpContent, SWT.NONE, serviceManager, workDoneToken);
        latestCopilotTurn = turnWidget;
        latestTurnWidget = turnWidget;
      } else {
        // Create new User turn widget
        turnWidget = new UserTurnWidget(cmpContent, SWT.NONE, serviceManager, workDoneToken);
        latestUserTurn = turnWidget;
        latestCopilotTurn = null;
        latestTurnWidget = turnWidget;
      }

      turns.put(workDoneToken, turnWidget);
      ref.set(turnWidget);
    }, this);

    return ref.get();

  }

  /**
   * Process turn event.
   */
  public void processTurnEvent(ChatProgressValue value) {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (!turns.containsKey(value.getTurnId())) {
        CopilotCore.LOGGER.error(new IllegalStateException("turnId not found: " + value.getTurnId()));
        return;
      }
      BaseTurnWidget turnWidget = turns.get(value.getTurnId());
      if (turnWidget == null) {
        appendMessageToTheLatestTurn(value.getReply());
      }

      ChatServiceManager chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();

      if (value.getKind() == WorkDoneProgressKind.report) {
        if (value.getAgentRounds() != null && !value.getAgentRounds().isEmpty()) {
          // Handle agent mode responses
          AgentRound agentRound = value.getAgentRounds().get(0);

          if (agentRound.getReply() != null) {
            turnWidget.appendMessage(agentRound.getReply());
          }

          if (agentRound.getToolCalls() != null && !agentRound.getToolCalls().isEmpty()) {
            AgentToolCall toolCall = agentRound.getToolCalls().get(0);
            turnWidget.appendToolCallStatus(toolCall);

            // Extract and process todo list from tool result details
            processTodoListFromToolCall(chatServiceManager, value.getConversationId(), toolCall);
          }
        } else {
          // Handle chat mode responses
          turnWidget.appendMessage(value.getReply());
        }
      } else if (value.getKind() == WorkDoneProgressKind.end) {
        turnWidget.notifyTurnEnd();
      }
      refreshScrollerLayout();

      // Auto-scroll to bottom if enabled
      if (shouldAutoScrollToBottom()) {
        scrollToBottom();
      }

      String errMsg = value.getErrorMessage();
      String reason = value.getErrorReason();
      if (StringUtils.isNotEmpty(reason) && reason.equals("model_not_supported")) {
        // TODO: add enable button for better UX.
        errMsg = Messages.chat_model_unsupported_message;
      }
      if (StringUtils.isNotEmpty(errMsg)) {
        // TODO: remove this error message replacement if statement when the CLS side warn message is aligned.
        if (value.getCode() == 402) {
          CopilotPlan userPlan = this.serviceManager.getAuthStatusManager().getQuotaStatus().getCopilotPlan();
          CopilotModel fallbackModel = this.serviceManager.getModelService().getFallbackModel();
          String fallbackModelName = fallbackModel != null ? fallbackModel.getModelName()
              : Messages.chat_noQuotaView_fallbackModel;

          if (userPlan == CopilotPlan.individual || userPlan == CopilotPlan.individual_pro
              || userPlan == CopilotPlan.individual_max) {
            // Paid individual plan message
            errMsg = String.format(Messages.chat_noQuotaView_proProplusWarnMsg, fallbackModelName);
          } else if (userPlan == CopilotPlan.business || userPlan == CopilotPlan.enterprise) {
            // CE and CB message
            errMsg = String.format(Messages.chat_noQuotaView_cbCeWarnMsg, fallbackModelName);
          }
        }

        renderWarnMessageWithUpgradePlanButton(errMsg, value.getCode());

        if (value.getCode() == 402
            && this.serviceManager.getAuthStatusManager().getQuotaStatus().getCopilotPlan() != CopilotPlan.free) {
          this.serviceManager.getModelService().setFallBackModelAsActiveModel();
          this.serviceManager.getAuthStatusManager().checkQuota();

          String previousInput = this.serviceManager.getUserPreferenceService().getPreviousInput(StringUtils.EMPTY);
          if (StringUtils.isNotEmpty(previousInput)) {
            IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
            Map<String, Object> properties = Map.of("previousInput", previousInput, "needCreateUserTurn", false);
            eventBroker.post(CopilotEventConstants.TOPIC_CHAT_ON_SEND, properties);
          }
        }
      }
    }, this);
  }

  /**
   * Append message to the latest turn.
   */
  public void appendMessageToTheLatestTurn(String message) {
    if (this.latestTurnWidget != null) {
      this.latestTurnWidget.appendMessage(message);
    }
  }

  /**
   * Process todo list from tool call result. Extracts todo list data from the tool-specific data
   * and updates the TodoListService.
   *
   * @param chatServiceManager the chat service manager
   * @param conversationId the conversation ID
   * @param toolCall the agent tool call containing tool-specific data
   */
  private void processTodoListFromToolCall(ChatServiceManager chatServiceManager, String conversationId,
      AgentToolCall toolCall) {
    if (chatServiceManager == null || conversationId == null || toolCall == null) {
      return;
    }

    ToolSpecificData toolSpecificData = toolCall.getToolSpecificData();
    if (toolSpecificData == null || toolSpecificData.getTodoList() == null) {
      return;
    }

    TodoListService todoListService = chatServiceManager.getTodoListService();
    if (todoListService == null) {
      return;
    }

    List<TodoItem> todos = toolSpecificData.getTodoList();
    if (todos != null) {
      todoListService.setTodoList(new ArrayList<>(todos));
    }
  }

  /**
   * Get an existed turn widget by turn ID.
   */
  public BaseTurnWidget getTurnWidget(String turnId) {
    return turns.get(turnId);
  }

  private void renderWarnMessageWithUpgradePlanButton(String errorMessage, int code) {
    latestTurnWidget.createWarnDialog(errorMessage, code);
    refreshScrollerLayout();
    scrollToLatestUserTurn();
  }

  /**
   * Render error message banner on the chat content viewer.
   */
  public void renderErrorMessage(String errorMessage) {
    if (this.errorWidget != null) {
      this.errorWidget.dispose();
    }
    this.errorWidget = new ErrorWidget(cmpContent, SWT.BOTTOM, errorMessage);
    refreshScrollerLayout();
    scrollToLatestUserTurn();
  }

  /**
   * Update the size of scrolled composite when there are content updates.
   */
  public void refreshScrollerLayout() {
    if (this.isDisposed()) {
      return;
    }

    Rectangle clientArea = this.getClientArea();
    Point containerSize = cmpContent.computeSize(clientArea.width, SWT.DEFAULT);

    // Use the default size as a fallback
    if (latestUserTurn == null) {
      this.setMinSize(containerSize);
      return;
    }

    Point userTurnSize = latestUserTurn.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    Point copilotTurnSize = latestCopilotTurn == null ? new Point(0, 0)
        : latestCopilotTurn.computeSize(SWT.DEFAULT, SWT.DEFAULT);

    // Calculate the content height, so that the latest user turn is able to be put at the top of the client area.
    int contentHeight = 0;
    int roundedHeight = userTurnSize.y + copilotTurnSize.y;
    if (roundedHeight < clientArea.height) {
      contentHeight = clientArea.height + containerSize.y - roundedHeight;
    } else {
      contentHeight = containerSize.y;
    }

    this.setMinHeight(contentHeight);
    this.setMinWidth(containerSize.x);
    this.layout(true, true);
  }

  /**
   * Check if auto-scroll to bottom is needed. Only scroll when auto-scroll is enabled (user hasn't manually scrolled
   * during response).
   */
  private boolean shouldAutoScrollToBottom() {
    if (this.isDisposed() || latestUserTurn == null) {
      return false;
    }

    if (!autoScrollEnabled) {
      return false;
    }

    Rectangle clientArea = this.getClientArea();
    Point userTurnSize = latestUserTurn.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    Point copilotTurnSize = latestCopilotTurn == null ? new Point(0, 0)
        : latestCopilotTurn.computeSize(SWT.DEFAULT, SWT.DEFAULT);

    int roundedHeight = userTurnSize.y + copilotTurnSize.y;

    // Only auto-scroll when content height exceeds the visible area
    return roundedHeight >= clientArea.height;
  }

  /**
   * Scroll to the bottom.
   */
  private void scrollToBottom() {
    ScrollBar verticalBar = this.getVerticalBar();
    if (verticalBar != null) {
      this.setOrigin(0, verticalBar.getMaximum());
    }
  }

  /**
   * Scroll to the latest user turn. It will be put at the top of the client area.
   */
  private void scrollToLatestUserTurn() {
    // Scroll to the bottom as a fallback.
    if (latestUserTurn == null) {
      scrollToBottom();
      return;
    }

    // Use async execution to ensure layout is computed before reading positions.
    // Using sync execution would read positions before the layout is complete,
    // resulting in incorrect scroll position (always scrolling to 0).
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (this.isDisposed() || latestUserTurn.isDisposed()) {
        return;
      }
      Point turnLocation = latestUserTurn.getLocation();
      this.setOrigin(0, turnLocation.y);
    }, this);
  }

  @Override
  public void dispose() {
    super.dispose();
    for (BaseTurnWidget turn : turns.values()) {
      turn.dispose();
    }
    turns.clear();
    if (this.errorWidget != null) {
      this.errorWidget.dispose();
    }
  }
}

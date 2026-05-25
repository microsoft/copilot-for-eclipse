// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

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
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.TodoListService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.MenuUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Widget to display chat content.
 */
public class ChatContentViewer extends ScrolledComposite {

  private static final int SCROLL_THRESHOLD = 100;

  /**
   * Matches the trailing "| Request ID: ..." and "GitHub Request ID: ..." segments that the
   * language server appends to user-facing error messages.
   */
  private static final Pattern REQUEST_ID_SUFFIX =
      Pattern.compile("\\s*\\|?\\s*(?:GitHub\\s+)?Request\\s+ID:\\s*\\S+\\.?", Pattern.CASE_INSENSITIVE);

  private ChatServiceManager serviceManager;
  private String conversationId;

  private Composite cmpContent;

  private Map<String, BaseTurnWidget> turns;
  private Map<String, String> activeThinkingBlockIds;
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
    this.activeThinkingBlockIds = new ConcurrentHashMap<>();

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
      activeThinkingBlockIds.remove(workDoneToken);
      ref.set(turnWidget);
    }, this);

    return ref.get();

  }

  /** Set the conversation ID used for thinking-block persistence. */
  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
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
        if (turnWidget instanceof ThinkingTurnWidget thinkingTurn) {
          thinkingTurn.setConversationContext(conversationId, value.getTurnId());
          thinkingTurn.appendThinking(value.getThinking());
          updateActiveThinkingBlockId(value.getTurnId(), thinkingTurn);
          if (hasRenderableOutput(value)) {
            // Seal before appending the reply so the spinner stops and the title is fetched.
            thinkingTurn.sealThinking();
          }
        }

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
        // Seal any in-progress thinking block before the turn ends.
        if (turnWidget instanceof ThinkingTurnWidget thinkingTurn) {
          thinkingTurn.sealThinking();
          updateActiveThinkingBlockId(value.getTurnId(), thinkingTurn);
        }
        turnWidget.notifyTurnEnd();
      }
      refreshScrollerLayout();

      // Auto-scroll to bottom if enabled
      if (shouldAutoScrollToBottom()) {
        scrollToBottom();
      }

      String errMsg = value.getErrorMessage();
      if (StringUtils.isNotEmpty(errMsg)) {
        errMsg = REQUEST_ID_SUFFIX.matcher(errMsg).replaceAll(StringUtils.EMPTY).trim();
      }
      String reason = value.getErrorReason();
      if (StringUtils.isNotEmpty(reason) && reason.equals("model_not_supported")) {
        // TODO: add enable button for better UX.
        errMsg = Messages.chat_model_unsupported_message;
      }
      if (StringUtils.isNotEmpty(errMsg)) {
        // TODO: Remove this legacy fallback after TBB is officially released.
        // When the language server has not enabled token-based billing yet, fall back to the
        // original main-branch 402 behavior: replace the message with a plan-driven fallback
        // notice, switch to the fallback model, refresh quota, and replay the previous input.
        CheckQuotaResult quotaStatus = this.serviceManager.getAuthStatusManager().getQuotaStatus();
        CopilotModel fallbackModel = null;
        if (!quotaStatus.tokenBasedBillingEnabled() && value.getCode() == 402) {
          CopilotPlan userPlan = quotaStatus.copilotPlan();
          fallbackModel = this.serviceManager.getModelService().getFallbackModel();
          String fallbackModelName = fallbackModel != null ? fallbackModel.getModelName()
              : Messages.chat_noQuotaView_fallbackModel;

          if (MenuUtils.isCfiPlan(userPlan)) {
            // Pro, Pro+ and Max message
            errMsg = String.format(Messages.chat_noQuotaView_proProplusWarnMsg, fallbackModelName);
          } else if (userPlan == CopilotPlan.business || userPlan == CopilotPlan.enterprise) {
            // CE and CB message
            errMsg = String.format(Messages.chat_noQuotaView_cbCeWarnMsg, fallbackModelName);
          }
        }

        renderWarnMessageWithUpgradePlanButton(errMsg, value.getCode(), value.getErrorModelProviderName());

        // TODO: Remove this legacy fallback after TBB is officially released.
        // Only replay the previous input when a fallback model is actually available; otherwise
        // setFallBackModelAsActiveModel() is a no-op and re-posting the input with the same
        // active model would just trigger the same 402 again.
        if (!quotaStatus.tokenBasedBillingEnabled() && value.getCode() == 402
            && quotaStatus.copilotPlan() != CopilotPlan.free
            && fallbackModel != null) {
          // Detach the failed turn so the replayed response creates a new Copilot turn below the
          // warning, instead of streaming into the same turn that just rendered the warn widget.
          this.latestTurnWidget = null;
          this.latestCopilotTurn = null;

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

  /** Returns the active thinking block ID last observed while processing this turn's progress. */
  public String getActiveThinkingBlockId(String turnId) {
    return activeThinkingBlockIds.get(turnId);
  }

  private void updateActiveThinkingBlockId(String turnId, ThinkingTurnWidget thinkingTurn) {
    String thinkingBlockId = thinkingTurn.getActiveThinkingBlockId();
    if (StringUtils.isBlank(thinkingBlockId)) {
      activeThinkingBlockIds.remove(turnId);
    } else {
      activeThinkingBlockIds.put(turnId, thinkingBlockId);
    }
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
   * Whether {@code value} carries reply text or an agent round with rendered content; thinking-only reports return
   * {@code false} so the banner keeps streaming.
   */
  private static boolean hasRenderableOutput(ChatProgressValue value) {
    return StringUtils.isNotBlank(value.getReply()) || hasRenderableAgentRound(value);
  }

  private static boolean hasRenderableAgentRound(ChatProgressValue value) {
    if (value.getAgentRounds() == null || value.getAgentRounds().isEmpty()) {
      return false;
    }
    for (AgentRound round : value.getAgentRounds()) {
      if (StringUtils.isNotBlank(round.getReply())) {
        return true;
      }
      if (round.getToolCalls() != null && !round.getToolCalls().isEmpty()) {
        return true;
      }
    }
    return false;
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

  private void renderWarnMessageWithUpgradePlanButton(String errorMessage, int code, String modelProviderName) {
    latestTurnWidget.createWarnDialog(errorMessage, code, modelProviderName);
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
   * Schedules a single async {@link #refreshScrollerLayout()} call so that multiple dispose/layout
   * events that arrive in the same event-loop tick are coalesced into one pass.
   */
  public void requestRefreshScrollerLayout() {
    SwtUtils.invokeOnDisplayThreadAsync(() -> refreshScrollerLayout(), this);
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

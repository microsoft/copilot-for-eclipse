// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
 *
 * <p>A self-managed, windowed vertical scroller (not a {@link org.eclipse.swt.custom.ScrolledComposite}):
 * on Windows a classic scroller cannot move very tall content far enough to reveal the newest messages.
 * Here {@code cmpContent} is pinned to the viewport and each turn is positioned in
 * viewport-local coordinates; off-window turns are parked with {@code setVisible(false)}, so no child is
 * ever given an out-of-range native coordinate.</p>
 */
public class ChatContentViewer extends Composite {

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
  private CopilotTurnWidget latestCopilotTurn;
  private BaseTurnWidget latestTurnWidget;
  private boolean autoScrollEnabled;

  /** Streaming events queued by LSP threads and drained on the UI thread in batches. */
  private final Queue<ChatProgressValue> pendingEvents = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean drainScheduled = new AtomicBoolean(false);

  /** Client-area width of the last layout pass; a change forces a full re-measure (text re-wraps). */
  private int lastLayoutWidth = -1;

  /** Guards against scheduling more than one pending async refresh from a burst of resize events. */
  private final AtomicBoolean refreshScheduled = new AtomicBoolean(false);

  /** Current logical scroll position (top of the viewport in content coordinates). */
  private int scrollOffset;

  /** Full logical content height in pixels; may far exceed any native coordinate limit. */
  private int totalHeight;

  /** Cached measured heights keyed by row control identity; invalidated on width change. */
  private final Map<Control, Integer> heightCache = new IdentityHashMap<>();

  /** Cached font line height (px), the unit for one scroll line; recomputed when the font changes. */
  private int cachedLineHeight = -1;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public ChatContentViewer(Composite parent, int style, ChatServiceManager serviceManager) {
    super(parent, style | SWT.V_SCROLL | SWT.DOUBLE_BUFFERED);
    // Null layout: children are positioned manually by relayoutWindow() so SWT never stacks them into
    // one oversized composite.
    this.setLayout(null);
    this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    this.setData(CssConstants.CSS_ID_KEY, "chat-content-viewer");

    this.cmpContent = new Composite(this, SWT.NONE);
    this.cmpContent.setLayout(null);

    this.addListener(SWT.Resize, e -> {
      layoutContentArea();
      coalesceAsync(refreshScheduled, this::refreshLayoutIncremental);
    });

    ScrollBar verticalBar = this.getVerticalBar();
    if (verticalBar != null) {
      verticalBar.addListener(SWT.Selection, event -> {
        scrollOffset = verticalBar.getSelection();
        autoScrollEnabled = isViewportAtBottom();
        relayoutWindow();
      });
    }

    // The wheel does not move the scrollbar on a manually-laid-out composite, so handle it explicitly.
    this.addListener(SWT.MouseWheel, event -> {
      if (totalHeight <= getClientArea().height) {
        return;
      }
      // event.count is the OS-provided number of lines to scroll for this notch; a font line height
      // turns it into pixels, so one notch moves whole text lines like a native scrollable.
      int newOffset = clampOffset(scrollOffset - event.count * lineHeight());
      if (newOffset != scrollOffset) {
        scrollOffset = newOffset;
        autoScrollEnabled = isViewportAtBottom();
        relayoutWindow();
      }
      event.doit = false;
    });

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
    turnWidget.flushMessageBuffer();

    refreshLayoutFull();
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
        CopilotTurnWidget copilotTurnWidget = new CopilotTurnWidget(cmpContent, SWT.NONE, serviceManager,
            workDoneToken);
        latestCopilotTurn = copilotTurnWidget;
        latestTurnWidget = copilotTurnWidget;
        turnWidget = copilotTurnWidget;
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
   * Process turn event. Events are queued and drained in batches on the UI thread so the LSP thread
   * is never blocked and multiple in-flight events coalesce into a single layout pass.
   */
  public void processTurnEvent(ChatProgressValue value) {
    pendingEvents.offer(value);
    coalesceAsync(drainScheduled, this::drainPendingEvents);
  }

  private void drainPendingEvents() {
    if (isDisposed()) {
      pendingEvents.clear();
      return;
    }
    ChatProgressValue event;
    while ((event = pendingEvents.poll()) != null) {
      doProcessTurnEvent(event);
    }
    refreshLayoutIncremental();
    scrollToBottomIfAutoScroll();
    // Events may have arrived while draining; schedule a follow-up drain if so.
    if (!pendingEvents.isEmpty()) {
      coalesceAsync(drainScheduled, this::drainPendingEvents);
    }
  }

  private void doProcessTurnEvent(ChatProgressValue value) {
    if (!turns.containsKey(value.getTurnId())) {
      CopilotCore.LOGGER.error(new IllegalStateException("turnId not found: " + value.getTurnId()));
      return;
    }
    BaseTurnWidget turnWidget = turns.get(value.getTurnId());
    if (turnWidget == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("turnWidget not found: " + value.getTurnId()));
      appendMessageToTheLatestTurn(value.getReply());
      return;
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
      turnWidget.flushMessageBuffer();
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
   * Shows the compacting status on the latest Copilot turn after flushing any buffered reply text.
   */
  public void showCompactingStatusOnLatestCopilotTurn() {
    if (latestCopilotTurn == null || latestCopilotTurn.isDisposed()) {
      return;
    }
    // Flush any buffered reply text from the previous round so it is rendered
    // above the compacting spinner; otherwise it would be concatenated with
    // the next round's reply and produce a single garbled line.
    latestCopilotTurn.flushMessageBuffer();
    latestCopilotTurn.showCompactingStatus();
    refreshLayoutFull();
    scrollToBottomIfAutoScroll();
  }

  /**
   * Hides the compacting status on the latest Copilot turn, flushing any buffered reply text
   * first as a guard against buffered content that was not flushed by an end progress event.
   */
  public void hideCompactingStatusOnLatestCopilotTurn() {
    if (latestCopilotTurn == null || latestCopilotTurn.isDisposed()) {
      return;
    }
    // Always flush before hiding; the buffer should be empty at this point, but flush as a guard
    // in case a cancel path did not receive an end progress event to flush it.
    latestCopilotTurn.flushMessageBuffer();
    latestCopilotTurn.hideCompactingStatus();
    refreshLayoutFull();
    scrollToBottomIfAutoScroll();
  }

  /**
   * Get an existed turn widget by turn ID.
   */
  public BaseTurnWidget getTurnWidget(String turnId) {
    return turns.get(turnId);
  }

  private void renderWarnMessageWithUpgradePlanButton(String errorMessage, int code, String modelProviderName) {
    latestTurnWidget.createWarnDialog(errorMessage, code, modelProviderName);
    refreshLayoutFull();
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
    refreshLayoutFull();
    scrollToLatestUserTurn();
  }

  /**
   * Coalesces a burst of calls into a single async pass on the UI thread, clearing {@code scheduled}
   * right before {@code task} runs so work arriving during the task re-schedules a follow-up pass.
   * Breaks synchronous re-entrancy without a re-entrancy guard.
   *
   * @param scheduled the per-task latch guarding against duplicate scheduling
   * @param task the work to run once on the next UI-thread turn
   */
  private void coalesceAsync(AtomicBoolean scheduled, Runnable task) {
    if (scheduled.compareAndSet(false, true)) {
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        scheduled.set(false);
        task.run();
      }, this);
    }
  }

  /**
   * Full re-measure entry point for external callers. Layout only; scrolling is a separate concern
   * handled by callers via {@link #scrollToBottomIfAutoScroll()}.
   */
  public void refreshLayoutFull() {
    refreshLayout(MeasureMode.FULL);
  }

  /**
   * Incremental re-measure of just the trailing (streaming) turns. Layout only; scrolling is handled
   * separately by callers via {@link #scrollToBottomIfAutoScroll()}.
   */
  private void refreshLayoutIncremental() {
    refreshLayout(MeasureMode.INCREMENTAL);
  }

  /**
   * Selects how many turns {@link #refreshLayout(MeasureMode)} re-measures.
   */
  private enum MeasureMode {
    /** Re-measure every turn. */
    FULL,
    /** Only re-measure the trailing (mutating) turns; sealed turns keep cached sizes. */
    INCREMENTAL
  }

  /**
   * Re-measures turns and re-runs the windowing pass. {@link MeasureMode#INCREMENTAL} keeps sealed
   * turns' cached heights; a width change forces a full re-measure because text re-wraps.
   */
  private void refreshLayout(MeasureMode mode) {
    if (this.isDisposed()) {
      return;
    }
    Rectangle clientArea = this.getClientArea();
    int width = clientArea.width;
    boolean fullMeasure = mode == MeasureMode.FULL || width != lastLayoutWidth;
    lastLayoutWidth = width;

    if (fullMeasure) {
      heightCache.clear();
    } else {
      invalidateTrailingTurnHeights();
    }

    layoutContentArea();
    relayoutWindow();
  }

  /**
   * Scrolls to the bottom when auto-scroll is enabled. The bottom padding reserved by {@link
   * #relayoutWindow()} makes this pin the latest user turn to the top while the round is short, then
   * follow the real bottom once it grows past the viewport.
   */
  public void scrollToBottomIfAutoScroll() {
    if (this.isDisposed() || latestUserTurn == null || latestUserTurn.isDisposed()) {
      return;
    }
    if (!autoScrollEnabled) {
      return;
    }
    scrollOffset = Integer.MAX_VALUE;
    relayoutWindow();
  }

  /**
   * Drops the cached heights of the trailing (mutating) turns so they are re-measured next pass, while
   * sealed historical turns keep their cached size.
   */
  private void invalidateTrailingTurnHeights() {
    if (latestUserTurn != null && !latestUserTurn.isDisposed()) {
      heightCache.remove(latestUserTurn);
    }
    if (latestCopilotTurn != null && !latestCopilotTurn.isDisposed()) {
      heightCache.remove(latestCopilotTurn);
    }
    if (errorWidget != null && !errorWidget.isDisposed()) {
      heightCache.remove(errorWidget);
    }
  }

  /** Pins {@code cmpContent} to the current viewport rectangle so it is never grown or moved. */
  private void layoutContentArea() {
    if (cmpContent == null || cmpContent.isDisposed()) {
      return;
    }
    Rectangle clientArea = this.getClientArea();
    cmpContent.setBounds(0, 0, Math.max(0, clientArea.width), Math.max(0, clientArea.height));
  }

  /**
   * The core windowing pass: measures every turn (cached), positions the ones intersecting the
   * viewport in viewport-local coordinates, and parks the rest with {@code setVisible(false)} so no
   * native child ever gets an out-of-range coordinate.
   */
  private void relayoutWindow() {
    if (this.isDisposed() || cmpContent == null || cmpContent.isDisposed()) {
      return;
    }
    Rectangle clientArea = this.getClientArea();
    int width = clientArea.width;
    int viewport = clientArea.height;
    if (width <= 0 || viewport <= 0) {
      return;
    }

    Control[] children = cmpContent.getChildren();
    int[] tops = new int[children.length];
    int[] heights = new int[children.length];
    boolean[] remeasured = new boolean[children.length];
    int running = 0;
    int latestUserTop = -1;
    for (int i = 0; i < children.length; i++) {
      if (children[i] == latestUserTurn) {
        latestUserTop = running;
      }
      boolean wasCached = heightCache.containsKey(children[i]);
      int height = measuredHeight(children[i], width);
      tops[i] = running;
      heights[i] = height;
      // A cache miss means the turn was (re)measured this pass: its width changed or its content
      // mutated, so its internal GridLayout must be re-run.
      remeasured[i] = !wasCached;
      running += height;
    }
    int rawHeight = running;

    // Bottom padding (virtual, no widget): when the last round is shorter than the viewport, reserve
    // whitespace below it so the latest user turn can pin to the top instead of floating mid-screen.
    // Without it maxOffset is too small, so the new message cannot reach the top and "scroll to
    // bottom" misaligns with the real maximum, breaking auto-scroll.
    int bottomPadding = 0;
    if (latestUserTop >= 0) {
      int lastRoundHeight = rawHeight - latestUserTop;
      if (lastRoundHeight < viewport) {
        bottomPadding = viewport - lastRoundHeight;
      }
    }
    totalHeight = rawHeight + bottomPadding;
    scrollOffset = clampOffset(scrollOffset);

    for (int i = 0; i < children.length; i++) {
      Control child = children[i];
      if (child.isDisposed()) {
        continue;
      }
      int y = tops[i] - scrollOffset;
      if (y + heights[i] > 0 && y < viewport) {
        child.setBounds(0, y, width, heights[i]);
        if (!child.getVisible()) {
          child.setVisible(true);
        }
        // Run the turn's own layout so its GridLayout children (wrapped text, code blocks, footers)
        // reflow.
        if (remeasured[i] && child instanceof Composite composite) {
          composite.layout();
        }
      } else if (child.getVisible()) {
        child.setVisible(false);
      }
    }

    updateScrollBar(viewport);
  }

  /** Returns the measured height of a row, using the identity cache when available. */
  private int measuredHeight(Control child, int width) {
    if (child == null || child.isDisposed()) {
      return 0;
    }
    Integer cached = heightCache.get(child);
    if (cached != null) {
      return cached;
    }
    int height = child.computeSize(width, SWT.DEFAULT, true).y;
    heightCache.put(child, height);
    return height;
  }

  private void updateScrollBar(int viewport) {
    ScrollBar verticalBar = this.getVerticalBar();
    if (verticalBar == null) {
      return;
    }
    if (totalHeight <= viewport) {
      int safeViewport = Math.max(1, viewport);
      verticalBar.setValues(0, 0, safeViewport, safeViewport, lineHeight(), safeViewport);
      verticalBar.setEnabled(false);
      return;
    }
    verticalBar.setEnabled(true);
    verticalBar.setValues(scrollOffset, 0, totalHeight, viewport, lineHeight(), viewport);
  }

  private int maxOffset() {
    return Math.max(0, totalHeight - getClientArea().height);
  }

  private int clampOffset(int offset) {
    return Math.max(0, Math.min(offset, maxOffset()));
  }

  private boolean isViewportAtBottom() {
    return scrollOffset >= maxOffset() - SCROLL_THRESHOLD;
  }

  /** One scroll "line" in pixels: the current font's line height. Cached until the font changes. */
  private int lineHeight() {
    if (cachedLineHeight < 0) {
      GC gc = new GC(this);
      try {
        gc.setFont(getFont());
        cachedLineHeight = Math.max(1, gc.getFontMetrics().getHeight());
      } finally {
        gc.dispose();
      }
    }
    return cachedLineHeight;
  }

  @Override
  public void setFont(Font font) {
    super.setFont(font);
    cachedLineHeight = -1;
  }

  /**
   * Scroll to the bottom.
   */
  private void scrollToBottom() {
    autoScrollEnabled = true;
    scrollOffset = Integer.MAX_VALUE;
    relayoutWindow();
  }

  /**
   * Scroll to the latest user turn. It will be put at the top of the client area.
   */
  private void scrollToLatestUserTurn() {
    // Scroll to the bottom as a fallback.
    if (latestUserTurn == null || latestUserTurn.isDisposed()) {
      scrollToBottom();
      return;
    }

    // Async so heights are measured before reading positions.
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (this.isDisposed() || latestUserTurn.isDisposed()) {
        return;
      }
      scrollOffset = clampOffset(topOf(latestUserTurn));
      relayoutWindow();
    }, this);
  }

  /** Returns the cumulative top offset (in content coordinates) of the given row control. */
  private int topOf(Control target) {
    int width = this.getClientArea().width;
    int running = 0;
    for (Control child : cmpContent.getChildren()) {
      if (child == target) {
        break;
      }
      running += measuredHeight(child, width);
    }
    return running;
  }

  @Override
  public void dispose() {
    pendingEvents.clear();
    heightCache.clear();
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

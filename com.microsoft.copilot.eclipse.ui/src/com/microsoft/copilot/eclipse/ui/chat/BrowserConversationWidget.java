// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.Bundle;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentRound;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult.ToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Thinking;
import com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent.CodingAgentMessageRequestParams;
import com.microsoft.copilot.eclipse.core.persistence.AbstractTurnData;
import com.microsoft.copilot.eclipse.core.persistence.ConversationDataFactory;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.AgentMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.EditAgentRoundData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ReplyData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData;
import com.microsoft.copilot.eclipse.core.utils.BundleUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.QuotaActions.QuotaAction;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * {@link IConversationWidget} implementation that renders copilot chat turns in an
 * Eclipse-internal web {@link Browser} widget using an HTML/JavaScript frontend.
 *
 * <p>This widget creates, updates, and removes HTML code blocks wrapped in <code>DIV</code>
 * blocks with unique IDs. It delegates HTML code generation to {@link ConversationHtmlBlockFactory}
 * and the commonmark Java library for rendering Markdown code,
 * including GitHub-flavoured Markdown tables.
 * The {@link Browser} receives the HTML code based on an HTML template (chat-view.html)
 * and offers a simple API for adding, replacing, or removing HTML div blocks with a certain ID.
 *
 * <p><em>HTML block ID scheme:</em> turn containers use the server-assigned turn ID.
 * Child blocks within a turn use {@code turnId-N} where N is a sequential counter.
 * The CSS class on each child block identifies its type
 * (thinking-block, tool-call, response, etc.).
 */
public class BrowserConversationWidget
    implements IConversationWidget, BrowserConversationJavaJsBridge.JavaCallbacks {

  private final Browser browser;
  private final ConversationHtmlBlockFactory htmlFactory;
  /**
   * Chat services used for quota resolution (and token-based-billing detection). Injected so the
   * browser renderer resolves quota from the same source as the StyledText renderer instead of the
   * global singleton, which keeps the two renderers consistent and makes quota behavior testable.
   */
  private final ChatServiceManager serviceManager;

  // Turn streaming state (per-turn, keyed by turnId).
  private final Map<String, TurnStreamState> turnStates = new HashMap<>();
  private String currentTurnId;
  private String lastCopilotTurnId;
  private String conversationId;

  // Subagent nesting (single level, mirrors SWT BaseTurnWidget). Set while the parent's
  // run_subagent tool call is running; consumed by the subagent's beginTurn.
  private String activeSubagentContentAreaId;
  // Maps a run_subagent tool call id to its nested content-area id (used on restore).
  private final Map<String, String> subagentContentAreaByToolCallId = new HashMap<>();

  // Tool confirmation state
  private CompletableFuture<LanguageModelToolConfirmationResult> pendingConfirmationFuture;
  private String pendingConfirmationTurnId;
  private List<ConfirmationAction> pendingConfirmationActions;
  private ConfirmationAction lastSelectedAction;

  /** Java&#8596;JavaScript bridge: creates/runs JS and registers Java call-back functions. */
  private final BrowserConversationJavaJsBridge bridge;
  private String copilotAvatarDataUri;

  /** Types of child blocks within a copilot turn. */
  private enum ChildBlockType {
    THINKING, TOOL_CALL, RESPONSE
  }

  /**
   * Per-turn streaming state, keyed by turnId. Keeping the block counter and current-block pointers
   * per turn means a parent turn that resumes after a nested subagent keeps its own counter, so no
   * duplicate DOM ids are produced. {@code contentAreaId} is where this turn's child blocks are
   * inserted: the turn's own content area, or — for a subagent turn — the nested subagent content
   * area.
   */
  private static final class TurnStreamState {
    private final String contentAreaId;
    private int childBlockCounter;
    private ChildBlockType currentBlockType;
    private String currentChildBlockId;
    private String currentToolCallId;
    private String currentThinkingBlockId;
    private StringBuilder currentThinkingText;
    private StringBuilder currentReplyText;
    private boolean streamingIndicatorVisible;

    TurnStreamState(String contentAreaId) {
      this.contentAreaId = contentAreaId;
    }

    void resetTransient() {
      currentBlockType = null;
      currentChildBlockId = null;
      currentToolCallId = null;
      currentThinkingBlockId = null;
      currentThinkingText = null;
      currentReplyText = null;
    }
  }

  /**
   * Creates a new browser-based conversation widget.
   *
   * @param parent the parent composite
   * @param serviceManager the chat services used to resolve avatars, display names, and quota
   *     status; must not be {@code null}
   */
  public BrowserConversationWidget(Composite parent, ChatServiceManager serviceManager) {
    this.serviceManager = Objects.requireNonNull(serviceManager, "serviceManager must not be null");
    browser = new Browser(parent, SWT.NONE);
    browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    htmlFactory = new ConversationHtmlBlockFactory();

    Bundle bundle = CopilotUi.getPlugin().getBundle();

    // Resolve avatars via the shared AvatarService so this renderer matches the StyledText renderer.
    this.copilotAvatarDataUri = serviceManager.getAvatarService().getAvatarForCopilotAsDataUri();

    // Load code block action icons from Eclipse platform bundles
    Bundle uiBundle = org.eclipse.core.runtime.Platform.getBundle("org.eclipse.ui");
    String copyUri = BundleUtils.loadPngAsDataUri(uiBundle, "icons/full/etool16/copy_edit.png");
    Bundle textEditorBundle = org.eclipse.core.runtime.Platform.getBundle(
        UiConstants.WORKBENCH_TEXTEDITOR);
    String insertUri = BundleUtils.loadPngAsDataUri(textEditorBundle, UiConstants.INSERT_ICON);
    htmlFactory.setCodeBlockIcons(copyUri, insertUri);

    bridge = new BrowserConversationJavaJsBridge(browser, this);

    browser.addProgressListener(new ProgressAdapter() {
      @Override
      public void completed(ProgressEvent event) {
        bridge.setDarkTheme(UiUtils.isDarkTheme());
        bridge.notifyPageLoaded();
      }
    });

    try {
      URL fileUrl = bundle.getEntry("resources/html/chat-view.html");
      URL resolvedUrl = FileLocator.toFileURL(fileUrl);
      browser.setUrl(resolvedUrl.toExternalForm());
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to load chat-view.html in browser widget", e);
    }
  }

  @Override
  public Control getControl() {
    return browser;
  }

  @Override
  public boolean isDisposed() {
    return browser.isDisposed();
  }

  @Override
  public void dispose() {
    if (!browser.isDisposed()) {
      browser.dispose();
    }
  }

  @Override
  public void requestLayout() {
    browser.requestLayout();
  }

  @Override
  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  @Override
  public void beginTurn(String turnId, boolean isCopilot, boolean isHistory) {
    // Idempotency guard: the turn container may already have been created, e.g. by
    // ensureCopilotTurnContainer() when a tool confirmation arrived before this begin event.
    // Re-creating it would insert a duplicate container into the DOM.
    if (turnStates.containsKey(turnId)) {
      currentTurnId = turnId;
      if (isCopilot) {
        lastCopilotTurnId = turnId;
      }
      return;
    }
    // Subagent begin: nest inside the parent's active subagent block instead of creating a
    // top-level turn container. Its child blocks are routed to the nested content area.
    if (isCopilot && activeSubagentContentAreaId != null) {
      currentTurnId = turnId;
      turnStates.put(turnId, new TurnStreamState(activeSubagentContentAreaId));
      return;
    }
    currentTurnId = turnId;
    if (isCopilot) {
      lastCopilotTurnId = turnId;
    }
    turnStates.put(turnId,
        new TurnStreamState(ConversationHtmlBlockFactory.contentBlockId(turnId, isCopilot)));
    String turnHtml = htmlFactory.createTurnContainerHtmlBlock(
        turnId, isCopilot,
        isCopilot ? copilotAvatarDataUri : getUserAvatarUri(),
        isCopilot ? getCopilotDisplayName() : getUserDisplayName());
    bridge.insertBlock(ConversationHtmlBlockFactory.CHAT_CONTAINER_ID, turnHtml);
    if (isCopilot && !isHistory) {
      showStreamingIndicator(turnId);
    }
  }

  @Override
  public void processTurnEvent(ChatProgressValue value) {
    String turnId = value.getTurnId();

    switch (value.getKind()) {
      case report -> {
        currentTurnId = turnId;
        ensureTurnState(turnId, value);
        removeStreamingIndicator(turnId);
        processThinking(turnId, value);
        processAgentRounds(turnId, value);
        processReply(turnId, extractReplyChunk(value));
      }
      case end -> {
        removeStreamingIndicator(turnId);
        finalizeTurn(turnId);
      }
      default -> {
        // begin is handled by beginTurn(), called separately by ChatView
      }
    }

    // Handle error messages (can arrive on any event kind, including report and end)
    String errorMessage = ChatErrorMessages.resolveDisplayMessage(value);
    if (StringUtils.isNotEmpty(errorMessage)) {
      processErrorMessage(turnId, errorMessage, value.getCode(), value.getErrorModelProviderName());
    }
  }

  @Override
  public void startNewUserTurn(String turnId, String message) {
    String turnHtml = htmlFactory.createTurnContainerHtmlBlock(
        turnId, false, getUserAvatarUri(), getUserDisplayName());
    bridge.insertBlock(ConversationHtmlBlockFactory.CHAT_CONTAINER_ID, turnHtml);
    String contentId = ConversationHtmlBlockFactory.contentBlockId(turnId, false);
    String blockId = contentId + "-0";
    String blockHtml = htmlFactory.createUserRequestHtmlBlock(blockId, message);
    bridge.insertBlock(contentId, blockHtml);
    // Re-arm auto-scroll for the new turn and jump to the bottom so the user turn is visible and
    // the streamed reply is followed. insertBlock's own scroll respects the auto-scroll flag,
    // which the user may have turned off by scrolling up during the previous turn.
    scrollToBottom();
  }

  @Override
  public void scrollToBottom() {
    bridge.scrollToBottom();
  }

  @Override
  public void refreshScrollerLayout() {
    // Browser handles its own layout; no action needed
  }

  @Override
  public void renderErrorMessage(String content) {
    String errorId = ConversationHtmlBlockFactory.errorBlockId();
    String renderedContent = htmlFactory.renderMarkdown(content);
    String errorHtml = htmlFactory.createErrorTurnHtmlBlock(errorId, renderedContent);
    bridge.insertBlock(ConversationHtmlBlockFactory.CHAT_CONTAINER_ID, errorHtml);
    // Scroll the newly inserted error banner into view so the user notices it, mirroring the
    // SWT renderer's showControl-based scroll for error banners.
    scrollToBottom();
  }

  @Override
  public void showCompactingStatusOnLatestCopilotTurn() {
    String turnId = lastCopilotTurnId != null ? lastCopilotTurnId : currentTurnId;
    if (turnId != null) {
      String blockId = ConversationHtmlBlockFactory.compactingBlockId(turnId);
      String html = htmlFactory.createCompactingStatusHtmlBlock(blockId);
      bridge.insertBlock(ConversationHtmlBlockFactory.contentBlockId(turnId, true), html);
    }
  }

  @Override
  public void hideCompactingStatusOnLatestCopilotTurn() {
    String turnId = lastCopilotTurnId != null ? lastCopilotTurnId : currentTurnId;
    if (turnId != null) {
      bridge.removeBlock(ConversationHtmlBlockFactory.compactingBlockId(turnId));
    }
  }

  @Override
  public String getActiveThinkingBlockId(String turnId) {
    TurnStreamState state = turnId != null ? turnStates.get(turnId) : null;
    return state != null ? state.currentThinkingBlockId : null;
  }

  @Override
  public void restoreTurn(AbstractTurnData turn, ConversationDataFactory dataFactory) {
    if (turn == null) {
      return;
    }
    // Subagent turn: render nested inside the parent turn's subagent block.
    if (turn instanceof CopilotTurnData copilotTurn
        && StringUtils.isNotBlank(copilotTurn.getParentTurnId())) {
      restoreSubagentTurn(copilotTurn);
      return;
    }
    if (turn instanceof UserTurnData userTurn) {
      if (userTurn.getMessage() != null
          && StringUtils.isNotBlank(userTurn.getMessage().getText())) {
        startNewUserTurn(turn.getTurnId(), userTurn.getMessage().getText());
      }
      return;
    }
    if (turn instanceof CopilotTurnData copilotTurn) {
      String turnId = turn.getTurnId();
      lastCopilotTurnId = turnId;
      String turnHtml = htmlFactory.createTurnContainerHtmlBlock(
          turnId, true, copilotAvatarDataUri, getCopilotDisplayName());
      bridge.insertBlock(ConversationHtmlBlockFactory.CHAT_CONTAINER_ID, turnHtml);
      restoreCopilotTurnContent(turnId,
          ConversationHtmlBlockFactory.contentBlockId(turnId, true), copilotTurn);
      ReplyData replyData = copilotTurn.getReply();
      if (replyData != null && StringUtils.isNotBlank(replyData.getModelName())) {
        renderModelInfo(turnId, replyData.getModelName(),
            replyData.getBillingMultiplier(), replyData.getReasoningEffort());
      }
    }
  }

  /**
   * Restores a subagent turn into the nested subagent block that was opened while restoring the
   * parent turn's {@code run_subagent} tool call. Falls back to the parent's content area when the
   * subagent tool-call id is missing (mirrors the SWT blank-toolCallId path).
   */
  private void restoreSubagentTurn(CopilotTurnData copilotTurn) {
    String toolCallId = copilotTurn.getSubagentToolCallId();
    String contentAreaId = StringUtils.isNotBlank(toolCallId)
        ? subagentContentAreaByToolCallId.get(toolCallId) : null;
    if (contentAreaId == null) {
      contentAreaId =
          ConversationHtmlBlockFactory.contentBlockId(copilotTurn.getParentTurnId(), true);
    }
    restoreCopilotTurnContent(copilotTurn.getTurnId(), contentAreaId, copilotTurn);
  }

  @Override
  public void renderModelInfo(String turnId, String modelName, double billingMultiplier,
      String reasoningEffort) {
    // When token-based billing is enabled, the per-turn billing multiplier is no longer
    // a meaningful price signal — hide it, matching StyledText widget behavior.
    double effectiveMultiplier = billingMultiplier;
    try {
      if (serviceManager != null && serviceManager.getAuthStatusManager()
          .getQuotaStatus().tokenBasedBillingEnabled()) {
        effectiveMultiplier = 0;
      }
    } catch (Exception e) {
      // Defensive: if quota status unavailable, fall back to showing multiplier
    }
    String blockId = ConversationHtmlBlockFactory.modelInfoBlockId(turnId);
    String html = htmlFactory.createModelInfoHtmlBlock(
        blockId, modelName, effectiveMultiplier, reasoningEffort);
    bridge.insertBlock(ConversationHtmlBlockFactory.copilotTurnContainerId(turnId), html);
  }

  @Override
  public void renderAgentMessage(CodingAgentMessageRequestParams params) {
    if (params == null) {
      return;
    }
    String turnId = params.getTurnId();
    if (StringUtils.isBlank(turnId)) {
      return;
    }
    String blockId = ConversationHtmlBlockFactory.agentMessageBlockId(turnId);
    String html = htmlFactory.createAgentMessageHtmlBlock(
        blockId, params.getTitle(), params.getDescription(), params.getPrLink());
    bridge.insertBlock(ConversationHtmlBlockFactory.copilotTurnContainerId(turnId), html);
  }

  @Override
  public CompletableFuture<LanguageModelToolConfirmationResult> requestToolConfirmation(
      String turnId, ConfirmationContent content, Object input) {
    // Cancel any existing pending confirmation
    cancelToolConfirmation(turnId);

    // Safety net: without actionable buttons the card can never be resolved by the user, so
    // returning a pending future would stall the agent indefinitely. Dismiss immediately instead.
    if (content == null || content.getActions() == null || content.getActions().isEmpty()) {
      return CompletableFuture.completedFuture(
          new LanguageModelToolConfirmationResult(ToolConfirmationResult.DISMISS));
    }

    // The confirmation request may arrive before the turn's begin event (e.g. when the terminal
    // call is the turn's first action), so the copilot turn container may not exist yet.
    ensureCopilotTurnContainer(turnId);

    // Remove "generating" animated dots which ensureCopilotTurnContainer() may have created.
    removeStreamingIndicator(turnId);

    CompletableFuture<LanguageModelToolConfirmationResult> future = new CompletableFuture<>();
    pendingConfirmationFuture = future;
    pendingConfirmationTurnId = turnId;
    pendingConfirmationActions = content.getActions();
    lastSelectedAction = null;

    String blockId = ConversationHtmlBlockFactory.confirmationBlockId(turnId);
    String html = htmlFactory.createConfirmationHtmlBlock(blockId, content, input);
    // Insert before the model-info block so confirmation appears above it
    String modelInfoId = ConversationHtmlBlockFactory.modelInfoBlockId(turnId);
    insertConfirmationBlock(turnId, html, modelInfoId, future);
    scrollToBottom();

    return future;
  }

  /**
   * Creates the copilot turn DOM container for {@code turnId} if {@link #beginTurn} has not run for
   * it yet. Delegates to {@code beginTurn}, which is idempotent, so a subsequent real begin event
   * for the same turn will not create a duplicate container.
   */
  private void ensureCopilotTurnContainer(String turnId) {
    if (turnStates.containsKey(turnId)) {
      return;
    }
    beginTurn(turnId, true, false);
  }

  /**
   * Inserts the confirmation card and verifies the DOM insertion actually happened. If the insertion
   * could not be performed (e.g. the target container is missing), the pending confirmation is
   * dismissed as a fallback so the agent never stalls waiting on a card that was never rendered.
   */
  private void insertConfirmationBlock(String turnId, String html, String beforeId,
      CompletableFuture<LanguageModelToolConfirmationResult> future) {
    bridge.insertBlockBefore(ConversationHtmlBlockFactory.copilotTurnContainerId(turnId), html,
        beforeId, success -> {
          if (!success && future == pendingConfirmationFuture && !future.isDone()) {
            CopilotCore.LOGGER.error(new IllegalStateException(
                "Failed to insert tool confirmation card for turn " + turnId
                    + "; dismissing to avoid a stall"));
            resolveConfirmation(-1, false);
          }
        });
  }

  @Override
  public void cancelToolConfirmation(String turnId) {
    if (pendingConfirmationFuture != null && !pendingConfirmationFuture.isDone()) {
      pendingConfirmationFuture.complete(
          new LanguageModelToolConfirmationResult(ToolConfirmationResult.DISMISS));
    }
    pendingConfirmationFuture = null;
    pendingConfirmationActions = null;
    pendingConfirmationTurnId = null;
    removeConfirmationBlock(turnId);
  }

  @Override
  public ConfirmationAction getLastSelectedConfirmationAction() {
    return lastSelectedAction;
  }

  private void resolveConfirmation(int actionIndex, boolean accepted) {
    if (pendingConfirmationFuture == null || pendingConfirmationFuture.isDone()) {
      return;
    }
    if (accepted && pendingConfirmationActions != null
        && actionIndex >= 0 && actionIndex < pendingConfirmationActions.size()) {
      lastSelectedAction = pendingConfirmationActions.get(actionIndex);
    }
    ToolConfirmationResult result = accepted
        ? ToolConfirmationResult.ACCEPT : ToolConfirmationResult.DISMISS;
    pendingConfirmationFuture.complete(new LanguageModelToolConfirmationResult(result));
    pendingConfirmationFuture = null;
    pendingConfirmationActions = null;
    if (pendingConfirmationTurnId != null) {
      removeConfirmationBlock(pendingConfirmationTurnId);
      pendingConfirmationTurnId = null;
    }
  }

  private void removeConfirmationBlock(String turnId) {
    if (turnId == null) {
      return;
    }
    String blockId = ConversationHtmlBlockFactory.confirmationBlockId(turnId);
    bridge.removeBlock(blockId);
  }

  /**
   * Processes a thinking chunk: starts a new thinking block or updates the current one.
   * Seals the thinking block if renderable output follows in the same event.
   */
  private void processThinking(String turnId, ChatProgressValue value) {
    Thinking thinking = value.getThinking();
    boolean hasThinking = thinking != null && thinking.text() != null
        && !thinking.text().isBlank();

    TurnStreamState state = stateFor(turnId);
    if (hasThinking) {
      if (state.currentBlockType != ChildBlockType.THINKING) {
        state.currentThinkingText = new StringBuilder(thinking.text());
        state.currentBlockType = ChildBlockType.THINKING;
        state.currentChildBlockId = ConversationHtmlBlockFactory.copilotChildBlockId(
            turnId, state.childBlockCounter++);
        state.currentThinkingBlockId = state.currentChildBlockId;
        String blockHtml = htmlFactory.createThinkingHtmlBlock(
            state.currentChildBlockId, thinking.text());
        bridge.insertBlock(state.contentAreaId, blockHtml);
      } else {
        state.currentThinkingText.append(thinking.text());
        bridge.updateThinkingBodyText(state.currentChildBlockId,
            htmlFactory.renderMarkdown(state.currentThinkingText.toString()));
      }
    }

    // Seal thinking if renderable output follows in the same event
    if (hasRenderableOutput(value) && state.currentBlockType == ChildBlockType.THINKING) {
      sealCurrentThinkingBlock(turnId);
    }
  }

  /**
   * Seals the current thinking block: collapses it, removes the spinner, and requests a title
   * from the language server. Mirrors the behavior of {@code ThinkingTurnWidget.sealThinking()}.
   * Uses targeted DOM manipulation instead of replacing the entire block.
   */
  private void sealCurrentThinkingBlock(String turnId) {
    TurnStreamState state = stateFor(turnId);
    if (state.currentThinkingBlockId == null || state.currentThinkingText == null) {
      state.currentBlockType = null;
      return;
    }
    String blockId = state.currentThinkingBlockId;
    String thinkingContent = state.currentThinkingText.toString();
    state.currentBlockType = null;

    // Collapse details, remove spinner (targeted DOM ops, no full re-render)
    bridge.collapseThinkingBlock(blockId, SvgIcons.get(SvgIcons.Icon.THINKING_BULB));

    // Request title from language server (async)
    requestThinkingTitle(blockId, thinkingContent, turnId);

    // The block is sealed; it is no longer the turn's active thinking block.
    state.currentThinkingBlockId = null;
    state.currentThinkingText = null;
  }

  /**
   * Requests a thinking block title from the language server and updates the summary on response.
   * Follows the same logic as {@code ThinkingTurnWidget}: extracts bold titles from thinking text
   * and sends either extractedTitles or the full content to the server.
   */
  private void requestThinkingTitle(String blockId, String thinkingContent, String turnId) {
    var ls = CopilotCore.getPlugin().getCopilotLanguageServer();
    if (ls == null || StringUtils.isBlank(thinkingContent)) {
      return;
    }
    var params = ThinkingTitles.buildTitleParams(
        thinkingContent, ThinkingTitles.extractTitles(thinkingContent));
    ls.generateThinkingTitle(params)
        .thenAccept(resp -> {
          if (resp != null && StringUtils.isNotBlank(resp.title())) {
            bridge.updateThinkingBlockTitle(blockId, htmlFactory.renderMarkdownInline(resp.title()));
            persistThinkingTitle(turnId, blockId, resp.title());
          }
        })
        .exceptionally(ex -> null);
  }

  private void persistThinkingTitle(String turnId, String thinkingBlockId, String title) {
    if (serviceManager != null) {
      ThinkingTitles.persistTitle(serviceManager.getPersistenceManager(),
          conversationId, turnId, thinkingBlockId, title);
    }
  }

  /**
   * Processes agent rounds: handles tool calls and agent-round replies.
   */
  private void processAgentRounds(String turnId, ChatProgressValue value) {
    List<AgentRound> agentRounds = value.getAgentRounds();
    if (agentRounds == null || agentRounds.isEmpty()) {
      return;
    }
    AgentRound round = agentRounds.get(0);
    if (round.getToolCalls() != null && !round.getToolCalls().isEmpty()) {
      AgentToolCall toolCall = round.getToolCalls().get(0);
      processToolCall(turnId, toolCall);
    }
  }

  /**
   * Processes a reply chunk: starts a new response block or updates the current one.
   */
  private void processReply(String turnId, String replyChunk) {
    if (replyChunk == null || replyChunk.isEmpty()) {
      return;
    }
    TurnStreamState state = stateFor(turnId);
    if (state.currentBlockType != ChildBlockType.RESPONSE) {
      state.currentReplyText = new StringBuilder(replyChunk);
      state.currentBlockType = ChildBlockType.RESPONSE;
      state.currentChildBlockId = ConversationHtmlBlockFactory.copilotChildBlockId(
          turnId, state.childBlockCounter++);
      String blockHtml = htmlFactory.createCopilotReplyHtmlBlock(
          state.currentChildBlockId, state.currentReplyText.toString());
      bridge.insertBlock(state.contentAreaId, blockHtml);
    } else {
      state.currentReplyText.append(replyChunk);
      String blockHtml = htmlFactory.createCopilotReplyHtmlBlock(
          state.currentChildBlockId, state.currentReplyText.toString());
      bridge.replaceBlock(state.currentChildBlockId, blockHtml);
    }
  }

  /**
   * Processes a tool call: creates a new tool call block or updates an existing one. The
   * {@code run_subagent} tool call is not rendered as a normal block — it drives subagent nesting
   * (mirrors {@code BaseTurnWidget} in the SWT renderer).
   */
  private void processToolCall(String turnId, AgentToolCall toolCall) {
    if (toolCall != null && "run_subagent".equalsIgnoreCase(toolCall.getName())) {
      handleSubagentToolCall(turnId, toolCall);
      return;
    }
    TurnStreamState state = stateFor(turnId);
    String toolCallId = toolCall.getId();
    boolean isSameToolCall = state.currentBlockType == ChildBlockType.TOOL_CALL
        && state.currentChildBlockId != null
        && toolCallId != null && toolCallId.equals(state.currentToolCallId);

    if (!isSameToolCall) {
      state.currentBlockType = ChildBlockType.TOOL_CALL;
      state.currentToolCallId = toolCallId;
      String blockId =
          ConversationHtmlBlockFactory.toolCallBlockId(turnId, state.childBlockCounter++);
      state.currentChildBlockId = blockId;
      String blockHtml = htmlFactory.createToolCallHtmlBlock(blockId, toolCall);
      bridge.insertBlock(state.contentAreaId, blockHtml);
    } else {
      String blockHtml = htmlFactory.createToolCallHtmlBlock(
          state.currentChildBlockId, toolCall);
      bridge.replaceBlock(state.currentChildBlockId, blockHtml);
    }
  }

  /**
   * Handles the parent turn's {@code run_subagent} tool call. On {@code running} it opens a nested
   * subagent block under the parent's content area; on completion/cancellation/error it closes the
   * active nesting. Mirrors {@code BaseTurnWidget.handleSubagentToolCall} in the SWT renderer.
   */
  private void handleSubagentToolCall(String parentTurnId, AgentToolCall toolCall) {
    String status = toolCall.getStatus() == null ? "" : toolCall.getStatus().toLowerCase();
    String toolCallId = toolCall.getId();
    switch (status) {
      case "running" -> {
        if (activeSubagentContentAreaId == null && StringUtils.isNotBlank(toolCallId)) {
          activeSubagentContentAreaId = openSubagentBlock(parentTurnId, toolCallId,
              subagentTitle(toolCall.getProgressMessage(), toolCall.getName()));
        }
      }
      case "completed", "cancelled", "error" -> activeSubagentContentAreaId = null;
      default -> {
        // pending/unknown: nothing to do yet
      }
    }
  }

  /**
   * Inserts a nested subagent block under the given parent turn's content area (idempotent per
   * tool-call id) and returns the id of its inner content area, where the subagent's child blocks
   * are inserted.
   */
  private String openSubagentBlock(String parentTurnId, String toolCallId, String title) {
    String existing = subagentContentAreaByToolCallId.get(toolCallId);
    if (existing != null) {
      return existing;
    }
    String blockId = ConversationHtmlBlockFactory.subagentBlockId(parentTurnId, toolCallId);
    String contentAreaId =
        ConversationHtmlBlockFactory.subagentContentAreaId(parentTurnId, toolCallId);
    String html = htmlFactory.createSubagentBlockHtmlBlock(
        blockId, contentAreaId, copilotAvatarDataUri, title);
    bridge.insertBlock(ConversationHtmlBlockFactory.contentBlockId(parentTurnId, true), html);
    subagentContentAreaByToolCallId.put(toolCallId, contentAreaId);
    return contentAreaId;
  }

  /** Derives the subagent card title: progress message, else tool name, else "Subagent". */
  private static String subagentTitle(String progressMessage, String name) {
    if (StringUtils.isNotBlank(progressMessage)) {
      return progressMessage;
    }
    if (StringUtils.isNotBlank(name)) {
      return name;
    }
    return "Subagent";
  }

  /**
   * Processes a streaming error message: renders a warning block with the error text and,
   * for quota-exceeded (402) errors, plan-driven action buttons.
   */
  private void processErrorMessage(String turnId, String message, int code,
      String modelProviderName) {
    List<QuotaAction> actions = resolveQuotaActions(code, modelProviderName);
    TurnStreamState state = stateFor(turnId);
    String blockId =
        ConversationHtmlBlockFactory.copilotChildBlockId(turnId, state.childBlockCounter++);
    state.currentBlockType = null;
    bridge.insertBlock(state.contentAreaId,
        htmlFactory.createWarningMessageHtmlBlock(blockId, message, actions));
    // Scroll the newly inserted warning banner (e.g. quota-exceeded / 402 with plan-driven
    // action buttons) into view so the user notices the required action, mirroring the SWT
    // renderer's showControl-based scroll for warn banners.
    scrollToBottom();
  }

  /**
   * Resolves the quota action buttons appropriate for an error code. Returns an empty list
   * for non-quota errors or when the required auth status is unavailable.
   */
  private List<QuotaAction> resolveQuotaActions(int code, String modelProviderName) {
    if (serviceManager == null) {
      return List.of();
    }
    try {
      return QuotaActions.forQuotaStatus(
          serviceManager.getAuthStatusManager().getQuotaStatus(), code, modelProviderName);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to resolve quota actions", e);
      return List.of();
    }
  }

  /** Finalizes a turn: seals thinking, final response render, and resets its transient pointers. */
  private void finalizeTurn(String turnId) {
    TurnStreamState state = turnStates.get(turnId);
    if (state == null) {
      return;
    }
    // Seal any active thinking block before the turn ends
    if (state.currentBlockType == ChildBlockType.THINKING) {
      sealCurrentThinkingBlock(turnId);
    } else if (state.currentBlockType == ChildBlockType.RESPONSE
        && state.currentReplyText != null && state.currentReplyText.length() > 0) {
      String blockHtml = htmlFactory.createCopilotReplyHtmlBlock(
          state.currentChildBlockId, state.currentReplyText.toString());
      bridge.replaceBlock(state.currentChildBlockId, blockHtml);
    }
    state.resetTransient();
  }

  private void restoreCopilotTurnContent(String turnId, String contentId,
      CopilotTurnData copilotTurn) {
    ReplyData replyData = copilotTurn.getReply();
    if (replyData == null) {
      return;
    }

    int blockIdx = 0;

    if (StringUtils.isNotBlank(replyData.getText())) {
      String blockId = ConversationHtmlBlockFactory.copilotChildBlockId(turnId, blockIdx++);
      bridge.insertBlock(contentId,
          htmlFactory.createCopilotReplyHtmlBlock(blockId, replyData.getText()));
    }

    if (replyData.getEditAgentRounds() != null) {
      for (EditAgentRoundData round : replyData.getEditAgentRounds()) {
        ThinkingBlockData thinkingBlock = round.getThinkingBlock();
        if (thinkingBlock != null && StringUtils.isNotBlank(thinkingBlock.getContent())) {
          String blockId = ConversationHtmlBlockFactory.copilotChildBlockId(turnId, blockIdx++);
          bridge.insertBlock(contentId,
              htmlFactory.createRestoredThinkingHtmlBlock(blockId, thinkingBlock));
        }
        // Reply text renders before tool calls so it appears above them (and above any nested
        // subagent card opened by a run_subagent tool call), matching StyledTextConversationWidget.
        if (StringUtils.isNotBlank(round.getReply())) {
          String blockId = ConversationHtmlBlockFactory.copilotChildBlockId(turnId, blockIdx++);
          bridge.insertBlock(contentId,
              htmlFactory.createCopilotReplyHtmlBlock(blockId, round.getReply()));
        }
        if (round.getToolCalls() != null) {
          for (ToolCallData tc : round.getToolCalls()) {
            // A run_subagent tool call opens a nested block; the subagent turn (restored
            // afterwards) renders into it rather than showing a normal tool-call block.
            if (tc != null && "run_subagent".equalsIgnoreCase(tc.getName())
                && StringUtils.isNotBlank(tc.getId())) {
              openSubagentBlock(turnId, tc.getId(),
                  subagentTitle(tc.getProgressMessage(), tc.getName()));
              continue;
            }
            String blockId = ConversationHtmlBlockFactory.copilotChildBlockId(turnId, blockIdx++);
            bridge.insertBlock(contentId,
                htmlFactory.createRestoredToolCallHtmlBlock(blockId, tc));
          }
        }
      }
    }

    if (replyData.getErrorMessages() != null) {
      for (ErrorMessageData errorMessageData : replyData.getErrorMessages()) {
        ErrorData errorData = errorMessageData.getError();
        String errorMessage = errorData != null
            ? errorData.getMessage() : "An error occurred";
        int errorCode = errorData != null ? errorData.getCode() : 0;
        String modelProviderName = errorData != null ? errorData.getModelProviderName() : null;
        List<QuotaAction> actions = resolveQuotaActions(errorCode, modelProviderName);
        String blockId = ConversationHtmlBlockFactory.copilotChildBlockId(turnId, blockIdx++);
        bridge.insertBlock(contentId,
            htmlFactory.createWarningMessageHtmlBlock(blockId, errorMessage, actions));
      }
    }

    if (replyData.getAgentMessages() != null) {
      for (AgentMessageData agentMsg : replyData.getAgentMessages()) {
        if (StringUtils.equals(agentMsg.getAgentSlug(),
            UiConstants.GITHUB_COPILOT_CODING_AGENT_SLUG)) {
          String blockId = ConversationHtmlBlockFactory.copilotChildBlockId(turnId, blockIdx++);
          bridge.insertBlock(contentId,
              htmlFactory.createAgentMessageHtmlBlock(blockId,
                  agentMsg.getTitle(), agentMsg.getDescription(),
                  agentMsg.getPrLink()));
        }
      }
    }
  }

  private void showStreamingIndicator(String turnId) {
    TurnStreamState state = stateFor(turnId);
    String indicatorId = ConversationHtmlBlockFactory.streamingIndicatorId(turnId);
    String html = htmlFactory.createStreamingIndicatorHtmlBlock(indicatorId);
    bridge.insertBlock(state.contentAreaId, html);
    state.streamingIndicatorVisible = true;
  }

  private void removeStreamingIndicator(String turnId) {
    TurnStreamState state = turnStates.get(turnId);
    if (state == null || !state.streamingIndicatorVisible) {
      return;
    }
    bridge.removeBlock(ConversationHtmlBlockFactory.streamingIndicatorId(turnId));
    state.streamingIndicatorVisible = false;
  }

  private boolean hasRenderableOutput(ChatProgressValue value) {
    if (StringUtils.isNotBlank(value.getReply())) {
      return true;
    }
    if (value.getAgentRounds() != null && !value.getAgentRounds().isEmpty()) {
      AgentRound round = value.getAgentRounds().get(0);
      if (round.getReply() != null && !round.getReply().isEmpty()) {
        return true;
      }
      if (round.getToolCalls() != null && !round.getToolCalls().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private String extractReplyChunk(ChatProgressValue value) {
    if (value.getAgentRounds() != null && !value.getAgentRounds().isEmpty()) {
      return value.getAgentRounds().get(0).getReply();
    }
    return value.getReply();
  }

  /** Returns the streaming state for a turn, creating a default top-level one if absent. */
  private TurnStreamState stateFor(String turnId) {
    return turnStates.computeIfAbsent(turnId,
        id -> new TurnStreamState(ConversationHtmlBlockFactory.contentBlockId(id, true)));
  }

  /**
   * Ensures a streaming state exists for a turn seen in a {@code report} event. Normally the state
   * is created by {@link #beginTurn}; this defensively creates one (nested when a subagent is
   * active) if the begin event was not observed.
   */
  private TurnStreamState ensureTurnState(String turnId, ChatProgressValue value) {
    TurnStreamState state = turnStates.get(turnId);
    if (state != null) {
      return state;
    }
    boolean nested = value != null && StringUtils.isNotBlank(value.getParentTurnId())
        && activeSubagentContentAreaId != null;
    String contentAreaId = nested
        ? activeSubagentContentAreaId
        : ConversationHtmlBlockFactory.contentBlockId(turnId, true);
    state = new TurnStreamState(contentAreaId);
    turnStates.put(turnId, state);
    return state;
  }

  private String getUserDisplayName() {
    return serviceManager.getAvatarService().getUserName();
  }

  private String getCopilotDisplayName() {
    return serviceManager.getAvatarService().getCopilotName();
  }

  /** Returns the current user's avatar as a {@code data:} URI (see {@link AvatarService}). */
  private String getUserAvatarUri() {
    return serviceManager.getAvatarService().getAvatarForCurrentUserAsDataUri();
  }

  @Override
  public void copyToClipboard(String code) {
    Display.getDefault().asyncExec(() -> {
      Clipboard clipboard = new Clipboard(Display.getDefault());
      clipboard.setContents(
          new Object[]{code},
          new Transfer[]{TextTransfer.getInstance()});
      clipboard.dispose();
    });
  }

  @Override
  public void insertAtCursor(String code) {
    Display.getDefault().asyncExec(() -> {
      if (browser.isDisposed()) {
        return;
      }
      IEditorPart editor = SwtUtils.getActiveEditorPart();
      if (editor == null) {
        showInsertError("Cannot Insert", "No active editor found.");
        return;
      }

      ITextEditor textEditor = editor instanceof ITextEditor te
          ? te
          : editor.getAdapter(ITextEditor.class);
      if (textEditor == null) {
        CopilotCore.LOGGER
            .error(new IllegalStateException("The active editor doesn't support text insertion."));
        showInsertError("Cannot Insert", "The active editor doesn't support text insertion.");
        return;
      }

      IDocumentProvider provider = textEditor.getDocumentProvider();
      IDocument doc = provider == null ? null : provider.getDocument(textEditor.getEditorInput());
      if (doc == null) {
        CopilotCore.LOGGER
            .error(new IllegalStateException("Failed to get the document from the active editor."));
        showInsertError("Cannot Insert", "Failed to get the document from the active editor.");
        return;
      }

      try {
        ITextSelection sel = (ITextSelection) textEditor.getSelectionProvider().getSelection();
        int offset = sel.getOffset();
        doc.replace(offset, sel.getLength(), code);

        // Set the cursor position after the inserted text
        textEditor.selectAndReveal(offset + code.length(), 0);
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error("Failed to insert code at cursor", e);
        showInsertError("Insert Failed", "An error occurred while inserting the code: "
            + e.getMessage());
      }
    });
  }

  /**
   * Shows a modal error dialog for a failed "Insert into editor" action, matching the feedback the
   * StyledText renderer gives for the same failures. The dialog restores focus to the previously
   * focused control on dismissal, so no explicit focus handling is needed here.
   *
   * @param title the dialog title
   * @param message the human-readable reason the insertion could not be performed
   */
  private void showInsertError(String title, String message) {
    if (browser.isDisposed()) {
      return;
    }
    MessageDialog.openError(browser.getShell(), title, message);
  }

  @Override
  public void acceptToolAction(int actionIndex) {
    resolveConfirmation(actionIndex, true);
  }

  @Override
  public void dismissToolAction() {
    resolveConfirmation(-1, false);
  }

  @Override
  public void copilotAction(String action, String param) {
    switch (action) {
      case "openLink":
        UiUtils.openLink(param);
        break;
      case "openJobList":
        UiUtils.openE4Part(Constants.GITHUB_JOBS_VIEW_ID);
        break;
      default:
        break;
    }
  }

  @Override
  public void logError(String message) {
    CopilotCore.LOGGER.error(new IllegalStateException("Browser chat renderer: " + message));
  }
}

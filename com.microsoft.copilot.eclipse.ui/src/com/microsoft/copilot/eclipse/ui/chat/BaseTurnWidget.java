// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent.CodingAgentMessageRequestParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.persistence.ConversationDataFactory;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.EditAgentRoundData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ReplyData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Base class for a custom widget that displays a turn.
 */
public abstract class BaseTurnWidget extends Composite {
  protected static final String CODE_BLOCK_ANNOTATION = "```";

  protected ChatServiceManager serviceManager;

  // Widgets
  protected SourceViewer currentTextBlock;
  protected SourceViewerComposite currentCodeBlock;
  protected Map<String, AgentStatusLabel> statusLabels;
  protected SubagentMessageBlock currentSubagentBlock;
  protected Map<String, SubagentMessageBlock> subagentBlocks;

  // Data
  protected StringBuilder messageBuffer;
  protected StringBuilder mdContentBuilder;
  protected boolean inCodeBlock;
  protected boolean isCopilot;
  protected String turnId;
  protected int codeBlockIndex;
  protected boolean inSubagentBlock;
  protected String overrideRoleName;

  // Resource
  protected Image icon = null;
  protected Font boldFont = null;
  protected InvokeToolConfirmationDialog confirmDialog;

  /** Returns the current confirmation dialog, or {@code null} if none active. */
  public InvokeToolConfirmationDialog getConfirmDialog() {
    return confirmDialog;
  }

  // Footer
  protected Composite footer;

  // Event handling
  protected EventHandler cancelMsgEventHandler;
  protected Runnable roleNameFontChangeCallback;
  protected Label roleNameLabel;

  /**
   * Create the widget.
   *
   * @param parent the parent composite
   * @param style the style
   * @param serviceManager the service manager
   * @param turnId the turn ID
   * @param isCopilot whether this is a copilot turn
   * @param overrideRoleName optional role name to override getRoleName(), can be null
   */
  protected BaseTurnWidget(Composite parent, int style, ChatServiceManager serviceManager, String turnId,
      boolean isCopilot, String overrideRoleName) {
    super(parent, style);
    this.overrideRoleName = overrideRoleName;
    this.messageBuffer = new StringBuilder();
    this.mdContentBuilder = new StringBuilder();
    this.serviceManager = serviceManager;
    this.isCopilot = isCopilot;
    this.turnId = turnId;
    this.codeBlockIndex = 1;
    this.statusLabels = new HashMap<>();
    this.subagentBlocks = new HashMap<>();
    // editor group
    // align all children vertically
    GridLayout gl = new GridLayout(1, true);
    gl.marginRight = 5;
    gl.marginLeft = 5;
    setLayout(gl);
    setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    createContent();
    layout();

    // TODO: the event broker can be injected once we fully migrated to e4 and use ui injection
    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    this.cancelMsgEventHandler = event -> {
      cancelToolConfirmation();
      onChatMessageCancelled();
    };
    eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_MESSAGE_CANCELLED, cancelMsgEventHandler);
  }

  /** Hook invoked when the chat message cancel event is broadcast. Default no-op; subclasses may override. */
  protected void onChatMessageCancelled() {
    // no-op
  }

  public String getTurnId() {
    return turnId;
  }

  /**
   * Get the active turn widget for tool execution.
   * If we're in a subagent context, returns the subagent turn widget.
   * Otherwise, returns this widget.
   *
   * @return the active turn widget for tool operations
   */
  public BaseTurnWidget getActiveTurnWidget() {
    if (inSubagentBlock && currentSubagentBlock != null) {
      BaseTurnWidget subagentWidget = currentSubagentBlock.getSubagentTurnWidget();
      if (subagentWidget != null) {
        return subagentWidget;
      }
    }
    return this;
  }

  private void createContent() {
    Composite cmpTitle = new Composite(this, SWT.NONE);
    GridLayout titleLayout = new GridLayout(2, false);
    titleLayout.marginLeft = -5;
    cmpTitle.setLayout(titleLayout);
    cmpTitle.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    AvatarService avatarService = serviceManager.getAvatarService();
    icon = getAvatar(avatarService);
    Label lblAvatar = createAvatarLabel(cmpTitle);
    lblAvatar.setImage(icon);

    roleNameLabel = new Label(cmpTitle, SWT.NONE);
    String name = overrideRoleName != null ? overrideRoleName : getRoleName();
    roleNameLabel.setText(name);
    GridData roleNameGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    roleNameGridData.horizontalIndent = 0;
    roleNameLabel.setLayoutData(roleNameGridData);

    // Register for chat font updates via centralized service with callback for bold font
    roleNameFontChangeCallback = this::applyRoleNameFont;
    serviceManager.getChatFontService().registerCallback(roleNameFontChangeCallback);

    roleNameLabel.addDisposeListener(e -> {
      if (this.boldFont != null) {
        this.boldFont.dispose();
      }
      if (roleNameFontChangeCallback != null) {
        serviceManager.getChatFontService().unregisterCallback(roleNameFontChangeCallback);
      }
    });
  }

  /**
   * Apply the chat font (bold) to the role name label.
   */
  private void applyRoleNameFont() {
    if (roleNameLabel == null || roleNameLabel.isDisposed()) {
      return;
    }
    // Dispose old font if exists
    if (this.boldFont != null) {
      this.boldFont.dispose();
    }
    // Create bold version of the chat font (or fallback font)
    this.boldFont = UiUtils.getBoldChatFont(this.getDisplay(), roleNameLabel.getFont());
    roleNameLabel.setFont(this.boldFont);
    roleNameLabel.requestLayout();
  }

  /**
   * Get the avatar image.
   */
  protected abstract Image getAvatar(AvatarService avatarService);

  /**
   * Get the role name.
   */
  protected abstract String getRoleName();

  /**
   * Create the avatar label.
   */
  protected abstract Label createAvatarLabel(Composite parent);

  /**
   * Add a message to the turn.
   *
   * @param message the message
   */
  public void appendMessage(String message) {
    if (StringUtils.isEmpty(message)) {
      return;
    }

    // If we're in a subagent block, route messages there
    if (inSubagentBlock && currentSubagentBlock != null) {
      currentSubagentBlock.appendMessage(message);
      return;
    }

    messageBuffer.append(message);
    int newlineIndex;
    while ((newlineIndex = messageBuffer.indexOf("\n")) != -1) {
      String line = messageBuffer.substring(0, newlineIndex + 1);
      messageBuffer.delete(0, newlineIndex + 1);
      processMessageLine(line);
    }
  }

  /**
   * Add a status message to the turn.
   *
   * @param toolCall the tool call of the agent turn
   */
  public void appendToolCallStatus(AgentToolCall toolCall) {
    if (toolCall == null || toolCall.getStatus() == null) {
      return;
    }

    // Subagent tool calls drive routing state for `currentSubagentBlock`/`inSubagentBlock`,
    // so they must always be dispatched, even when the terminal event has no display message.
    if ("run_subagent".equalsIgnoreCase(toolCall.getName())) {
      handleSubagentToolCall(toolCall);
      return;
    }

    String status = toolCall.getStatus().toLowerCase();
    // Non-error events require a non-blank progressMessage to render (otherwise we'd
    // call ChatMarkupViewer#setMarkup(null) and NPE). Error events always pass through:
    // getErrorDisplayText(...) provides a non-blank fallback.
    boolean isError = "error".equals(status);
    if (!isError && StringUtils.isBlank(toolCall.getProgressMessage())) {
      return;
    }

    // If we're in a subagent block, route tool calls there
    if (inSubagentBlock && currentSubagentBlock != null) {
      currentSubagentBlock.appendToolCallStatus(toolCall);
      return;
    }

    reset();

    // We will skip updating status here, if the cancelled event is already handled in
    // InvokeToolConfirmationDialog.cancelConfirmation()
    Control[] children = this.getChildren();
    if (children.length > 0 && children[children.length - 1] instanceof AgentToolCancelLabel) {
      return;
    }

    AgentStatusLabel statusLabel = statusLabels.computeIfAbsent(toolCall.getId(),
        id -> new AgentStatusLabel(this, SWT.LEFT));

    switch (status) {
      case "running":
        statusLabel.setRunningStatus(toolCall.getProgressMessage());
        break;
      case "completed":
        statusLabel.setCompletedStatus(toolCall.getProgressMessage());
        break;
      case "cancelled":
        statusLabel.setCancelledStatus();
        statusLabel.setText(toolCall.getProgressMessage());
        break;
      case "error":
        statusLabel.setErrorStatus();
        statusLabel.setText(getErrorDisplayText(toolCall));
        break;
      default:
        statusLabel.setErrorStatus();
        CopilotCore.LOGGER.error(new IllegalStateException("Unknown status: " + status));
    }
  }

  /**
   * Handle run_subagent tool call specially.
   *
   * @param toolCall the subagent tool call
   */
  private void handleSubagentToolCall(AgentToolCall toolCall) {
    String status = toolCall.getStatus().toLowerCase();

    // TODO: Extract tool call status to enum and reuse it in AgentStatusLabel
    switch (status) {
      case "running":
        // Start of subagent block
        if (currentSubagentBlock == null) {
          reset();
          inSubagentBlock = true;
          currentSubagentBlock = new SubagentMessageBlock(this, SWT.NONE, serviceManager, toolCall.getId(), toolCall);
          currentSubagentBlock.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
          subagentBlocks.put(toolCall.getId(), currentSubagentBlock);
          requestLayout();
        }
        break;
      case "completed":
        // End of subagent block
        if (currentSubagentBlock != null) {
          currentSubagentBlock.flushMessageBuffer();
          inSubagentBlock = false;
          currentSubagentBlock = null;
          requestLayout();
        } else if (!subagentBlocks.containsKey(toolCall.getId())) {
          // Restoration path: create a completed subagent block for later content injection
          reset();
          SubagentMessageBlock block = new SubagentMessageBlock(this, SWT.NONE, serviceManager, toolCall.getId(),
              toolCall);
          block.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
          subagentBlocks.put(toolCall.getId(), block);
          requestLayout();
        }
        break;
      case "cancelled":
      case "error":
        // Handle errors in subagent
        if (currentSubagentBlock != null) {
          currentSubagentBlock.flushMessageBuffer();
          inSubagentBlock = false;
          currentSubagentBlock = null;
        }
        // Show error status
        AgentStatusLabel statusLabel = statusLabels.computeIfAbsent(toolCall.getId(),
            id -> new AgentStatusLabel(this, SWT.LEFT));
        if ("cancelled".equals(status)) {
          statusLabel.setCancelledStatus();
          if (StringUtils.isNotBlank(toolCall.getProgressMessage())) {
            statusLabel.setText(toolCall.getProgressMessage());
          }
        } else {
          statusLabel.setErrorStatus();
          statusLabel.setText(getErrorDisplayText(toolCall));
        }
        requestLayout();
        break;
      default:
        statusLabel = statusLabels.computeIfAbsent(toolCall.getId(),
            id -> new AgentStatusLabel(this, SWT.LEFT));
        statusLabel.setErrorStatus();
        CopilotCore.LOGGER.error(new IllegalStateException("Unknown status: " + status));
        break;
    }
  }

  /**
  /**
   * Restores subagent content into the SubagentMessageBlock identified by the tool call ID. Creates the block if it
   * doesn't exist (for restoration from persisted data). Used during conversation history restoration.
   *
   * @param toolCallId the run_subagent tool call ID
   * @param copilotTurn the subagent's CopilotTurnData
   * @param dataFactory the factory for converting tool call data
   */
  public void restoreSubagentContent(String toolCallId, CopilotTurnData copilotTurn,
      ConversationDataFactory dataFactory) {
    // Find existing SubagentMessageBlock or create one for restoration
    SubagentMessageBlock block = subagentBlocks.get(toolCallId);
    if (block == null) {
      block = new SubagentMessageBlock(this, SWT.NONE, serviceManager, toolCallId, null);
      block.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      subagentBlocks.put(toolCallId, block);
      requestLayout();
    }

    // Append subagent's content into the block
    ReplyData replyData = copilotTurn.getReply();
    if (replyData == null) {
      return;
    }

    // Restore thinking blocks for the subagent
    BaseTurnWidget subagentWidget = block.getSubagentTurnWidget();
    ThinkingTurnWidget thinkingWidget =
        subagentWidget instanceof ThinkingTurnWidget ? (ThinkingTurnWidget) subagentWidget : null;

    if (StringUtils.isNotBlank(replyData.getText())) {
      block.appendMessage(replyData.getText());
    }

    if (replyData.getEditAgentRounds() != null) {
      for (EditAgentRoundData round : replyData.getEditAgentRounds()) {
        // Restore thinking block before the round's reply and tool calls
        if (thinkingWidget != null && round.getThinkingBlock() != null) {
          thinkingWidget.restoreThinkingBlock(round.getThinkingBlock());
        }
        if (round.getReply() != null && !round.getReply().isEmpty()) {
          block.appendMessage(round.getReply());
        }
        if (round.getToolCalls() != null) {
          for (ToolCallData toolCallData : round.getToolCalls()) {
            AgentToolCall agentToolCall = dataFactory.convertToolCallDataToAgentToolCall(toolCallData);
            block.appendToolCallStatus(agentToolCall);
          }
        }
      }
    }

    // Restore error messages into the subagent block
    if (replyData.getErrorMessages() != null) {
      BaseTurnWidget errorWidget = block.getSubagentTurnWidget();
      if (errorWidget != null) {
        for (CopilotTurnData.ErrorMessageData errorMessageData : replyData.getErrorMessages()) {
          CopilotTurnData.ErrorData errorData = errorMessageData.getError();
          String errorMessage = errorData != null ? errorData.getMessage() : "";
          int errorCode = errorData != null ? errorData.getCode() : 0;
          String modelProviderName = errorData != null ? errorData.getModelProviderName() : null;
          errorWidget.createWarnDialog(errorMessage, errorCode, modelProviderName);
        }
      }
    }

    block.flushMessageBuffer();
  }

  /**
   * Resolve the user-facing error text for a tool call.
   *
   * <p>Picks the first non-blank of {@code toolCall.getError()} or {@code toolCall.getProgressMessage()},
   * falls back to a generic message when both are blank, and prefixes the result with the tool name
   * so the user knows which tool failed.
   *
   * @param toolCall the failing tool call
   * @return a non-blank, prefixed display string suitable for {@link AgentStatusLabel#setText(String)}
   */
  private static String getErrorDisplayText(AgentToolCall toolCall) {
    String detail = toolCall.getError();
    if (StringUtils.isBlank(detail)) {
      detail = toolCall.getProgressMessage();
    }
    if (StringUtils.isBlank(detail)) {
      detail = Messages.chat_toolCall_genericError;
    }
    String name = toolCall.getName();
    if (StringUtils.isBlank(name)) {
      return detail;
    }
    return NLS.bind(Messages.chat_toolCall_errorTemplate, name, detail);
  }

  private void processMessageLine(String line) {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (line.trim().startsWith(CODE_BLOCK_ANNOTATION)) {
        if (inCodeBlock) {
          // end of code block
          inCodeBlock = false;
          currentCodeBlock = null;
        } else {
          // start of code block
          inCodeBlock = true;
          mdContentBuilder.setLength(0);
          currentTextBlock = null;
          String language = line.trim().substring(CODE_BLOCK_ANNOTATION.length());
          createCodeBlock(language);
        }
      } else {
        if (inCodeBlock) {
          if (currentCodeBlock == null) {
            this.createCodeBlock("plaintext");
          }
          appendTextToSourceViewer(line);
        } else {
          mdContentBuilder.append(line);
          appendTextToTextViewer(mdContentBuilder.toString());
        }
      }
    }, this);
  }

  private void appendTextToSourceViewer(String text) {
    if (currentCodeBlock == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("source viewer is null to append text"));
      return;
    }
    this.currentCodeBlock.setText(text);
  }

  private void appendTextToTextViewer(String text) {
    if (currentTextBlock == null) {
      this.createTextBlock();
      ensureFooterAtBottom();
    }
    if (currentTextBlock instanceof ChatMarkupViewer markupViewer) {
      markupViewer.setMarkup(text);
    } else {
      currentTextBlock.setDocument(new Document(text));
    }
  }

  /**
   * Flushes any buffered, not-yet-newline-terminated message text by processing it as a final
   * line and clearing the buffer.
   */
  public void flushMessageBuffer() {
    if (messageBuffer.length() > 0) {
      this.processMessageLine(messageBuffer.toString());
      messageBuffer.setLength(0);
    }
  }

  private void reset() {
    if (messageBuffer.length() > 0) {
      this.processMessageLine(messageBuffer.toString());
    }

    // Cancel the existing dialog to prevent resource leaks
    // TODO: Support multiple confirmation dialogs so that we can pend multiple tool invocations
    if (this.confirmDialog != null) {
      this.confirmDialog.cancelConfirmation();
      this.confirmDialog = null;
    }

    this.messageBuffer.setLength(0);
    this.mdContentBuilder.setLength(0);
    this.currentCodeBlock = null;
    this.currentTextBlock = null;
    this.inCodeBlock = false;

    // Subagent and thinking blocks have their own lifecycle (tool call status / ThinkingTurnWidget)
    // and are intentionally not reset here.
  }

  /**
   * Add a code block to the turn.
   *
   * @param code the code block
   */
  private void createCodeBlock(String language) {
    final SourceViewerComposite codeBlock = new SourceViewerComposite(this, SWT.BORDER, this.serviceManager, language,
        turnId, this.codeBlockIndex);
    this.addDisposeListener(e -> codeBlock.dispose());
    codeBlock.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    codeBlock.layout();

    this.currentCodeBlock = codeBlock;
    this.codeBlockIndex++;
    ensureFooterAtBottom();
  }

  /**
   * Create the appropriate type of text block based on implementation.
   */
  protected abstract void createTextBlock();

  /**
   * Create an optional footer component. Subclasses can override this to define footer structure.
   * The footer is placed at the bottom of the turn widget.
   * This should be called only when there is content to display (e.g., model info is available).
   */
  protected void createFooter() {
    // Base implementation - subclasses should override to create footer structure
  }

  /**
   * Move the footer composite (if any) to the bottom of this turn widget. Should be called after
   * adding a new child widget so the footer always remains visually at the bottom of the turn,
   * including when content is appended to the same turn after the footer is first created (for
   * example, when a legacy 402 response triggers a fallback-model replay into the same turn).
   */
  protected void ensureFooterAtBottom() {
    if (this.footer != null && !this.footer.isDisposed()) {
      this.footer.moveBelow(null);
    }
  }

  /**
   * Render a warning dialog under the turn. BYOK 402 (402 + non-blank provider name) shows the BYOK message with no
   * actions; plain 402 shows the server message plus plan-driven actions; other codes show the server message only.
   *
   * @param message the server error message
   * @param code the server error code
   * @param modelProviderName the BYOK model-provider name, or {@code null} for built-in models
   */
  protected void createWarnDialog(String message, int code, String modelProviderName) {
    // TODO: Remove this legacy fallback after TBB is officially released.
    // When the language server has not enabled token-based billing yet, restore the original
    // main-branch warning behavior (no plan-driven actions; single upgrade button on the legacy
    // 30-day free trial message).
    if (!this.serviceManager.getAuthStatusManager().getQuotaStatus().tokenBasedBillingEnabled()) {
      new WarnWidget(this, SWT.BOTTOM, message, code);
      ensureFooterAtBottom();
      requestLayout();
      return;
    }
    boolean byokQuotaExceeded = QuotaActions.isByokQuotaExceeded(code, modelProviderName);
    String displayMessage = byokQuotaExceeded ? Messages.chat_warnWidget_byokQuotaUsageMessage : message;
    CopilotPlan planForActions = null;
    boolean overageEnabled = false;
    Boolean canUpgradePlan = null;
    if (code == 402 && !byokQuotaExceeded) {
      CheckQuotaResult quotaStatus = this.serviceManager.getAuthStatusManager().getQuotaStatus();
      planForActions = quotaStatus.copilotPlan();
      overageEnabled = quotaStatus.premiumInteractions() != null
          && quotaStatus.premiumInteractions().overagePermitted();
      canUpgradePlan = quotaStatus.canUpgradePlan();
    }
    new WarnWidget(this, SWT.NONE, displayMessage, planForActions, overageEnabled, canUpgradePlan);
    ensureFooterAtBottom();
    requestLayout();
  }

  /**
   * Create an agent message widget to the turn widget.
   */
  protected void createAgentMessageWidget(CodingAgentMessageRequestParams params) {
    new AgentMessageWidget(this, SWT.BOTTOM, params);
    requestLayout();
  }

  /**
   * Prompts the user to confirm or deny a tool execution.
   *
   * @param content The confirmation content with title, message, and action buttons.
   * @param input The input object to be passed to the tool.
   */
  public CompletableFuture<LanguageModelToolConfirmationResult> requestToolExecutionConfirmation(
      ConfirmationContent content, Object input) {
    reset();

    this.confirmDialog = new InvokeToolConfirmationDialog(this, content, input);
    this.confirmDialog.addDisposeListener(e -> {
      Composite ancestor = this.getParent();
      while (ancestor != null && !ancestor.isDisposed()) {
        if (ancestor instanceof ChatContentViewer viewer) {
          SwtUtils.invokeOnDisplayThreadAsync(() -> viewer.refreshLayoutFull(), viewer);
          break;
        }
        ancestor = ancestor.getParent();
      }
    });
    CompletableFuture<LanguageModelToolConfirmationResult> toolConfirmationFuture = this.confirmDialog
        .getConfirmationFuture();

    this.getParent().requestLayout();

    return toolConfirmationFuture;
  }

  /**
   * Cancels the current tool confirmation dialog programmatically. This has the same effect as clicking the Cancel
   * button in the confirmation dialog.
   */
  public void cancelToolConfirmation() {
    if (this.confirmDialog == null) {
      return;
    }
    this.confirmDialog.cancelConfirmation();
    this.confirmDialog = null;
  }

  /**
   * Dispose the widget.
   */
  @Override
  public void dispose() {
    super.dispose();
    if (messageBuffer != null) {
      messageBuffer.setLength(0);
    }
    if (mdContentBuilder != null) {
      mdContentBuilder.setLength(0);
    }
    if (statusLabels != null) {
      for (AgentStatusLabel label : statusLabels.values()) {
        label.dispose();
      }
      statusLabels.clear();
    }
    if (currentSubagentBlock != null) {
      currentSubagentBlock.dispose();
      currentSubagentBlock = null;
    }
    // TODO: the event broker can be injected once we fully migrated to e4 and use ui injection
    if (this.cancelMsgEventHandler != null) {
      IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
      eventBroker.unsubscribe(this.cancelMsgEventHandler);
      this.cancelMsgEventHandler = null;
    }
  }
}

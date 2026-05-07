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
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GenerateThinkingTitleParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GenerateThinkingTitleResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Thinking;
import com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent.CodingAgentMessageRequestParams;
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
  protected ThinkingBlock currentThinkingBlock;

  // Data
  protected StringBuilder messageBuffer;
  protected StringBuilder mdContentBuilder;
  protected boolean inCodeBlock;
  protected boolean isCopilot;
  protected String turnId;
  protected int codeBlockIndex;
  protected boolean inSubagentBlock;
  protected String overrideRoleName;
  protected StringBuilder thinkingTextBuffer;

  // Resource
  protected Image icon = null;
  protected Font boldFont = null;
  protected InvokeToolConfirmationDialog confirmDialog;

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
    this.thinkingTextBuffer = new StringBuilder();
    this.serviceManager = serviceManager;
    this.isCopilot = isCopilot;
    this.turnId = turnId;
    this.codeBlockIndex = 1;
    this.statusLabels = new HashMap<>();
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
    };
    eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_MESSAGE_CANCELLED, cancelMsgEventHandler);
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
    if (toolCall == null || StringUtils.isEmpty(toolCall.getProgressMessage())) {
      return;
    }

    // Check if this is a run_subagent tool call
    if ("run_subagent".equalsIgnoreCase(toolCall.getName())) {
      handleSubagentToolCall(toolCall);
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

    String status = toolCall.getStatus().toLowerCase();
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
          requestLayout();
        }
        break;
      case "completed":
        // End of subagent block
        if (currentSubagentBlock != null) {
          currentSubagentBlock.notifyTurnEnd();
          inSubagentBlock = false;
          currentSubagentBlock = null;
          requestLayout();
        }
        break;
      case "cancelled":
      case "error":
        // Handle errors in subagent
        if (currentSubagentBlock != null) {
          currentSubagentBlock.notifyTurnEnd();
          inSubagentBlock = false;
          currentSubagentBlock = null;
        }
        // Show error status
        AgentStatusLabel statusLabel = statusLabels.computeIfAbsent(toolCall.getId(),
            id -> new AgentStatusLabel(this, SWT.LEFT));
        if ("cancelled".equals(status)) {
          statusLabel.setCancelledStatus();
          statusLabel.setText(toolCall.getProgressMessage());
        } else {
          statusLabel.setErrorStatus();
          String errorText = StringUtils.isNotEmpty(toolCall.getError()) ? toolCall.getError()
              : toolCall.getProgressMessage();
          statusLabel.setText(errorText);
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
    }
    if (currentTextBlock instanceof ChatMarkupViewer markupViewer) {
      markupViewer.setMarkup(text);
    } else {
      currentTextBlock.setDocument(new Document(text));
    }
  }

  /**
   * Notify the end of the turn.
   */
  public void notifyTurnEnd() {
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

    // Don't reset subagent block state here - it's managed by tool call status.
    // Don't reset the thinking block / buffer here either: a thinking phase spans across
    // tool calls within the same turn, and appendThinking()/sealThinking() manage its lifecycle
    // explicitly (sealing on reply or end-of-turn, starting a fresh block on the next phase).
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
   * Create a warning dialog to the turn widget.
   */
  protected void createWarnDialog(String message, int code) {
    new WarnWidget(this, SWT.BOTTOM, message, code);
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
   * @param title The title of the confirmation dialog.
   * @param message The message to display in the confirmation dialog.
   * @param input The input object to be passed to the tool.
   */
  public CompletableFuture<LanguageModelToolConfirmationResult> requestToolExecutionConfirmation(String title,
      String message, Object input) {
    // process all the messages before showing the confirmation dialog
    reset();

    this.confirmDialog = new InvokeToolConfirmationDialog(this, title, message, input);
    CompletableFuture<LanguageModelToolConfirmationResult> toolConfirmationFuture = this.confirmDialog
        .getConfirmationFuture();

    this.getParent().layout();

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
   * Append a thinking delta from the language server. Routes to the active turn (parent or
   * subagent) and lazily creates the {@link ThinkingBlock} on the first non-empty delta.
   *
   * @param thinking the thinking delta from {@code ChatProgressValue.thinking}
   */
  public void appendThinking(Thinking thinking) {
    if (thinking == null || StringUtils.isBlank(thinking.text())) {
      return;
    }

    SwtUtils.invokeOnDisplayThread(() -> {
      if (isDisposed()) {
        return;
      }
      // Route to subagent if we are inside one.
      if (inSubagentBlock && currentSubagentBlock != null) {
        BaseTurnWidget subagentWidget = currentSubagentBlock.getSubagentTurnWidget();
        if (subagentWidget != null) {
          subagentWidget.appendThinking(thinking);
          return;
        }
      }
      // If the previous block was already sealed (e.g. between agent rounds), start a fresh one
      // so each thinking phase gets its own banner.
      if (currentThinkingBlock != null && !currentThinkingBlock.isDisposed()
          && currentThinkingBlock.isComplete()) {
        currentThinkingBlock = null;
        thinkingTextBuffer.setLength(0);
      }
      if (currentThinkingBlock == null || currentThinkingBlock.isDisposed()) {
        currentThinkingBlock = new ThinkingBlock(this, SWT.NONE);
      }
      // The blank-text guard above guarantees a non-empty fragment here.
      String fragment = thinking.text();
      thinkingTextBuffer.append(fragment);
      currentThinkingBlock.appendText(fragment);
      requestLayout();
    }, this);
  }

  /**
   * Seal the active thinking block: stop the spinner, swap to the completed icon, then
   * asynchronously fetch a server-generated title and update the banner.
   */
  public void sealThinking() {
    SwtUtils.invokeOnDisplayThread(() -> {
      // Route to subagent first if applicable, and stop here so the parent's still-streaming
      // block (if any) is not accidentally sealed by a child seal event.
      if (inSubagentBlock && currentSubagentBlock != null) {
        BaseTurnWidget subagentWidget = currentSubagentBlock.getSubagentTurnWidget();
        if (subagentWidget != null) {
          subagentWidget.sealThinking();
          return;
        }
      }
      if (currentThinkingBlock == null || currentThinkingBlock.isDisposed()
          || currentThinkingBlock.isComplete()) {
        return;
      }
      // Show the fallback title immediately while the LSP request runs.
      currentThinkingBlock.markComplete(null);
      requestLayout();

      String accumulated = thinkingTextBuffer.toString();
      if (StringUtils.isBlank(accumulated)) {
        return;
      }

      // Capture the target block so a late title response is only applied if this exact block is
      // still around (a new thinking phase may have started in between).
      requestThinkingTitle(accumulated, currentThinkingBlock);
    }, this);
  }

  private void requestThinkingTitle(String thinkingContent, ThinkingBlock target) {
    GenerateThinkingTitleParams params = new GenerateThinkingTitleParams(thinkingContent, null);
    // The connection layer already logs and swallows exceptions, returning null on failure;
    // applyThinkingTitle() handles a null response by leaving the fallback title in place.
    CopilotCore.getPlugin().getCopilotLanguageServer().generateThinkingTitle(params)
        .thenAccept(response -> applyThinkingTitle(response, target));
  }

  private void applyThinkingTitle(GenerateThinkingTitleResponse response, ThinkingBlock target) {
    if (response == null || StringUtils.isBlank(response.title())) {
      return;
    }
    SwtUtils.invokeOnDisplayThread(() -> {
      // Only apply the title to the exact block that triggered the request; otherwise a stale
      // response could overwrite a brand-new, still-streaming thinking block.
      if (target == null || target.isDisposed() || !target.isComplete()) {
        return;
      }
      target.setTitle(response.title());
      requestLayout();
    }, this);
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
    if (thinkingTextBuffer != null) {
      thinkingTextBuffer.setLength(0);
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
    currentThinkingBlock = null;
    // TODO: the event broker can be injected once we fully migrated to e4 and use ui injection
    if (this.cancelMsgEventHandler != null) {
      IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
      eventBroker.unsubscribe(this.cancelMsgEventHandler);
      this.cancelMsgEventHandler = null;
    }
  }
}

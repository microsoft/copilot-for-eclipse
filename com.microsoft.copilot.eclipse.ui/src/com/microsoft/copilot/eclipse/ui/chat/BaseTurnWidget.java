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
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
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

  // Data
  protected StringBuilder messageBuffer;
  protected StringBuilder mdContentBuilder;
  protected boolean inCodeBlock;
  protected boolean isCopilot;
  protected String turnId;
  protected int codeBlockIndex;

  // Resource
  protected Image icon = null;
  protected Font boldFont = null;
  protected InvokeToolConfirmationDialog confirmDialog;

  // Event handling
  protected EventHandler cancelMsgEventHandler;

  /**
   * Create the widget.
   *
   * @param parent the parent composite
   * @param style the style
   */
  protected BaseTurnWidget(Composite parent, int style, ChatServiceManager serviceManager, String turnId,
      boolean isCopilot) {
    super(parent, style);
    this.messageBuffer = new StringBuilder();
    this.mdContentBuilder = new StringBuilder();
    this.serviceManager = serviceManager;
    this.isCopilot = isCopilot;
    this.turnId = turnId;
    this.codeBlockIndex = 1;
    this.statusLabels = new HashMap<>();
    this.setBackground(parent.getBackground());
    // editor group
    // align all children vertically
    GridLayout gl = new GridLayout(1, true);
    gl.marginRight = 20;
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

  private void createContent() {
    Composite cmpTitle = new Composite(this, SWT.NONE);
    GridLayout titleLayout = new GridLayout(2, false);
    titleLayout.marginLeft = -2;
    cmpTitle.setLayout(titleLayout);
    cmpTitle.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    cmpTitle.setBackground(this.getBackground());

    AvatarService avatarService = serviceManager.getAvatarService();
    icon = getAvatar(avatarService);
    Label lblAvatar = createAvatarLabel(cmpTitle);
    lblAvatar.setBackground(this.getBackground());
    lblAvatar.setImage(icon);

    Label lblRoleName = new Label(cmpTitle, SWT.NONE);
    lblRoleName.setBackground(this.getBackground());
    String name = getRoleName();
    lblRoleName.setText(name);
    if (this.boldFont == null) {
      this.boldFont = UiUtils.getBoldFont(this.getDisplay(), lblRoleName.getFont());
    }
    lblRoleName.setFont(this.boldFont);
    lblRoleName.addDisposeListener(e -> {
      this.boldFont.dispose();
    });
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

    reset();

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
      default:
        CopilotCore.LOGGER.error(new IllegalStateException("Unknown status: " + status));
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
   * Create a warning dialog to the turn widget.
   */
  protected void createWarnDialog(String message, int code) {
    new WarnWidget(this, SWT.BOTTOM, message, code);
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
    // TODO: the event broker can be injected once we fully migrated to e4 and use ui injection
    if (this.cancelMsgEventHandler != null) {
      IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
      eventBroker.unsubscribe(this.cancelMsgEventHandler);
      this.cancelMsgEventHandler = null;
    }
  }
}

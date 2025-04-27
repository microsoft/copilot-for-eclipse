package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult.ToolConfirmationResult;
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
    if (boldFont == null) {
      boldFont = UiUtils.getBoldFont(this.getDisplay(), lblRoleName.getFont());
    }
    lblRoleName.setFont(boldFont);
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
   * Prompts the user to confirm or deny a tool execution.
   *
   * @param title The title of the confirmation dialog.
   * @param message The message to display in the confirmation dialog.
   */
  public CompletableFuture<LanguageModelToolConfirmationResult> requestToolExecutionConfirmation(String title,
      String message, Object input) {
    // process all the messages before showing the confirmation dialog
    reset();

    Composite widgetParent = new Composite(this, SWT.BORDER | SWT.WRAP);
    widgetParent.setLayout(new GridLayout(1, false));
    widgetParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    // Title of the confirmation dialog
    Label titleLbl = new Label(widgetParent, SWT.LEFT | SWT.WRAP);
    titleLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    titleLbl.setText(title);
    if (boldFont == null) {
      boldFont = UiUtils.getBoldFont(this.getDisplay(), titleLbl.getFont());
    }
    titleLbl.setFont(boldFont);

    // Confirmation message of the confirmation dialog
    Label messageLbl = new Label(widgetParent, SWT.LEFT | SWT.WRAP);
    GridData messageGridData = new GridData(SWT.FILL, SWT.FILL, true, false);
    messageLbl.setLayoutData(messageGridData);
    messageLbl.setText(message);

    // More information about the tool invocation
    if (input != null) {
      // TODO: Improve the logic to show more information about the tool invocation when confirm with users. The
      // following code only works for the run in terminal tool.
      Map<String, Object> inputMap = (Map<String, Object>) input;
      if (inputMap.containsKey("command")) {
        Label commandLbl = new Label(widgetParent, SWT.LEFT | SWT.WRAP);
        commandLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        commandLbl.setText((String) inputMap.get("command"));
        commandLbl.setBackground(getBackground());
      }

      if (inputMap.containsKey("explanation")) {
        Label explanationLbl = new Label(widgetParent, SWT.LEFT | SWT.WRAP);
        explanationLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        explanationLbl.setText((String) inputMap.get("explanation"));
      }
    }

    GridLayout actionLayout = new GridLayout(2, false);
    actionLayout.marginLeft = 0;
    actionLayout.marginRight = 0;
    actionLayout.marginWidth = 0;
    actionLayout.horizontalSpacing = 0;
    Composite actionArea = new Composite(widgetParent, SWT.NONE);
    actionArea.setLayout(actionLayout);
    actionArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    CompletableFuture<LanguageModelToolConfirmationResult> future = new CompletableFuture<>();
    Button continueButton = new Button(actionArea, SWT.PUSH);
    continueButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    continueButton.setText("Continue");
    continueButton.addListener(SWT.Selection, e -> {
      future.complete(new LanguageModelToolConfirmationResult(ToolConfirmationResult.ACCEPT));
      widgetParent.dispose();
      if (this.getParent() != null) {
        this.getParent().layout();
      }
    });

    Button cancelButton = new Button(actionArea, SWT.PUSH);
    cancelButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    cancelButton.setText("Cancel");
    cancelButton.addListener(SWT.Selection, e -> {
      future.complete(new LanguageModelToolConfirmationResult(ToolConfirmationResult.DISMISS));
      widgetParent.dispose();
      if (this.getParent() != null) {
        this.getParent().layout();
      }
    });

    this.getParent().layout();

    return future;
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
    if (boldFont != null) {
      boldFont.dispose();
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
  }
}

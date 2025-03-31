package com.microsoft.copilot.eclipse.ui.chat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.CopilotCore;
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

  // Data
  protected StringBuilder sb;
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
    this.sb = new StringBuilder();
    this.mdContentBuilder = new StringBuilder();
    this.serviceManager = serviceManager;
    this.isCopilot = isCopilot;
    this.turnId = turnId;
    this.codeBlockIndex = 1;
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
    sb.append(message);
    int newlineIndex;
    while ((newlineIndex = sb.indexOf("\n")) != -1) {
      String line = sb.substring(0, newlineIndex + 1);
      sb.delete(0, newlineIndex + 1);
      processMessageLine(line);
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
    if (sb.length() > 0) {
      this.processMessageLine(sb.toString());
      sb.setLength(0);
    }
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
   * Dispose the widget.
   */
  @Override
  public void dispose() {
    super.dispose();
    if (sb != null) {
      sb.setLength(0);
    }
    if (boldFont != null) {
      boldFont.dispose();
    }
    if (mdContentBuilder != null) {
      mdContentBuilder.setLength(0);
    }
  }
}
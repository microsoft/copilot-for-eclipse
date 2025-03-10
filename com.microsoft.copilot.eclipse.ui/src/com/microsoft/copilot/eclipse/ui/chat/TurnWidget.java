package com.microsoft.copilot.eclipse.ui.chat;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.MultipleHyperlinkPresenter;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.mylyn.internal.wikitext.ui.editor.syntax.MarkupHyperlinkDetector;
import org.eclipse.mylyn.internal.wikitext.ui.viewer.AnnotationHyperlinkDetector;
import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.ui.viewer.MarkupViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A custom widget that displays a turn.
 */
public class TurnWidget extends Composite {
  private static final String CODE_BLOCK_ANNOTATION = "```";

  private ChatServiceManager serviceManager;

  // Widgets
  private SourceViewer currentTextBlock;
  private SourceViewerComposite currentCodeBlock;

  // Data
  private StringBuilder sb;
  private StringBuilder mdContentBuilder;
  private boolean inCodeBlock;
  private boolean isCopilot;
  private String turnId;
  private int codeBlockIndex;

  // Resource
  private Image icon = null;
  private Font blodFont = null;

  /**
   * Create the widget.
   *
   * @param parent the parent composite
   * @param style the style
   */
  public TurnWidget(Composite parent, int style, ChatServiceManager serviceManager, String turnId, boolean isCopilot) {
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
    // TODO: add avatar
    Composite cmpTitle = new Composite(this, SWT.NONE);
    GridLayout titleLayout = new GridLayout(2, false);
    titleLayout.marginLeft = -2;
    cmpTitle.setLayout(titleLayout);
    cmpTitle.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    cmpTitle.setBackground(this.getBackground());

    AvatarService avatarService = serviceManager.getAvatarService();
    icon = isCopilot ? avatarService.getAvatarForCopilot() : avatarService.getAvatarForCurrentUser(getDisplay());
    Label lblAvatar = isCopilot ? new Label(cmpTitle, SWT.NONE) : buildAvatarLabel(cmpTitle, SWT.NONE);
    lblAvatar.setBackground(this.getBackground());
    lblAvatar.setImage(icon);

    Label lblRoleName = new Label(cmpTitle, SWT.NONE);
    lblRoleName.setBackground(this.getBackground());
    String name = isCopilot ? Messages.chat_turnWidget_copilot
        : Optional.ofNullable(serviceManager.getAuthStatusManager().getUserName()).filter(s -> !s.isEmpty())
            .orElse(Messages.chat_turnWidget_user);
    lblRoleName.setText(name);
    if (blodFont == null) {
      blodFont = UiUtils.getBoldFont(this.getDisplay(), lblRoleName.getFont());
    }
    lblRoleName.setFont(blodFont);
  }

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
      if (line.startsWith(CODE_BLOCK_ANNOTATION)) {
        if (inCodeBlock) {
          // end of code block
          inCodeBlock = false;
          currentCodeBlock = null;
        } else {
          // start of code block
          inCodeBlock = true;
          mdContentBuilder.setLength(0);
          currentTextBlock = null;
          String language = line.substring(CODE_BLOCK_ANNOTATION.length()).trim();
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
    }, TurnWidget.this);
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
    if (currentTextBlock instanceof MarkupViewer markupViewer) {
      markupViewer.setMarkup(text);
      // reset text presentation to update the style, otherwise the style won't be updated
      markupViewer.setTextPresentation(markupViewer.getTextPresentation());
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
    this.currentCodeBlock = new SourceViewerComposite(this, SWT.BORDER, this.serviceManager, language, turnId,
        this.codeBlockIndex);
    this.codeBlockIndex++;
    this.currentCodeBlock.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    this.currentCodeBlock.layout();
  }

  /**
   * Add a message to the turn.
   *
   * @param message the message
   */
  private void createTextBlock() {
    if (isCopilot) {
      MarkupViewer markupViewer = new MarkupViewer(this, null, SWT.MULTI | SWT.WRAP);
      markupViewer.setMarkupLanguage(new MarkdownLanguage());
      markupViewer.setDisplayImages(false);
      IHyperlinkDetector[] hyperlinkDetectors = { new URLHyperlinkDetector(), new MarkupHyperlinkDetector(),
          new AnnotationHyperlinkDetector() };
      markupViewer.setHyperlinkDetectors(hyperlinkDetectors, SWT.NONE);
      MultipleHyperlinkPresenter hyperlinkPresenter = new MultipleHyperlinkPresenter((RGB) null);
      markupViewer.setHyperlinkPresenter(hyperlinkPresenter);

      this.currentTextBlock = markupViewer;
    } else {
      this.currentTextBlock = new SourceViewer(this, null, SWT.MULTI | SWT.WRAP);
    }
    StyledText styledText = this.currentTextBlock.getTextWidget();
    styledText.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
    styledText.setEditable(false);
    styledText.setBackground(this.getBackground());
  }

  private Label buildAvatarLabel(Composite parent, int style) {
    Label lblAvatar = new Label(parent, SWT.DOUBLE_BUFFERED | style);
    lblAvatar.setBackground(parent.getBackground());

    // Set size based on icon dimensions
    int size = icon.getBounds().width;
    GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
    gridData.widthHint = size;
    gridData.heightHint = size;
    lblAvatar.setLayoutData(gridData);
    lblAvatar.addPaintListener(new PaintListener() {
      @Override
      public void paintControl(PaintEvent e) {
        Image avatar = lblAvatar.getImage();
        if (avatar == null || avatar.isDisposed() || lblAvatar.isDisposed()) {
          return;
        }

        GC gc = e.gc;
        gc.setAdvanced(true);
        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);

        Rectangle bounds = e.gc.getClipping();

        // Clear previous content with background color
        gc.setBackground(lblAvatar.getBackground());
        gc.fillRectangle(bounds);

        // Create circular clipping path for the image
        Rectangle imgBounds = avatar.getBounds();
        int diameter = Math.min(imgBounds.width, imgBounds.height);
        Path path = new Path(getDisplay());
        path.addArc(0, 0, diameter, diameter, 0, 360);

        Color borderColor = getDisplay().getSystemColor(SWT.COLOR_GRAY);
        int borderWidth = 1;

        // Draw the image first
        gc.setClipping(path);
        gc.drawImage(avatar, 0, 0, imgBounds.width, imgBounds.height, borderWidth, borderWidth,
            diameter - (2 * borderWidth), diameter - (2 * borderWidth));

        // Reset clipping to draw the border
        gc.setClipping(bounds);

        // Draw border
        gc.setForeground(borderColor);
        gc.setLineWidth(borderWidth);
        gc.drawOval(0, 0, diameter - 1, diameter - 1);

        path.dispose();
      }
    });

    return lblAvatar;
  }

  /**
   * Dispose the widget.
   */
  @Override
  public void dispose() {
    super.dispose();
    if (sb != null) {
      sb.setLength(0);
    }
    if (blodFont != null) {
      blodFont.dispose();
    }
    if (mdContentBuilder != null) {
      mdContentBuilder.setLength(0);
    }
  }
}
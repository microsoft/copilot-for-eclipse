package com.microsoft.copilot.eclipse.ui.chat;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCodeCopyParams;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.AccessibilityUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.TextMateUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A composite that contains a read-only source viewer with syntax highlighting and action buttons (copy, insert).
 */
public class SourceViewerComposite extends Composite {
  private static final int ACTIONS_PADDING_RIGHT = 10;
  private static final int ACTIONS_PADDING_TOP = 5;
  private ChatServiceManager serviceManager;
  private String language;
  private String turnId;
  private int codeBlockIndex;
  private SourceViewer sourceViewer;
  private Composite actionsComposite;

  private Image copyIcon;
  private Image insertIcon;
  private Runnable fontChangeCallback;

  /**
   * Constructs a new SourceViewerComposite.
   */
  public SourceViewerComposite(Composite parent, int style, ChatServiceManager serviceManager, String language,
      String turnId, int codeBlockIndex) {
    super(parent, style);
    this.serviceManager = serviceManager;
    this.language = language;
    this.turnId = turnId;
    this.codeBlockIndex = codeBlockIndex;
    this.init();
  }

  /**
   * Sets the text to be displayed in the source viewer.
   */
  public void setText(String text) {
    if (sourceViewer.getDocument() == null) {
      sourceViewer.setDocument(new Document(text));
    } else {
      int len = sourceViewer.getDocument().getLength();
      try {
        sourceViewer.getDocument().replace(len, 0, text);
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error(e);
      }
    }
    // update the size for code scroll and text widget
    if (this.sourceViewer.getTextWidget() != null && !this.sourceViewer.getTextWidget().isDisposed()) {
      refreshScrollerLayout();
    }
  }

  private void init() {
    // use null layout here,as for other layout, it will re-sort the components when call layout()
    // which will break the hover behavior of the action buttons (copy)
    this.setLayout(null);

    this.sourceViewer = createSourceViewer();
    this.actionsComposite = createActionsComposite();
    // as we use null layout, need to adjust the size of text widget when resize
    this.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        refreshScrollerLayout();
      }
    });
  }

  private SourceViewer createSourceViewer() {
    SourceViewer viewer = new SourceViewer(this, null, SWT.H_SCROLL);
    viewer.setEditable(false);
    viewer.configure(TextMateUtils.getConfiguration(language));
    viewer.setHoverControlCreator(null);

    StyledText styledText = viewer.getTextWidget();
    styledText.setAlwaysShowScrollBars(false);

    // TODO: Add VerifyKeyListener to listen for copy events
    // See: https://github.com/microsoft/copilot-eclipse/issues/372

    // Show/hide button on mouse enter/exit
    styledText.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        Rectangle textBounds = styledText.getBounds();
        Rectangle actionsBounds = actionsComposite.getBounds();
        actionsComposite.setLocation(textBounds.width - ACTIONS_PADDING_RIGHT - actionsBounds.width,
            ACTIONS_PADDING_TOP);
        actionsComposite.moveAbove(styledText);
        actionsComposite.setVisible(true);
      }

      @Override
      public void mouseExit(MouseEvent e) {
        // if mouse move to copy button, it will also trigger mouse exit, check this case here
        Rectangle buttonBounds = actionsComposite.getBounds();
        Point cursorLocation = new Point(e.x, e.y);
        if (!buttonBounds.contains(cursorLocation)) {
          actionsComposite.moveBelow(styledText);
          actionsComposite.setVisible(false);
        }
      }
    });

    // Register for chat font updates via callback
    fontChangeCallback = () -> {
      if (styledText != null && !styledText.isDisposed()) {
        styledText.setFont(UiUtils.getChatFont());
        refreshScrollerLayout();
      }
    };
    serviceManager.getChatFontService().registerCallback(fontChangeCallback);
    AccessibilityUtils.addFocusBorderToComposite(styledText);

    return viewer;
  }

  private Composite createActionsComposite() {
    final Composite result = new Composite(this, SWT.NONE);
    RowLayout layout = new RowLayout();
    layout.fill = true;
    layout.wrap = false;
    layout.marginLeft = 0;
    layout.marginRight = 0;
    layout.marginTop = 0;
    layout.marginBottom = 0;
    layout.spacing = 3;
    layout.pack = true; // Pack the composite tightly
    result.setLayout(layout);

    this.copyIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY);
    Button copyButton = createActionButton(result, SWT.PUSH | SWT.FLAT, copyIcon, "Copy", "Copy to clipboard");
    copyButton.addListener(SWT.Selection, e -> {
      String content = this.sourceViewer.getDocument().get();
      if (StringUtils.isNotEmpty(content)) {
        SwtUtils.copyToClipboard(result, content);
        notifyCodeCopy(content, ConversationCodeCopyParams.COPY_SOURCE_TOOLBAR);
      }
    });

    ImageDescriptor insertDesc = AbstractUIPlugin.imageDescriptorFromPlugin(UiConstants.WORKBENCH_TEXTEDITOR,
        UiConstants.INSERT_ICON);
    this.insertIcon = Optional.ofNullable(insertDesc).map(desc -> desc.createImage()).orElse(null);
    Image pasteIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_PASTE);
    Image insertButtonIcon = this.insertIcon != null ? this.insertIcon : pasteIcon;
    Button insertButton = createActionButton(result, SWT.PUSH | SWT.FLAT, insertButtonIcon, "Insert",
        "Insert into editor");
    insertButton.addListener(SWT.Selection, this::insert);

    result.setVisible(false);
    result.pack();
    return result;
  }

  private Button createActionButton(Composite parent, int style, Image image, String text, String tooltip) {
    Button result = new Button(parent, style);
    result.setToolTipText(tooltip);
    result.setVisible(true);
    if (image == null) {
      result.setText(text);
    } else {
      result.setImage(image);
    }

    // Compute size based on icon
    Point size = Optional.ofNullable(image).map(icon -> new Point(icon.getBounds().width, icon.getBounds().height))
        .map(iconSize -> new Point(iconSize.x + 4, iconSize.y + 4)) // Add minimal padding
        .orElseGet(() -> result.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    result.setSize(size);
    return result;
  }

  private void refreshScrollerLayout() {
    if (this.sourceViewer == null) {
      return;
    }
    StyledText textWidget = this.sourceViewer.getTextWidget();
    textWidget.getDisplay().asyncExec(() -> {
      if (textWidget == null || textWidget.isDisposed()) {
        return;
      }
      Point size = textWidget.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      Rectangle clientArea = this.getClientArea();
      // remove scroll-bar height
      ScrollBar horizontalBar = textWidget.getHorizontalBar();
      int scrollbarHeight = horizontalBar != null ? horizontalBar.getSize().y : 0;
      int height = size.y - scrollbarHeight;
      // Set bounds on SourceViewer's control (the direct child), not just the textWidget
      this.sourceViewer.getControl().setBounds(0, 0, clientArea.width, height);
      textWidget.redraw();
    });
  }

  private void insert(Event e) {
    String content = this.sourceViewer.getDocument().get();
    if (StringUtils.isNotEmpty(content)) {
      // Get the active editor
      SwtUtils.getActiveEditorPart();
      try {
        IEditorPart editor = SwtUtils.getActiveEditorPart();
        if (editor == null) {
          MessageDialog.openError(getShell(), "Cannot Insert", "No active editor found.");
          return;
        }

        ITextEditor textEditor = null;
        if (editor instanceof ITextEditor) {
          textEditor = (ITextEditor) editor;
        } else {
          textEditor = (ITextEditor) editor.getAdapter(ITextEditor.class);
        }

        if (textEditor != null) {
          insertIntoTextEditor(textEditor, content);
        } else {
          CopilotCore.LOGGER.error(new IllegalStateException("The active editor doesn't support text insertion."));
          MessageDialog.openError(getShell(), "Cannot Insert", "The active editor doesn't support text insertion.");
        }
      } catch (BadLocationException ex) {
        CopilotCore.LOGGER.error("Failed to insert code into editor", ex);
        MessageDialog.openError(getShell(), "Insert Failed",
            "An error occurred while inserting the code: " + ex.getMessage());
      }
    }
  }

  private void insertIntoTextEditor(ITextEditor textEditor, String content) throws BadLocationException {
    IDocumentProvider provider = textEditor.getDocumentProvider();
    IDocument document = Optional.ofNullable(provider).map(p -> p.getDocument(textEditor.getEditorInput()))
        .orElse(null);

    if (document == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("Failed to get the document from the active editor."));
      MessageDialog.openError(getShell(), "Cannot Insert", "Failed to get the document from the active editor.");
      return;
    }

    ITextSelection selection = (ITextSelection) textEditor.getSelectionProvider().getSelection();
    // Insert the content at current position
    int offset = selection.getOffset();
    document.replace(offset, selection.getLength(), content);

    // Set the cursor position after the inserted text
    textEditor.selectAndReveal(offset + content.length(), 0);
  }

  private void notifyCodeCopy(String copiedText, String source) {
    IDocument document = this.sourceViewer.getDocument();
    if (StringUtils.isBlank(this.turnId) || document == null
        || this.serviceManager.getLanguageServerConnection() == null) {
      return;
    }
    ConversationCodeCopyParams params = new ConversationCodeCopyParams(this.turnId, this.codeBlockIndex, source,
        copiedText.length(), document.getLength(), copiedText);
    this.serviceManager.getLanguageServerConnection().codeCopy(params);
  }

  @Override
  public void dispose() {
    super.dispose();
    if (fontChangeCallback != null) {
      serviceManager.getChatFontService().unregisterCallback(fontChangeCallback);
    }
    if (insertIcon != null) {
      insertIcon.dispose();
    }
  }

}

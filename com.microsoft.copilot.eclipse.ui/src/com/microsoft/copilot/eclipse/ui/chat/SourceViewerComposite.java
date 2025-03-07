package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.registry.IGrammarRegistryManager;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
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
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

class SourceViewerComposite extends Composite {
  private static HashMap<String, SourceViewerConfiguration> configurations = new HashMap<>();
  private static final int ACTIONS_PADDING_RIGHT = 10;
  private static final int ACTIONS_PADDING_TOP = 5;
  private ChatServiceManager serviceManager;
  private String language;
  private String turnId;
  private int codeBlockIndex;
  private SourceViewer sourceViewer;
  private Composite actionsComposite;
  private ScrolledComposite codeScroll;

  private Image copyIcon;
  private Image insertIcon;

  public SourceViewerComposite(Composite parent, int style, ChatServiceManager serviceManager, String language,
      String turnId, int codeBlockIndex) {
    super(parent, style);
    this.serviceManager = serviceManager;
    this.language = language;
    this.turnId = turnId;
    this.codeBlockIndex = codeBlockIndex;
    this.init();
  }

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
    this.setBackground(this.getParent().getBackground());

    this.sourceViewer = createSourceViewer();
    this.actionsComposite = createActionsComposite();
    // as we use null layout, need to adjust the size of text widget when resize
    this.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        Rectangle bounds = SourceViewerComposite.this.getClientArea();
        SourceViewerComposite.this.codeScroll.setBounds(0, 0, bounds.width, bounds.height);
        if (actionsComposite.isVisible()) {
          Rectangle scrollBounds = SourceViewerComposite.this.codeScroll.getBounds();
          Rectangle actionsBounds = actionsComposite.getBounds();
          actionsComposite.setLocation(scrollBounds.width - ACTIONS_PADDING_RIGHT - actionsBounds.width,
              ACTIONS_PADDING_TOP);
        }
      }
    });
  }

  private SourceViewer createSourceViewer() {
    this.codeScroll = new ScrolledComposite(this, SWT.H_SCROLL);

    SourceViewer viewer = new SourceViewer(codeScroll, null, SWT.NONE);
    viewer.setEditable(false);
    viewer.configure(getConfiguration(language));
    viewer.setHoverControlCreator(null);
    viewer.getTextWidget().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    this.codeScroll.setExpandHorizontal(true);
    this.codeScroll.setExpandVertical(false);
    this.codeScroll.setContent(viewer.getTextWidget());
    this.codeScroll.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        refreshScrollerLayout();
      }
    });
    GridLayout ly = new GridLayout(1, true);
    ly.marginWidth = 0;
    ly.horizontalSpacing = 0;
    this.codeScroll.setLayout(ly);

    StyledText styledText = viewer.getTextWidget();

    // TODO: Add VerifyKeyListener to listen for copy events
    // See: https://github.com/microsoft/copilot-eclipse/issues/372

    // Show/hide button on mouse enter/exit
    styledText.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        Rectangle scrollBounds = SourceViewerComposite.this.codeScroll.getBounds();
        Rectangle actionsBounds = actionsComposite.getBounds();
        actionsComposite.setLocation(scrollBounds.width - ACTIONS_PADDING_RIGHT - actionsBounds.width,
            ACTIONS_PADDING_TOP);
        actionsComposite.moveAbove(codeScroll);
        actionsComposite.setVisible(true);
      }

      @Override
      public void mouseExit(MouseEvent e) {
        // if mouse move to copy button, it will also trigger mouse exit, check this case here
        Rectangle buttonBounds = actionsComposite.getBounds();
        Point cursorLocation = new Point(e.x, e.y);
        if (!buttonBounds.contains(cursorLocation)) {
          actionsComposite.moveBelow(codeScroll);
          actionsComposite.setVisible(false);
        }
      }
    });
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
    result.setBackground(getBackground());

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
    this.insertIcon = Optional.ofNullable(insertDesc).map(desc -> desc.createImage())
        .orElseGet(() -> PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_PASTE));
    Button insertButton = createActionButton(result, SWT.PUSH | SWT.FLAT, insertIcon, "Insert", "Insert into editor");
    insertButton.addListener(SWT.Selection, this::insert);

    result.setVisible(false);
    result.pack();
    return result;
  }

  private Button createActionButton(Composite parent, int style, Image image, String text, String tooltip) {
    Button result = new Button(parent, style);
    result.setToolTipText(tooltip);
    result.setVisible(true);
    result.setBackground(getBackground());
    if (image == null) {
      result.setText(text);
    } else {
      result.setImage(image);
    }

    // Compute size based on icon
    Point size = Optional.ofNullable(image)
        .map(icon -> new Point(icon.getBounds().width, icon.getBounds().height))
        .map(iconSize -> new Point(iconSize.x + 4, iconSize.y + 4)) // Add minimal padding
        .orElseGet(() -> result.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    result.setSize(size);
    return result;
  }

  private void refreshScrollerLayout() {
    Point size = this.sourceViewer.getTextWidget().computeSize(SWT.DEFAULT, SWT.DEFAULT);
    this.codeScroll.setSize(this.getSize().x, size.y);
    this.codeScroll.setMinSize(size);
    this.sourceViewer.getTextWidget().setSize(size);
    this.codeScroll.layout(true, true);
  }

  private void insert(Event e) {
    String content = this.sourceViewer.getDocument().get();
    if (StringUtils.isNotEmpty(content)) {
      // Get the active editor
      SwtUtils.getActiveEditorPart();
      try {
        IEditorPart editor = SwtUtils.getActiveEditorPart();
        if (editor != null && editor instanceof ITextEditor textEditor) {
          // Get document and selection
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

  private static SourceViewerConfiguration getConfiguration(String lang) {
    if (configurations.containsKey(lang)) {
      SourceViewerConfiguration result = configurations.get(lang);
      IPresentationReconciler reconciler = result.getPresentationReconciler(null);
      if (reconciler instanceof TMPresentationReconciler tmReconciler && tmReconciler.getGrammar() != null) {
        // TMPresentationReconciler will auto update source theme based on eclipse theme,
        // however, the method to check whether is dark theme will always return false when set theme in settings panel
        // https://github.com/eclipse-tm4e/tm4e/blob/main/org.eclipse.tm4e.ui/src/main/java/org/eclipse/tm4e/ui/internal/utils/UI.java#L167-L172
        // so we need to set theme again here
        tmReconciler.setTheme(TMUIPlugin.getThemeManager().getThemeForScope(tmReconciler.getGrammar().getScopeName()));
      }
    }
    TMPresentationReconciler reconciler = new TMPresentationReconciler();
    IGrammarRegistryManager mgr = TMEclipseRegistryPlugin.getGrammarRegistryManager();
    IGrammar grammar = mgr.getGrammarForFileExtension(lang);
    reconciler.setGrammar(grammar);
    if (grammar != null) {
      reconciler.setTheme(TMUIPlugin.getThemeManager().getThemeForScope(grammar.getScopeName()));
    }
    SourceViewerConfiguration ret = new SourceViewerConfiguration() {
      @Override
      public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        return reconciler;
      }
    };
    configurations.put(lang, ret);
    return ret;
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
    if (copyIcon != null) {
      copyIcon.dispose();
    }
    if (insertIcon != null) {
      insertIcon.dispose();
    }
  }

}

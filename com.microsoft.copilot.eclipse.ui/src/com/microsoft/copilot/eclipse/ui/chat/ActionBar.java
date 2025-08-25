package com.microsoft.copilot.eclipse.ui.chat;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.utils.ChatMessageUtils;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.handlers.OpenPreferencesHandler;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A custom widget that displays a turn.
 */
public class ActionBar extends Composite implements NewConversationListener {
  private Button btnMsgToggle;
  private Combo cmbModelPicker;
  private Combo cmbChatModePicker;
  private ChatInputTextViewer inputTextViewer;
  private Composite cmpFileRef;
  private Composite cmpActionArea;
  private Composite bottomRightButtonsComposite;
  private CurrentReferencedFile currentFileRef;
  private ContentAssistant ca;
  private Image sendImage;
  private Image cancelImage;
  private boolean isSendButton = true;
  private LinkedHashSet<MessageListener> messageListeners = new LinkedHashSet<>();
  private Button mcpToolButton;
  private Image mcpToolImage;
  private Image mcpToolDisabledImage;

  private ChatServiceManager chatServiceManager;
  IEventBroker eventBroker;
  EventHandler updateSendButtonToCancelButtonHandler;

  private static enum SendOrCancelButtonStates {
    SEND_ENABLED, SEND_DISABLED, CANCEL_ENABLED;
  }

  /**
   * Creates a new InputArea.
   */
  public ActionBar(Composite parent, int style, ChatServiceManager chatServiceManager) {
    super(parent, style | SWT.BORDER);
    this.chatServiceManager = chatServiceManager;
    this.updateSendButtonToCancelButtonHandler = event -> {
      updateButtonState(SendOrCancelButtonStates.CANCEL_ENABLED);
    };
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_ON_SEND, updateSendButtonToCancelButtonHandler);
    this.setBackground(UiUtils.getThemeColor(UiConstants.EDITOR_BACKGROUND));
    GridLayout gl = new GridLayout(1, false);
    gl.marginHeight = 5;
    gl.verticalSpacing = 0;
    setLayout(gl);
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    RowLayout rowLayout = new RowLayout();
    rowLayout.wrap = true;
    rowLayout.pack = true;
    rowLayout.justify = false;
    rowLayout.type = SWT.HORIZONTAL;
    // marginWidth/marginHeight will not overwrite marginLeft/Right marginTop/Bottom
    // both of them are used to compute size in row layout, so set them separately
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    rowLayout.marginRight = 0;
    rowLayout.marginLeft = 0;
    rowLayout.marginTop = 0;
    rowLayout.marginBottom = 10;
    this.cmpFileRef = new Composite(this, SWT.NONE);
    this.cmpFileRef.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    this.cmpFileRef.setLayout(rowLayout);
    UiUtils.useParentBackground(this.cmpFileRef);
    new AddContextButton(this.cmpFileRef);
    this.currentFileRef = new CurrentReferencedFile(this.cmpFileRef);
    ReferencedFileService referencedFileService = chatServiceManager.getReferencedFileService();
    referencedFileService.bindCurrentFileWidget(currentFileRef);
    referencedFileService.bindReferencedFilesWidget(this);

    UserPreferenceService userPreferenceService = chatServiceManager.getUserPreferenceService();
    userPreferenceService.bindActionBarForSupportVisionChange(this);

    ChatInputTextViewer tv = new ChatInputTextViewer(this, chatServiceManager);
    tv.setEditable(true);
    tv.addTextListener(new ITextListener() {
      @Override
      public void textChanged(TextEvent event) {
        if (!isSendButton) {
          return;
        }
        if (tv.getDocument().get().equals(StringUtils.EMPTY)) {
          updateButtonState(SendOrCancelButtonStates.SEND_DISABLED);
        } else {
          updateButtonState(SendOrCancelButtonStates.SEND_ENABLED);
        }
      }
    });
    tv.setSendMessageHandler((message) -> {
      if (isSendButton) {
        handleSendMessage();
      }
    });
    this.inputTextViewer = tv;

    ca = new ContentAssistant();
    ca.enableAutoActivateCompletionOnType(true);
    ca.enableCompletionProposalTriggerChars(true);
    ca.enableAutoActivation(true);
    ca.setContentAssistProcessor(new ChatAssistProcessor(tv, chatServiceManager), IDocument.DEFAULT_CONTENT_TYPE);
    ca.setProposalPopupOrientation(IContentAssistant.PROPOSAL_STACKED);
    ca.enableColoredLabels(true);
    ca.setAutoActivationDelay(0);
    ca.addCompletionListener(new ICompletionListener() {
      private static final int MAX_VISIBLE_ITEMS = 10; // follow the same behavior of CompletionProposalPopup
      private Map<Table, Listener> tableListeners = new HashMap<>();

      @Override
      public void assistSessionStarted(ContentAssistEvent event) {
      }

      @Override
      public void assistSessionEnded(ContentAssistEvent event) {
      }

      @Override
      public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
        Object proposalPopup = PlatformUtils.getPropertyWithReflection(ca, "fProposalPopup");
        Object popupTable = PlatformUtils.getPropertyWithReflection(proposalPopup, "fProposalTable");
        // get ca.fProposalPopup.fProposalTable using reflection
        if (popupTable != null && popupTable instanceof Table table && table.getLayoutData() instanceof GridData gd) {
          updateTableLayout(table);
          // when selection changed, table did not fill data in mac, which will make the size incorrect
          // use listener to track the set data event, and update layout when data is filled
          Listener listener = tableListeners.computeIfAbsent(table, t -> e -> updateTableLayout(t));
          table.addListener(SWT.SetData, listener);
        }
      }

      private void updateTableLayout(Table table) {
        Point size = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int heightHint = Math.min(size.y, table.getItemHeight() * MAX_VISIBLE_ITEMS);
        int widthHint = Math.min(size.x, tv.getControl().getSize().x);

        // If horizontal scrollbar is needed, add its height to the table height
        // Otherwise, the last raw may not be fully visible
        if (size.x > widthHint) {
          heightHint += table.getHorizontalBar().getSize().y;
        }

        table.getShell().setSize(widthHint, heightHint);
      }
    });
    ca.install(tv);
    tv.setContentAssistProcessor(ca);

    GridLayout glActionArea = new GridLayout(2, false);
    // Same as RowLayout above, need to set marginWidth/Height and marginLeft/Right/Top/Bottom separately in GridLayout
    glActionArea.marginWidth = 0;
    glActionArea.marginHeight = 0;
    glActionArea.marginRight = 0;
    glActionArea.marginLeft = 0;
    glActionArea.marginTop = 5;
    glActionArea.marginBottom = -5;
    this.cmpActionArea = new Composite(this, SWT.NONE);
    this.cmpActionArea.setLayout(glActionArea);
    this.cmpActionArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    UiUtils.useParentBackground(this.cmpActionArea);

    Composite cmpControlBar = new Composite(this.cmpActionArea, SWT.NONE);
    GridLayout glControlBar = new GridLayout(2, false);
    glControlBar.marginWidth = 0;
    glControlBar.marginLeft = 0;
    cmpControlBar.setLayout(glControlBar);
    cmpControlBar.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
    UiUtils.useParentBackground(cmpControlBar);
    setUpChatModePicker(cmpControlBar);
    setUpModelPicker(cmpControlBar);

    // Create a composite for the bottom-right side buttons
    GridLayout buttonsLayout = new GridLayout(2, false);
    buttonsLayout.marginWidth = 0;
    buttonsLayout.marginHeight = 0;
    this.bottomRightButtonsComposite = new Composite(this.cmpActionArea, SWT.NONE);
    this.bottomRightButtonsComposite.setLayout(buttonsLayout);
    this.bottomRightButtonsComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    UiUtils.useParentBackground(this.bottomRightButtonsComposite);

    // Update both MCP button and send button together
    updateButtonsLayout();
  }

  /**
   * Update the referenced file widgets when supportVision changes.
   *
   * @param supportVision true if the current model supports vision, false otherwise
   */
  public void updateReferencedWidgetsWithSupportVision(boolean supportVision) {
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      List<IResource> referencedFiles = chatServiceManager.getReferencedFileService().getReferencedFiles();
      updateReferencedFilesInternal(referencedFiles, supportVision);
    }, this);
  }

  /**
   * Update the referenced file widgets when the file set changes.
   *
   * @param files the list of files to update
   */
  public void updateReferencedWidgetsWithFiles(List<IResource> files) {
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      boolean supportVision = chatServiceManager.getUserPreferenceService().isVisionSupported();
      updateReferencedFilesInternal(files, supportVision);
    }, this);
  }

  /**
   * Update the referenced file widgets with the given files and supportVision flag.
   */
  private void updateReferencedFilesInternal(List<IResource> files, boolean supportVision) {
    if (files == null) {
      return;
    }

    if (this.cmpFileRef == null || this.cmpFileRef.isDisposed()) {
      return;
    }

    // Get parent composite and disable redraw to avoid flickering when references files are updated
    Composite actionBarParent = this.getParent();
    actionBarParent.setRedraw(false);
    
    try {
      for (Control child : cmpFileRef.getChildren()) {
        if (child instanceof ReferencedFile && !(child instanceof CurrentReferencedFile)) {
          child.dispose();
        }
      }

      for (IResource file : files) {
        if (file instanceof IFile) {
          boolean isUnSupportedFile = !supportVision && ChatMessageUtils.isImageFile((IFile) file);
          new ReferencedFile(this.cmpFileRef, file, isUnSupportedFile);
        } else if (file instanceof IFolder) {
          new ReferencedFile(this.cmpFileRef, file, false);
        }
      }
      
    } finally {
      actionBarParent.setRedraw(true);
      refreshLayout();
    }
  }

  /**
   * Refresh the layout of both MCP button and send button together to ensure proper coordination.
   */
  public void updateButtonsLayout() {
    if (this.mcpToolButton != null && !this.mcpToolButton.isDisposed()) {
      this.chatServiceManager.getMcpConfigService().unbindWithMcpToolButton();
      this.mcpToolButton.dispose();
      this.mcpToolButton = null;
    }
    if (mcpToolImage != null && !mcpToolImage.isDisposed()) {
      mcpToolImage.dispose();
      mcpToolImage = null;
    }
    if (mcpToolDisabledImage != null && !mcpToolDisabledImage.isDisposed()) {
      mcpToolDisabledImage.dispose();
      mcpToolDisabledImage = null;
    }
    if (btnMsgToggle != null && !btnMsgToggle.isDisposed()) {
      btnMsgToggle.dispose();
      btnMsgToggle = null;
    }

    // Update the bottom right buttons composite layout based on chat mode
    boolean isAgentMode = chatServiceManager.getUserPreferenceService().getActiveChatMode().equals(ChatMode.Agent);
    GridLayout buttonsLayout = (GridLayout) this.bottomRightButtonsComposite.getLayout();
    buttonsLayout.numColumns = isAgentMode ? 2 : 1;

    // Add MCP button for Agent mode
    if (isAgentMode) {
      mcpToolImage = UiUtils.buildImageFromPngPath("/icons/chat/tools.png");
      mcpToolDisabledImage = UiUtils.buildImageFromPngPath("/icons/chat/tools_disabled.png");
      this.addDisposeListener(e -> {
        if (mcpToolImage != null && !mcpToolImage.isDisposed()) {
          mcpToolImage.dispose();
        }
        if (mcpToolDisabledImage != null && !mcpToolDisabledImage.isDisposed()) {
          mcpToolDisabledImage.dispose();
        }
      });

      this.mcpToolButton = UiUtils.createIconButton(this.bottomRightButtonsComposite, SWT.PUSH | SWT.FLAT);
      this.chatServiceManager.getMcpConfigService().bindWithMcpToolButton(mcpToolButton, mcpToolImage,
          mcpToolDisabledImage);
      GridData mcpToolGd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
      mcpToolGd.widthHint = mcpToolImage.getImageData().width + 2 * UiConstants.BTN_PADDING;
      mcpToolGd.heightHint = mcpToolImage.getImageData().height + 2 * UiConstants.BTN_PADDING;
      this.mcpToolButton.setLayoutData(mcpToolGd);
      this.mcpToolButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if (!CopilotCore.getPlugin().getFeatureFlags().isMcpEnabled()) {
            return;
          }

          Map<String, Object> parameters = new HashMap<>();

          parameters.put("com.microsoft.copilot.eclipse.commands.openPreferences.activePageId",
              OpenPreferencesHandler.mcpPreferencePage);

          parameters.put("com.microsoft.copilot.eclipse.commands.openPreferences.pageIds",
              String.join(",", OpenPreferencesHandler.copilotPreferencesPage,
                  OpenPreferencesHandler.customInstructionsPreferencePage, OpenPreferencesHandler.mcpPreferencePage));

          UiUtils.executeCommandWithParameters("com.microsoft.copilot.eclipse.commands.openPreferences", parameters);
        }
      });
    }

    // Add toggle button for all modes if it has not been created
    if (btnMsgToggle == null || btnMsgToggle.isDisposed()) {
      this.sendImage = UiUtils.buildImageFromPngPath("/icons/chat/send.png");
      this.cancelImage = UiUtils.buildImageFromPngPath("/icons/chat/cancel.png");
      this.btnMsgToggle = UiUtils.createIconButton(bottomRightButtonsComposite, SWT.PUSH | SWT.FLAT);
      this.btnMsgToggle.setEnabled(StringUtils.isBlank(this.inputTextViewer.getContent()) ? false : true);
      this.btnMsgToggle.setImage(this.sendImage);
      this.btnMsgToggle.setToolTipText(Messages.chat_actionBar_sendButton_Tooltip);
      GridData sendGd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
      sendGd.widthHint = this.sendImage.getImageData().width + 2 * UiConstants.BTN_PADDING;
      sendGd.heightHint = this.sendImage.getImageData().height + 2 * UiConstants.BTN_PADDING;
      this.btnMsgToggle.setLayoutData(sendGd);
      this.btnMsgToggle.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
          if (isSendButton) {
            handleSendMessage();
          } else {
            handleCancelMessage();
          }
        }
      });
      this.btnMsgToggle.addDisposeListener(e -> {
        if (sendImage != null && !sendImage.isDisposed()) {
          sendImage.dispose();
        }
        if (cancelImage != null && !cancelImage.isDisposed()) {
          cancelImage.dispose();
        }
      });
    }
    // Refresh the layout
    this.bottomRightButtonsComposite.requestLayout();
  }

  /**
   * Refresh the chat input text viewer.
   */
  public void refreshChatInputTextViewer() {
    if (this.inputTextViewer != null) {
      this.inputTextViewer.refresh();
    }
  }

  private void setUpModelPicker(Composite parent) {
    this.cmbModelPicker = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
    this.cmbModelPicker.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
    UserPreferenceService userPreferenceService = chatServiceManager.getUserPreferenceService();
    userPreferenceService.bindModelPicker(cmbModelPicker);
  }

  private void setUpChatModePicker(Composite parent) {
    this.cmbChatModePicker = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
    this.cmbChatModePicker.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
    UserPreferenceService userPreferenceService = chatServiceManager.getUserPreferenceService();
    userPreferenceService.bindChatModePicker(this.cmbChatModePicker);
    this.cmbChatModePicker.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        int index = cmbChatModePicker.getSelectionIndex();
        userPreferenceService.setActiveChatMode(index);
      }
    });
  }

  @Override
  public void onNewConversation() {
    resetSendButton();
  }

  /**
   * Handles the cancel message event.
   */
  public void resetSendButton() {
    if (this.inputTextViewer.getContent().isEmpty()) {
      updateButtonState(SendOrCancelButtonStates.SEND_DISABLED);
    } else {
      updateButtonState(SendOrCancelButtonStates.SEND_ENABLED);
    }
    this.chatServiceManager.getFileToolService().setFileChangeSummaryBarButtonStatus(true);
  }

  /**
   * Sets the focus to the chat input text viewer.
   *
   * @return true if the focus was set, false otherwise
   */
  public boolean setFocusToInputTextViewer() {
    if (inputTextViewer != null && inputTextViewer.getTextWidget() != null
        && !inputTextViewer.getTextWidget().isDisposed()) {
      return inputTextViewer.getTextWidget().setFocus();
    }
    return false;
  }

  /**
   * Get the content of the input text viewer.
   *
   * @return the current content of the input text viewer
   */
  public String getInputTextViewerContent() {
    if (inputTextViewer != null) {
      return inputTextViewer.getContent();
    }
    return StringUtils.EMPTY;
  }

  /**
   * Set the content of the input text viewer.
   *
   * @param content the content to set
   */
  public void setInputTextViewerContent(String content) {
    if (inputTextViewer != null && content != null) {
      inputTextViewer.setContent(content);
    }
  }

  /**
   * Handles the send message event.
   */
  public void handleSendMessage() {
    updateButtonState(SendOrCancelButtonStates.CANCEL_ENABLED);
    String message = this.inputTextViewer.getContent();
    String workDoneToken = UUID.randomUUID().toString();
    this.inputTextViewer.setContent(StringUtils.EMPTY);
    notifySend(workDoneToken, message);
  }

  private void handleCancelMessage() {
    resetSendButton();
    notifyCancel();
    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    eventBroker.post(CopilotEventConstants.TOPIC_CHAT_MESSAGE_CANCELLED, null);
  }

  private void notifyCancel() {
    for (MessageListener listener : messageListeners) {
      listener.onCancel();
    }
  }

  /**
   * Registers a send message listener.
   *
   * @param listener the listener
   */
  public void registerMessageListener(MessageListener listener) {
    this.messageListeners.add(listener);
  }

  /**
   * Unregisters a send message listener.
   *
   * @param listener the listener
   */
  public void unregisterMessageListener(MessageListener listener) {
    this.messageListeners.remove(listener);
  }

  /**
   * Returns the current action bar conversation state. Return true if the conversation is stand by or cancelled, false
   * otherwise
   */
  public boolean isSendButton() {
    return isSendButton;
  }

  private void updateButtonState(SendOrCancelButtonStates state) {
    switch (state) {
      case SEND_ENABLED:
        isSendButton = true;
        updateSendOrCancelMsgBtn(true, sendImage, Messages.chat_actionBar_sendButton_Tooltip);
        break;
      case SEND_DISABLED:
        isSendButton = true;
        updateSendOrCancelMsgBtn(false, sendImage, Messages.chat_actionBar_sendButton_Tooltip);
        break;
      case CANCEL_ENABLED:
        isSendButton = false;
        updateSendOrCancelMsgBtn(true, cancelImage, Messages.chat_actionBar_cancelButton_Tooltip);
        break;
      default:
        break;
    }
  }

  /**
   * Notifies the send message listeners.
   *
   * @param workDoneToken the work done token
   * @param message the message
   */
  public void notifySend(String workDoneToken, String message) {
    for (MessageListener listener : this.messageListeners) {
      listener.onSend(workDoneToken, message, true);
    }
  }

  private void updateSendOrCancelMsgBtn(boolean enable, Image image, String tooltip) {
    if (btnMsgToggle == null || btnMsgToggle.isDisposed()) {
      return;
    }
    SwtUtils.invokeOnDisplayThread(() -> {
      btnMsgToggle.setEnabled(enable);
      btnMsgToggle.setImage(image);
      btnMsgToggle.setToolTipText(tooltip);
    }, btnMsgToggle);
  }

  /**
   * Popup a file picker dialog to select files. It's guaranteed that the selected files are unique.
   */
  @NonNull
  private List<IFile> selectFile() {
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IContainer container = root.getContainerForLocation(root.getLocation());
    AttachFileSelectionDialog dialog = new AttachFileSelectionDialog(shell, true, container);
    dialog.setTitle(Messages.chat_filePicker_title);
    dialog.setMessage(Messages.chat_filePicker_message);
    List<IFile> result = new ArrayList<>();
    if (dialog.open() == Window.OK) {
      Object[] selectedFiles = dialog.getResult();
      Set<String> selectedFileUris = new HashSet<>();
      for (Object selectedFile : selectedFiles) {
        if (selectedFile instanceof IFile file) {
          URI fileUri = file.getLocationURI();
          if (fileUri != null && selectedFileUris.add(fileUri.toASCIIString())) {
            result.add(file);
          }
        }
      }
      return result;
    }
    return result;
  }

  private void refreshLayout() {
    Composite parent = ActionBar.this.getParent();
    if (parent != null) {
      parent.layout(true, true);
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    ReferencedFile.disposeLabelProvider();
    if (messageListeners != null) {
      messageListeners.clear();
    }
    if (currentFileRef != null) {
      currentFileRef.dispose();
    }
    if (inputTextViewer != null) {
      inputTextViewer.dispose();
    }
    if (eventBroker != null && updateSendButtonToCancelButtonHandler != null) {
      eventBroker.unsubscribe(updateSendButtonToCancelButtonHandler);
      updateSendButtonToCancelButtonHandler = null;
    }
  }
}

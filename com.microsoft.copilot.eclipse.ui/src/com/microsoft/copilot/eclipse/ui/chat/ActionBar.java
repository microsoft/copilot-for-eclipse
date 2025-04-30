package com.microsoft.copilot.eclipse.ui.chat;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
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
import org.eclipse.swt.events.MouseAdapter;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A custom widget that displays a turn.
 */
public class ActionBar extends Composite implements NewConversationListener {
  private Button btnMsgToggle;
  private Combo cmbModelPicker;
  private ChatInputTextViewer inputTextViewer;
  private Composite cmpFileRef;
  private Composite cmpActionArea;
  private CurrentReferencedFile currentFileRef;
  private ContentAssistant ca;
  private Image sendImage;
  private Image cancelImage;
  private Image attachImage;
  private boolean isSendButton = true;
  // The reason that we use map to dedup the context file is that the hashCode() method
  // of the IFile checks the full path, which will fail to dedup when it comes to multi-module
  // project, so we use the URI instead.
  private Map<String, IFile> uriToFile = new LinkedHashMap<>();
  private LinkedHashSet<MessageListener> messageListeners = new LinkedHashSet<>();

  private ChatServiceManager chatServiceManager;

  private static enum SendOrCancelButtonStates {
    SEND_ENABLED, SEND_DISABLED, CANCEL_ENABLED;
  }

  /**
   * Creates a new InputArea.
   */
  public ActionBar(Composite parent, int style, ChatServiceManager chatServiceManager) {
    super(parent, style | SWT.BORDER);
    this.chatServiceManager = chatServiceManager;
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
    new AddContextButton(this.cmpFileRef, this);
    this.currentFileRef = new CurrentReferencedFile(this.cmpFileRef);
    ReferencedFileService referencedFileService = chatServiceManager.getReferencedFileService();
    referencedFileService.bindCurrentFileWidget(currentFileRef);

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
    ca.setContentAssistProcessor(new SlashCommandAssistProcessor(tv, chatServiceManager),
        IDocument.DEFAULT_CONTENT_TYPE);
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
          gd.horizontalIndent = 3;
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
        table.getShell().setSize(widthHint + 3, heightHint);
      }

    });
    ca.install(tv);

    GridLayout glActionArea = new GridLayout(2, false);
    // Same as RowLayout above, need to set marginWidth/Height and marginLeft/Right/Top/Bottom separately in GridLayout
    glActionArea.marginWidth = 0;
    glActionArea.marginHeight = 0;
    glActionArea.marginRight = 0;
    glActionArea.marginLeft = 0;
    glActionArea.marginTop = 5;
    glActionArea.marginBottom = 0;
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

    sendImage = UiUtils.buildImageFromPngPath("/icons/chat/send.png");
    cancelImage = UiUtils.buildImageFromPngPath("/icons/chat/cancel.png");
    this.btnMsgToggle = UiUtils.createIconButton(cmpActionArea, SWT.PUSH | SWT.FLAT);
    this.btnMsgToggle.setEnabled(false);
    this.btnMsgToggle.setImage(sendImage);
    this.btnMsgToggle.setToolTipText(Messages.chat_actionBar_sendButton_Tooltip);
    GridData sendGd = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
    sendGd.widthHint = sendImage.getImageData().width + 2 * UiConstants.BTN_PADDING;
    sendGd.heightHint = sendImage.getImageData().height + 2 * UiConstants.BTN_PADDING;
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
  }

  /**
   * Handles the add context button click event. This is temporary solution. Should be removed once referenced file
   * service is implemented.
   */
  public void onAddContextClicked() {
    List<IFile> files = selectFile();
    if (files.isEmpty()) {
      return;
    }
    // selectFile makes sure the file doesn't duplicate
    for (IFile file : files) {
      URI fileUri = file.getLocationURI();
      // skip if the file is already in the list, note that for now we won't check the
      // duplication with the current file, which is the same behavior as vscode.
      if (fileUri == null || uriToFile.containsKey(fileUri.toASCIIString())) {
        continue;
      }
      ReferencedFile fileRef = new ReferencedFile(ActionBar.this.cmpFileRef, file);
      fileRef.setCloseClickAction(new MouseAdapter() {
        @Override
        public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
          ActionBar.this.uriToFile.remove(fileUri.toASCIIString());
          fileRef.dispose();
          refreshLayout();
        }
      });
      uriToFile.put(fileUri.toASCIIString(), file);
    }
    refreshLayout();
  }

  private void setUpModelPicker(Composite parent) {
    this.cmbModelPicker = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
    this.cmbModelPicker.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
    UserPreferenceService copilotModelService = chatServiceManager.getUserPreferenceService();
    copilotModelService.bindModelPicker(cmbModelPicker);
    this.cmbModelPicker.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String[] models = cmbModelPicker.getItems();
        int index = cmbModelPicker.getSelectionIndex();
        if (index >= 0 && index < models.length) {
          copilotModelService.setActiveModel(models[index]);
        }
      }
    });
  }

  private void setUpChatModePicker(Composite parent) {
    Combo cmbChatModePicker = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
    cmbChatModePicker.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
    UserPreferenceService userPreferenceService = chatServiceManager.getUserPreferenceService();
    userPreferenceService.bindChatModePicker(cmbChatModePicker);
    cmbChatModePicker.addSelectionListener(new SelectionAdapter() {
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
    this.chatServiceManager.getEditFileToolService().setFileChangeSummaryBarButtonStatus(true);
  }

  private void handleSendMessage() {
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
    List<IFile> allFiles = new ArrayList<>(this.uriToFile.values());
    for (MessageListener listener : this.messageListeners) {
      listener.onSend(workDoneToken, message, new ArrayList<>(allFiles));
    }
  }

  private void updateSendOrCancelMsgBtn(boolean enable, Image image, String tooltip) {
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
    if (sendImage != null && !sendImage.isDisposed()) {
      sendImage.dispose();
    }
    if (cancelImage != null && !cancelImage.isDisposed()) {
      cancelImage.dispose();
    }
    if (uriToFile != null) {
      uriToFile.clear();
    }
    if (attachImage != null) {
      attachImage.dispose();
    }
    if (messageListeners != null) {
      messageListeners.clear();
    }
    if (currentFileRef != null) {
      currentFileRef.dispose();
    }
    if (inputTextViewer != null) {
      inputTextViewer.dispose();
    }
  }
}

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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
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
import org.eclipse.swt.custom.StyledText;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatMode;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatModeManager;
import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;
import com.microsoft.copilot.eclipse.core.chat.CustomChatModeManager;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.utils.ChatMessageUtils;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.core.utils.WorkspaceUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.ModelService;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.dialogs.jobs.GitHubCodingAgentDialog;
import com.microsoft.copilot.eclipse.ui.dialogs.jobs.ProjectSelectionDialog;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.AccessibilityUtils;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;
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
  private Image sendDisabledImage;
  private boolean isSendButton = true;
  private LinkedHashSet<MessageListener> messageListeners = new LinkedHashSet<>();
  private Button mcpToolButton;
  private Image mcpToolImage;
  private Image mcpToolDisabledImage;
  private Image mcpToolDetectedImage;
  private Image redNoticeImage;
  private Button sendToJobButton;
  private Image sendToJobImage;
  private Image sendToJobDisabledImage;

  private ChatServiceManager chatServiceManager;
  IEventBroker eventBroker;
  EventHandler updateSendButtonToCancelButtonHandler;
  EventHandler featureFlagsChangedEventHandler;
  EventHandler updateMcpToolButtonAndPlaceHolderHandler;

  private static enum SendOrCancelButtonStates {
    SEND_ENABLED, SEND_DISABLED, CANCEL_ENABLED;
  }

  /**
   * Creates a new InputArea.
   */
  public ActionBar(Composite parent, int style, ChatServiceManager chatServiceManager) {
    super(parent, SWT.NONE);
    GridLayout glContainer = new GridLayout(1, false);
    glContainer.marginWidth = 10;
    glContainer.marginHeight = 0;
    glContainer.verticalSpacing = 0;
    this.setLayout(glContainer);
    this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    this.setData(CssConstants.CSS_ID_KEY, "chat-action-bar-wrapper");
    this.chatServiceManager = chatServiceManager;
    this.updateSendButtonToCancelButtonHandler = event -> {
      updateButtonState(SendOrCancelButtonStates.CANCEL_ENABLED);
    };
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_ON_SEND, updateSendButtonToCancelButtonHandler);

    this.updateMcpToolButtonAndPlaceHolderHandler = event -> {
      updateMcpToolButtonVisibility();
      refreshPlaceholder();
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_MODE_CHANGED, updateMcpToolButtonAndPlaceHolderHandler);

    this.featureFlagsChangedEventHandler = event -> {
      // Update buttons layout when feature flags change
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        updateButtonsLayout();
      }, this);
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_DID_CHANGE_FEATURE_FLAGS,
        featureFlagsChangedEventHandler);
    Composite actionBar = new Composite(this, style | SWT.BORDER);
    GridLayout gl = new GridLayout(1, false);
    gl.marginHeight = 5;
    gl.verticalSpacing = 0;
    actionBar.setLayout(gl);
    actionBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    actionBar.setData(CssConstants.CSS_ID_KEY, "chat-action-bar");

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
    this.cmpFileRef = new Composite(actionBar, SWT.NONE);
    this.cmpFileRef.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    this.cmpFileRef.setLayout(rowLayout);
    new AddContextButton(this.cmpFileRef);
    this.currentFileRef = new CurrentReferencedFile(this.cmpFileRef);
    ReferencedFileService referencedFileService = chatServiceManager.getReferencedFileService();
    referencedFileService.bindCurrentFileWidget(currentFileRef);
    referencedFileService.bindReferencedFilesWidget(this);

    ModelService modelService = chatServiceManager.getModelService();
    modelService.bindActionBarForSupportVisionChange(this);

    ChatInputTextViewer tv = new ChatInputTextViewer(actionBar, chatServiceManager);
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
    AccessibilityUtils.addAccessibilityNameForUiComponent(tv.getTextWidget(), "ask copilot input text area");

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
        if (popupTable != null && popupTable instanceof Table table && table.getLayoutData() instanceof GridData) {
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
    this.cmpActionArea = new Composite(actionBar, SWT.NONE);
    this.cmpActionArea.setLayout(glActionArea);
    this.cmpActionArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    Composite cmpControlBar = new Composite(this.cmpActionArea, SWT.NONE);
    GridLayout glControlBar = new GridLayout(3, false);
    glControlBar.marginWidth = 0;
    glControlBar.marginLeft = 0;
    cmpControlBar.setLayout(glControlBar);
    cmpControlBar.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
    setUpChatModePicker(cmpControlBar);
    setUpModelPicker(cmpControlBar);
    setUpMcpToolButtonInControlBar(cmpControlBar);

    // Create a composite for the bottom-right side buttons
    GridLayout buttonsLayout = new GridLayout(2, false);
    buttonsLayout.marginWidth = 0;
    buttonsLayout.marginHeight = 0;
    this.bottomRightButtonsComposite = new Composite(this.cmpActionArea, SWT.NONE);
    this.bottomRightButtonsComposite.setLayout(buttonsLayout);
    this.bottomRightButtonsComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    // Update send to job button and send button together
    updateButtonsLayout();

    this.addDisposeListener(e -> {
      if (mcpToolImage != null && !mcpToolImage.isDisposed()) {
        mcpToolImage.dispose();
      }
      if (mcpToolDisabledImage != null && !mcpToolDisabledImage.isDisposed()) {
        mcpToolDisabledImage.dispose();
      }
      if (mcpToolDetectedImage != null && !mcpToolDetectedImage.isDisposed()) {
        mcpToolDetectedImage.dispose();
      }
      if (redNoticeImage != null && !redNoticeImage.isDisposed()) {
        redNoticeImage.dispose();
      }
    });
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
      boolean supportVision = chatServiceManager.getModelService().isVisionSupported();
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
   * Refresh the layout of both sendToJob button and send button together to ensure proper coordination.
   */
  public void updateButtonsLayout() {
    if (sendToJobButton != null && !sendToJobButton.isDisposed()) {
      sendToJobButton.dispose();
      sendToJobButton = null;
    }
    if (btnMsgToggle != null && !btnMsgToggle.isDisposed()) {
      btnMsgToggle.dispose();
      btnMsgToggle = null;
    }

    // Check if client preview features are enabled
    boolean clientPreviewFeaturesEnabled = CopilotCore.getPlugin().getFeatureFlags().isClientPreviewFeatureEnabled();

    // Bottom right buttons composite: 2 columns if sendToJob is visible, 1 column otherwise
    GridLayout buttonsLayout = (GridLayout) this.bottomRightButtonsComposite.getLayout();
    buttonsLayout.numColumns = clientPreviewFeaturesEnabled ? 2 : 1;

    // Add sendToJob button - only visible if clientPreviewFeaturesEnabled
    if (clientPreviewFeaturesEnabled) {
      if (sendToJobImage == null || sendToJobImage.isDisposed()) {
        sendToJobImage = UiUtils.buildImageFromPngPath("/icons/chat/send_to_job.png");
      }
      if (sendToJobDisabledImage == null || sendToJobDisabledImage.isDisposed()) {
        sendToJobDisabledImage = UiUtils.buildImageFromPngPath("/icons/chat/send_to_job_disabled.png");
      }

      this.sendToJobButton = UiUtils.createIconButton(this.bottomRightButtonsComposite, SWT.PUSH | SWT.FLAT);

      boolean hasText = !StringUtils.isBlank(this.inputTextViewer.getContent());
      this.sendToJobButton.setEnabled(hasText);
      this.sendToJobButton.setImage(hasText ? sendToJobImage : sendToJobDisabledImage);
      this.sendToJobButton.setToolTipText(Messages.chat_actionBar_sendToJobButton_Tooltip);
      GridData sendToJobGd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
      sendToJobGd.widthHint = sendToJobImage.getImageData().width + 2 * UiConstants.BTN_PADDING;
      sendToJobGd.heightHint = sendToJobImage.getImageData().height + 2 * UiConstants.BTN_PADDING;
      this.sendToJobButton.setLayoutData(sendToJobGd);
      this.sendToJobButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          handleSendToJob();
          setFocusToInputTextViewer();
        }
      });
    }

    // Add toggle button for all modes if it has not been created
    if (btnMsgToggle == null || btnMsgToggle.isDisposed()) {
      this.sendImage = UiUtils.buildImageFromPngPath("/icons/chat/send.png");
      this.sendDisabledImage = UiUtils.buildImageFromPngPath("/icons/chat/send_disabled.png");
      this.btnMsgToggle = UiUtils.createIconButton(bottomRightButtonsComposite, SWT.PUSH | SWT.FLAT);
      boolean isEnabled = !StringUtils.isBlank(this.inputTextViewer.getContent());
      this.btnMsgToggle.setEnabled(isEnabled);
      this.btnMsgToggle.setImage(isEnabled ? this.sendImage : this.sendDisabledImage);
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
        if (sendDisabledImage != null && !sendDisabledImage.isDisposed()) {
          sendDisabledImage.dispose();
        }
      });
      AccessibilityUtils.addAccessibilityNameForUiComponent(this.btnMsgToggle,
          Messages.chat_actionBar_sendButton_Tooltip);
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
    AccessibilityUtils.addAccessibilityNameForUiComponent(cmbModelPicker, "model picker");
    ModelService modelService = chatServiceManager.getModelService();
    modelService.bindModelPicker(cmbModelPicker);
    UiUtils.addFocusBorderToComposite(cmbModelPicker);
  }

  private void setUpChatModePicker(Composite parent) {
    this.cmbChatModePicker = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
    this.cmbChatModePicker.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
    UserPreferenceService userPreferenceService = chatServiceManager.getUserPreferenceService();
    userPreferenceService.bindChatModePicker(this.cmbChatModePicker);

    // Add a listener to reload modes when dropdown is about to be shown
    this.cmbChatModePicker.addListener(SWT.MouseDown, event -> {
      userPreferenceService.reloadChatModes();
    });

    this.cmbChatModePicker.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        int index = cmbChatModePicker.getSelectionIndex();
        userPreferenceService.setActiveChatMode(index, cmbChatModePicker);
        updateMcpToolButtonVisibility();
      }
    });
    AccessibilityUtils.addAccessibilityNameForUiComponent(cmbChatModePicker, "chat mode picker");
    UiUtils.addFocusBorderToComposite(cmbChatModePicker);
  }

  private void setUpMcpToolButtonInControlBar(Composite parent) {
    if (mcpToolImage == null || mcpToolImage.isDisposed()) {
      mcpToolImage = UiUtils.buildImageFromPngPath("/icons/chat/tools.png");
    }
    if (mcpToolDisabledImage == null || mcpToolDisabledImage.isDisposed()) {
      mcpToolDisabledImage = UiUtils.buildImageFromPngPath("/icons/chat/tools_disabled.png");
    }
    if (mcpToolDetectedImage == null || mcpToolDetectedImage.isDisposed()) {
      mcpToolDetectedImage = UiUtils.buildImageFromPngPath("/icons/chat/tools_detected.png");
    }

    this.mcpToolButton = UiUtils.createIconButton(parent, SWT.PUSH | SWT.FLAT);
    this.chatServiceManager.getMcpConfigService().bindWithMcpToolButton(mcpToolButton, mcpToolImage,
        mcpToolDisabledImage, mcpToolDetectedImage);

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

        // Check if new MCP registrations are found
        if (chatServiceManager.getMcpConfigService().isNewExtMcpRegFound()) {
          showMcpToolContextMenu();
        } else {
          // Default behavior - open preference page directly
          openMcpPreferences();
        }

        // set focus back to input text viewer after handling MCP button click
        setFocusToInputTextViewer();
      }
    });

    // Set initial visibility based on chat mode
    updateMcpToolButtonVisibility();
  }

  //@formatter:off
  /**
   * Update MCP tool button visibility based on whether the current mode allows tool configuration.
   * Shows the tool button only when the mode allows tool configuration:
   * - Agent mode: Shows tool button
   * - Plan mode: Hides tool button (Plan uses Agent UI but no tools)
   * - Ask mode: Hides tool button
   * - Custom modes: Shows tool button (all custom modes allow tools)
   */
  //@formatter:on
  public void updateMcpToolButtonVisibility() {
    if (mcpToolButton == null || mcpToolButton.isDisposed()) {
      return;
    }

    boolean allowsToolConfiguration = false;

    // Get the active mode name or ID from UserPreferenceService
    String activeModeNameOrId = chatServiceManager.getUserPreferenceService().getActiveModeNameOrId();

    if (activeModeNameOrId != null) {
      // First check if it's a built-in mode
      BuiltInChatMode builtInMode = BuiltInChatModeManager.INSTANCE.getBuiltInModeByDisplayName(activeModeNameOrId);
      if (builtInMode != null) {
        // Use the allowsToolConfiguration() method from the built-in mode
        allowsToolConfiguration = builtInMode.allowsToolConfiguration();
      } else {
        // Check if it's a custom mode
        CustomChatMode customMode = CustomChatModeManager.INSTANCE.getCustomModeById(activeModeNameOrId);
        if (customMode != null) {
          // Custom modes always allow tool configuration
          allowsToolConfiguration = customMode.allowsToolConfiguration();
        } else {
          // Fallback: Check if current ChatMode enum equals Agent (backward compatibility)
          ChatMode activeChatMode = chatServiceManager.getUserPreferenceService().getActiveChatMode();
          allowsToolConfiguration = ChatMode.Agent.equals(activeChatMode);
        }
      }
    } else {
      // Fallback when activeModeNameOrId is null
      ChatMode activeChatMode = chatServiceManager.getUserPreferenceService().getActiveChatMode();
      allowsToolConfiguration = ChatMode.Agent.equals(activeChatMode);
    }

    mcpToolButton.setVisible(allowsToolConfiguration);
    ((GridData) mcpToolButton.getLayoutData()).exclude = !allowsToolConfiguration;
    mcpToolButton.requestLayout();
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
    if (inputTextViewer == null) {
      return false;
    }

    StyledText textWidget = inputTextViewer.getTextWidget();
    if (textWidget != null && !textWidget.isDisposed()) {
      textWidget.setSelection(inputTextViewer.getContent().length());
      return textWidget.setFocus();
    }
    return false;
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

  /**
   * Handles the send to job button click event. Shows a dialog to inform user about git repository requirement.
   */
  private void handleSendToJob() {
    ChatServiceManager chatServiceManager = (ChatServiceManager) CopilotCore.getPlugin().getChatServiceManager();
    if (chatServiceManager != null) {
      UserPreferenceService userPreferenceService = chatServiceManager.getUserPreferenceService();
      if (userPreferenceService != null) {
        String selectedProjectPath = null;
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        // Only proceed if a project was selected when multiple projects are present or user confirmed the Copilot agent
        // job when single project
        List<IProject> topLevelGitProjects = WorkspaceUtils.listTopLevelProjectsWithGitRepository();
        if (topLevelGitProjects.isEmpty()) {
          MessageDialog.openInformation(shell,
              com.microsoft.copilot.eclipse.ui.dialogs.Messages.githubCodingAgentDialog_title,
              Messages.chat_actionBar_sendToJob_noProject);
        } else if (topLevelGitProjects.size() > 1) {
          selectedProjectPath = ProjectSelectionDialog.open(shell);
        } else if (userPreferenceService.isSkipGitHubJobConfirmDialog() || GitHubCodingAgentDialog.open(shell)) {
          selectedProjectPath = FileUtils.getResourceUri(topLevelGitProjects.get(0));
        }

        // Only proceed if a project path was selected (dialog was not cancelled)
        if (selectedProjectPath != null) {
          updateButtonState(SendOrCancelButtonStates.CANCEL_ENABLED);
          String message = this.inputTextViewer.getContent();
          String workDoneToken = UUID.randomUUID().toString();
          this.inputTextViewer.setContent(StringUtils.EMPTY);
          notifySendWithSlug(workDoneToken, message, UiConstants.GITHUB_COPILOT_CODING_AGENT_SLUG, selectedProjectPath);
        } else {
          // Dialog was cancelled, reset button state based on current input content
          if (this.inputTextViewer.getContent().isEmpty()) {
            updateButtonState(SendOrCancelButtonStates.SEND_DISABLED);
          } else {
            updateButtonState(SendOrCancelButtonStates.SEND_ENABLED);
          }
        }
      }
    }

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
        updateSendToJobBtn(true);
        break;
      case SEND_DISABLED:
        isSendButton = true;
        updateSendOrCancelMsgBtn(false, sendDisabledImage, Messages.chat_actionBar_sendButton_Tooltip);
        updateSendToJobBtn(false);
        break;
      case CANCEL_ENABLED:
        isSendButton = false;
        Image cancelImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_STOP);
        updateSendOrCancelMsgBtn(true, cancelImage, Messages.chat_actionBar_cancelButton_Tooltip);
        updateSendToJobBtn(false);
        break;
      default:
        break;
    }
  }

  /**
   * Refreshes the placeholder text in the chat input text viewer.
   */
  public void refreshPlaceholder() {
    if (inputTextViewer != null && !inputTextViewer.getTextWidget().isDisposed()) {
      inputTextViewer.getTextWidget().redraw();
    }
  }

  /**
   * Notifies the send message listeners.
   *
   * @param workDoneToken the work done token
   * @param message the message
   */
  public void notifySend(String workDoneToken, String message) {
    Map<String, Object> properties = Map.of("workDoneToken", workDoneToken, "message", message, "createNewTurn", true);
    this.eventBroker.post(CopilotEventConstants.TOPIC_CHAT_MESSAGE_SEND, properties);
  }

  /**
   * Notifies the send message listeners with a specific agent slug.
   *
   * @param workDoneToken the work done token
   * @param message the message
   * @param agentSlug the agent slug to use for this turn
   * @param agentJobWorkspaceFolder the workspace folder for the agent job
   */
  public void notifySendWithSlug(String workDoneToken, String message, String agentSlug,
      String agentJobWorkspaceFolder) {
    Map<String, Object> properties = Map.of("workDoneToken", workDoneToken, "message", message, "agentSlug", agentSlug,
        "agentJobWorkspaceFolder", agentJobWorkspaceFolder, "createNewTurn", true);
    this.eventBroker.post(CopilotEventConstants.TOPIC_CHAT_MESSAGE_SEND, properties);
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

  private void updateSendToJobBtn(boolean enable) {
    if (sendToJobButton == null || sendToJobButton.isDisposed()) {
      return;
    }
    SwtUtils.invokeOnDisplayThread(() -> {
      sendToJobButton.setEnabled(enable);
      sendToJobButton.setImage(enable ? sendToJobImage : sendToJobDisabledImage);
    }, sendToJobButton);
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
    if (eventBroker != null && updateSendButtonToCancelButtonHandler != null) {
      eventBroker.unsubscribe(updateSendButtonToCancelButtonHandler);
      updateSendButtonToCancelButtonHandler = null;
    }
    if (eventBroker != null && featureFlagsChangedEventHandler != null) {
      eventBroker.unsubscribe(featureFlagsChangedEventHandler);
      featureFlagsChangedEventHandler = null;
    }
    if (eventBroker != null && updateMcpToolButtonAndPlaceHolderHandler != null) {
      eventBroker.unsubscribe(updateMcpToolButtonAndPlaceHolderHandler);
      updateMcpToolButtonAndPlaceHolderHandler = null;
    }
  }

  /**
   * Shows a context menu for the MCP tool button when new registrations are found.
   */
  private void showMcpToolContextMenu() {
    Menu contextMenu = new Menu(mcpToolButton);

    // First menu item
    MenuItem approvalItem = new MenuItem(contextMenu, SWT.NONE);
    approvalItem.setText(Messages.chat_actionBar_toolButton_detected_toolTip);
    if (redNoticeImage == null || redNoticeImage.isDisposed()) {
      redNoticeImage = UiUtils.buildImageFromPngPath("/icons/chat/red_notice.png");
    }
    approvalItem.setImage(redNoticeImage);
    approvalItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String res = chatServiceManager.getMcpExtensionPointManager().approveExtMcpRegistration();
        if (StringUtils.isNotBlank(res)) {
          CopilotUi.getPlugin().getLanguageServerSettingManager().syncMcpRegistrationConfiguration();
        }
      }
    });

    // Second menu item
    MenuItem configureItem = new MenuItem(contextMenu, SWT.NONE);
    configureItem.setText(Messages.chat_actionBar_toolButton_toolTip);
    configureItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        openMcpPreferences();
      }
    });

    // Show the menu at the button location
    Point buttonLocation = mcpToolButton.toDisplay(0, mcpToolButton.getSize().y);
    contextMenu.setLocation(buttonLocation);
    contextMenu.setVisible(true);
  }

  /**
   * Opens the MCP preferences page.
   */
  private void openMcpPreferences() {
    // Get the current chat mode name/ID from observable
    String currentModeId = chatServiceManager.getUserPreferenceService().getActiveModeNameOrId();

    // Open MCP preference page with the current mode selected
    PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), McpPreferencePage.ID,
        PreferencesUtils.getAllPreferenceIds(), currentModeId // Pass the current mode ID as data
    );

    dialog.open();
  }
}

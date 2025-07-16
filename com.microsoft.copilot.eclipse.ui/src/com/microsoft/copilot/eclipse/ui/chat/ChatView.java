package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ChatEventsManager;
import com.microsoft.copilot.eclipse.core.chat.ChatProgressListener;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatCreateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatStep;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatStepStatus;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatStepTitles;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatTurnResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatCompletionService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.viewers.AfterLoginWelcomeViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.AgentModeViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.BeforeLoginWelcomeViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.LoadingViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.NoSubscriptionViewer;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * A view that displays chat messages.
 */
public class ChatView extends ViewPart implements ChatProgressListener, MessageListener, NewConversationListener {
  // service
  private ChatServiceManager chatServiceManager;

  private Composite parent;
  private TopBanner topBanner;
  private Composite contentWrapper;
  private Composite mainSection;
  private ActionBar actionBar;
  private ChatContentViewer chatContentViewer;
  private Composite loadingViewer;
  private Composite noSubscriptionViewer;
  private Composite beforeLoginWelcomeViewer;
  private Composite afterLoginWelcomeViewer;
  private Composite agentModeViewer;
  private boolean hasHistory = false;
  private String conversationId = "";
  private Set<CompletableFuture<?>> conversationFutures = new HashSet<>();
  private IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);

  @Override
  public void createPartControl(Composite parent) {
    this.parent = parent;
    GridLayout layout = new GridLayout(1, true);
    layout.verticalSpacing = 0;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    parent.setLayout(layout);
    parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    this.chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
    if (this.chatServiceManager == null) {
      parent.layout();

      // if chat service manager is not ready, wait for the init job to finish
      JobChangeAdapter adapter = new JobChangeAdapter() {
        @Override
        public void done(IJobChangeEvent event) {
          if (!event.getJob().belongsTo(CopilotUi.INIT_JOB_FAMILY)) {
            return;
          }
          ChatView.this.chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
          if (ChatView.this.chatServiceManager == null) {
            CopilotCore.LOGGER.error(new IllegalStateException("Chat service manager is not ready."));
            return;
          }
          ChatView.this.chatServiceManager.getAuthStatusService().bindChatView(ChatView.this);
          ChatView.this.chatServiceManager.getUserPreferenceService().bindChatView(ChatView.this);
          ChatView.this.chatServiceManager.getAgentToolService().bindChatView(ChatView.this);
          ChatView.this.chatServiceManager.getFileToolService().bindFileChangeSummaryBar(ChatView.this);
          Job.getJobManager().removeJobChangeListener(this);
        }
      };
      Job.getJobManager().addJobChangeListener(adapter);
    } else {
      this.chatServiceManager.getAuthStatusService().bindChatView(this);
      this.chatServiceManager.getUserPreferenceService().bindChatView(this);
      this.chatServiceManager.getAgentToolService().bindChatView(this);
      this.chatServiceManager.getFileToolService().bindFileChangeSummaryBar(ChatView.this);
    }

    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_ON_SEND, event -> {
      Object params = event.getProperty(IEventBroker.DATA);
      if (params != null && params instanceof Map properties) {
        String workDoneToken = UUID.randomUUID().toString();
        String previousInput = (String) properties.get("previousInput");
        boolean needCreateUserTurn = (boolean) properties.get("needCreateUserTurn");
        onSend(workDoneToken, previousInput, needCreateUserTurn);
      }
    });
  }

  /**
   * Build the view for the given status.
   *
   * @param status the status
   */
  public void buildViewFor(String status) {
    this.hasHistory = false;
    disposeChildren(parent);
    switch (status) {
      case CopilotStatusResult.LOADING:
        showLoadingPage();
        break;
      case CopilotStatusResult.NOT_SIGNED_IN:
        showBeforeLoginPage();
        break;
      case CopilotStatusResult.NOT_AUTHORIZED:
        showNoSubscriptionPage();
        break;
      default:
        showChatPage(chatServiceManager.getUserPreferenceService().getActiveChatMode());
        break;
    }
    this.parent.layout();
  }

  /**
   * Build the view for the given chat mode.
   *
   * @param chatMode the chat mode
   */
  public void buildViewFor(ChatMode chatMode) {
    if (chatMode == null) {
      return;
    }
    this.onCancel();

    // Skip building the view when the user is not signed in or authenticated to avoid mode page overrides
    // @link BeforeLoginWelcomeViewer. See: https://github.com/microsoft/copilot-eclipse/issues/851
    if (!this.chatServiceManager.getAuthStatusManager().isNotSignedInOrNotAuthorized()) {
      if (hasHistory) {
        // Keep the main section to keep the conversation history and refresh the action bar only
        refreshActionBarTextViewerAndButtons();
      } else {
        // If no history, refresh the main section and action bar together
        disposeChildren(parent);
        showChatPage(chatMode);
      }
      this.parent.layout();
    }
  }

  private void showChatPage(ChatMode chatMode) {
    switch (chatMode) {
      case Agent:
        showAgentModePage();
        break;
      case Ask:
      default:
        showAfterLoginPage();
        break;
    }
  }

  private void showLoadingPage() {
    createContentWrapper();
    createMainSection(new GridData(SWT.FILL, SWT.CENTER, true, true));
    createLoadingPage();
  }

  private void showBeforeLoginPage() {
    createContentWrapper();
    createMainSection(new GridData(SWT.FILL, SWT.CENTER, true, true));
    createBeforeLoginWelcomePage();
  }

  private void showNoSubscriptionPage() {
    createContentWrapper();
    createMainSection(new GridData(SWT.FILL, SWT.CENTER, true, true));
    createNoSubscriptionPage();
  }

  private void showAgentModePage() {
    // upper bar
    this.topBanner = new TopBanner(parent, SWT.NONE);
    this.topBanner.registerNewConversationListener(this);

    createContentWrapper();

    // main section
    createMainSection(new GridData(SWT.FILL, SWT.FILL, true, true));

    if (hasHistory) {
      createConversationPage();
    } else {
      createAgentModePage();
    }

    // input field
    if (this.actionBar == null || this.actionBar.isDisposed()) {
      createActionBar();
    } else {
      refreshActionBarTextViewerAndButtons();
    }
  }

  private void showAfterLoginPage() {
    // upper bar
    this.topBanner = new TopBanner(parent, SWT.NONE);
    this.topBanner.registerNewConversationListener(this);

    createContentWrapper();

    // main section
    createMainSection(new GridData(SWT.FILL, SWT.FILL, true, true));

    if (hasHistory) {
      createConversationPage();
    } else {
      createAfterLoginWelcomePage();
    }

    // input field
    if (this.actionBar == null || this.actionBar.isDisposed()) {
      createActionBar();
    } else {
      refreshActionBarTextViewerAndButtons();
    }
  }

  private void disposeChildren(Composite composite) {
    if (composite == null || composite.isDisposed()) {
      return;
    }
    for (Control child : composite.getChildren()) {
      child.dispose();
    }
  }

  private void createContentWrapper() {
    this.contentWrapper = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, true);
    layout.marginWidth = 10;
    layout.marginHeight = 0;
    layout.marginBottom = 10;
    layout.verticalSpacing = 0;
    this.contentWrapper.setLayout(layout);
    this.contentWrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
  }

  private void createMainSection(GridData gridData) {
    this.mainSection = new Composite(this.contentWrapper, SWT.NONE);
    GridLayout gl = new GridLayout(1, true);
    gl.marginLeft = 0;
    gl.marginRight = 0;
    gl.marginWidth = 0;
    this.mainSection.setLayout(gl);
    this.mainSection.setLayoutData(gridData);
  }

  private void createActionBar() {
    this.actionBar = new ActionBar(this.contentWrapper, SWT.NONE, chatServiceManager);
    this.actionBar.registerMessageListener(this);
    this.topBanner.registerNewConversationListener(this.actionBar);
  }

  private void createLoadingPage() {
    clearChatView();
    this.loadingViewer = new LoadingViewer(this.mainSection, SWT.NONE);
    this.mainSection.layout();
  }

  /**
   * Create a conversation page.
   */
  private void createConversationPage() {
    clearChatView();
    this.chatContentViewer = new ChatContentViewer(this.mainSection, SWT.NONE, this.chatServiceManager);
    this.mainSection.layout(true, true);
  }

  /**
   * Create a welcome page.
   */
  private void createAfterLoginWelcomePage() {
    clearChatView();
    this.afterLoginWelcomeViewer = new AfterLoginWelcomeViewer(this.mainSection, SWT.NONE);
    this.mainSection.layout();
  }

  private void createBeforeLoginWelcomePage() {
    clearChatView();
    this.beforeLoginWelcomeViewer = new BeforeLoginWelcomeViewer(parent, SWT.NONE);
    this.mainSection.layout();
  }

  private void createNoSubscriptionPage() {
    clearChatView();
    this.beforeLoginWelcomeViewer = new NoSubscriptionViewer(this.mainSection, SWT.NONE);
    this.mainSection.layout();
  }

  private void createAgentModePage() {
    clearChatView();
    this.agentModeViewer = new AgentModeViewer(this.mainSection, SWT.NONE);
    this.mainSection.layout();
  }

  private void refreshActionBarTextViewerAndButtons() {
    this.actionBar.refreshChatInputTextViewer();
    this.actionBar.updateButtonsLayout();
  }

  private void clearChatView() {
    if (this.mainSection == null || mainSection.isDisposed()) {
      return;
    }
    for (Control control : mainSection.getChildren()) {
      control.dispose();
    }
    if (this.beforeLoginWelcomeViewer != null) {
      this.beforeLoginWelcomeViewer.dispose();
      this.beforeLoginWelcomeViewer = null;
    }
    if (this.afterLoginWelcomeViewer != null) {
      this.afterLoginWelcomeViewer.dispose();
      this.afterLoginWelcomeViewer = null;
    }
    if (this.agentModeViewer != null) {
      this.agentModeViewer.dispose();
      this.agentModeViewer = null;
    }
    if (this.chatContentViewer != null) {
      this.chatContentViewer.dispose();
      this.chatContentViewer = null;
    }
    if (this.noSubscriptionViewer != null) {
      this.noSubscriptionViewer.dispose();
      this.noSubscriptionViewer = null;
    }
    if (this.loadingViewer != null) {
      this.loadingViewer.dispose();
      this.loadingViewer = null;
    }
  }

  /**
   * Custom function.
   */
  @Override
  public void onChatProgress(ChatProgressValue value) {
    if (this.actionBar.isSendButton()) {
      return;
    }
    switch (value.getKind()) {
      case begin:
        if (this.chatContentViewer != null) {
          this.chatContentViewer.getLatestOrCreateNewTurnWidget(value.getTurnId(), true, false);
        }
        // new a turn widget
        this.conversationId = value.getConversationId();
        break;
      case report:
        if (value.getSteps() != null) {
          for (ChatStep step : value.getSteps()) {
            if (step.getStatus().equals(ChatStepStatus.CANCELLED)
                && step.getId().equals(ChatStepTitles.GENERATE_RESPONSE)) {
              return;
            }
            if (step.getStatus().equals(ChatStepStatus.FAILED)) {
              CopilotCore.LOGGER.error(new IllegalStateException("Turn step failed, title=" + step.getTitle()));
              return;
            }
          }
        }
        if ((value.getAgentRounds() == null || value.getAgentRounds().isEmpty())
            && (value.getReply() == null || value.getReply().isEmpty())) {
          return;
        }
        if (this.chatContentViewer != null) {
          this.chatContentViewer.processTurnEvent(value);
        }
        break;
      case end:
        if (this.chatContentViewer != null) {
          this.chatContentViewer.processTurnEvent(value);
          this.actionBar.resetSendButton();
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void setFocus() {
    ChatEventsManager p = CopilotCore.getPlugin().getChatEventsManager();
    if (p != null && p.chatProgressListeners.size() == 0) {
      p.addChatProgressListener(this);
    }
    if (actionBar != null) {
      actionBar.setFocusToInputTextViewer();
    }
  }

  @Override
  public void onSend(String workDoneToken, String message, boolean createNewTurn) {
    String processedMessage = replaceWorkspaceCommand(message);
    CopilotLanguageServerConnection ls = CopilotCore.getPlugin().getCopilotLanguageServer();
    CopilotModel activeModel = chatServiceManager.getUserPreferenceService().getActiveModel();
    String modelName = activeModel == null ? null
        : activeModel.isChatFallback() ? activeModel.getId() : activeModel.getModelFamily();
    String chatModeName = chatServiceManager.getUserPreferenceService().getActiveChatMode().toString();
    if (!(this.hasHistory)) {
      this.hasHistory = true;
      createConversationPage();
    }

    ReferencedFileService fileService = chatServiceManager.getReferencedFileService();
    IFile currentFile = fileService.getCurrentFile();
    List<IFile> references = fileService.getReferencedFiles();
    if (conversationId == null || conversationId.isEmpty()) {
      // create a new conversation
      CompletableFuture<ChatCreateResult> createConversationFuture = ls.createConversation(workDoneToken,
          processedMessage, references, currentFile, modelName, chatModeName);
      conversationFutures.add(createConversationFuture);

      createConversationFuture.exceptionally(ex -> {
        if (ex instanceof CancellationException) {
          return null;
        }
        CopilotCore.LOGGER.error("Error creating new conversation with exception: ", ex);
        displayErrorAndResetSendButton(workDoneToken, ex.getMessage());
        return null;
      });
    } else {
      // send message to existing conversation
      CompletableFuture<ChatTurnResult> addConversationFuture = ls.addConversationTurn(workDoneToken, conversationId,
          processedMessage, references, currentFile, modelName, chatModeName);
      conversationFutures.add(addConversationFuture);

      addConversationFuture.exceptionally(ex -> {
        if (ex instanceof CancellationException) {
          return null;
        }
        CopilotCore.LOGGER.error("Error sending message to existing conversation with exception: ", ex);
        displayErrorAndResetSendButton(workDoneToken, ex.getMessage());
        return null;
      });
    }

    if (createNewTurn) {
      // TODO: Move to createPartControl...eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_ON_SEND...(line 114)
      // after the refactor.
      this.chatServiceManager.getFileToolService().notifyCodeAcceptance();
      this.chatContentViewer.startNewTurn(workDoneToken, message);
    }
  }

  /**
   * Align with @Workspace of vscode, because we are actually indexing the whole workspace, not a single project.
   * (@Project is only for IntelliJ.)
   *
   * @param message the original message
   * @return the processed message
   */
  private String replaceWorkspaceCommand(String message) {
    if (!StringUtils.isBlank(message)
        && chatServiceManager.getUserPreferenceService().getActiveChatMode() == ChatMode.Ask
        && message.trim().startsWith(ChatCompletionService.AGENT_MARK + "workspace")) {
      return message.replaceFirst(ChatCompletionService.AGENT_MARK + "workspace",
          ChatCompletionService.AGENT_MARK + "project");
    }

    return message;
  }

  private void displayErrorAndResetSendButton(String workDoneToken, String message) {
    if (message == null) {
      message = "Unknown error";
    }
    String content = String.format(Messages.chat_chatContentView_errorTemplate, message, workDoneToken);
    SwtUtils.invokeOnDisplayThread(() -> {
      chatContentViewer.renderErrorMessage(content);
      actionBar.resetSendButton();
    }, mainSection);
  }

  @Override
  public void onCancel() {
    conversationFutures.forEach(future -> {
      future.cancel(false);
    });
    conversationFutures.clear();
  }

  @Override
  public void onNewConversation() {
    this.onCancel();
    this.hasHistory = false;
    this.conversationId = "";
    ChatMode chatMode = chatServiceManager.getUserPreferenceService().getActiveChatMode();
    if (chatMode != null && chatMode.equals(ChatMode.Agent)) {
      createAgentModePage();
    } else {
      createAfterLoginWelcomePage();
    }
    chatServiceManager.getReferencedFileService().updateReferencedFiles(List.of());
    setFocus();
  }

  /**
   * Get the current conversation ID.
   */
  public String getConversationId() {
    return this.conversationId;
  }

  /**
   * Get the current chat content viewer.
   */
  public ChatContentViewer getChatContentViewer() {
    return this.chatContentViewer;
  }

  /**
   * Get the content section of the chat view.
   */
  public Composite getContentWrapper() {
    return this.contentWrapper;
  }

  public Composite getActionBar() {
    return this.actionBar;
  }

  /**
   * Register a new conversation listener to the action bar.
   */
  public void registerNewConversationListenerToTheTopBanner(NewConversationListener listener) {
    this.topBanner.registerNewConversationListener(listener);
  }

  /**
   * Dispose the view.
   */
  @Override
  public void dispose() {
    ChatEventsManager p = CopilotCore.getPlugin().getChatEventsManager();
    if (p != null) {
      p.removeChatProgressListener(this);
    }
    // Unbind from the auth status service to dispose of the side effect
    if (this.chatServiceManager != null) {
      this.chatServiceManager.getAuthStatusService().unbindChatView(this);
    }
    if (this.actionBar != null) {
      this.actionBar.unregisterMessageListener(this);
      this.actionBar.dispose();
    }
    if (this.topBanner != null) {
      this.topBanner.unregisterNewConversationListener(this);
      this.topBanner.dispose();
    }
    if (this.chatContentViewer != null) {
      this.chatContentViewer.dispose();
    }
    if (this.afterLoginWelcomeViewer != null) {
      this.afterLoginWelcomeViewer.dispose();
    }
    if (this.beforeLoginWelcomeViewer != null) {
      this.beforeLoginWelcomeViewer.dispose();
    }
    if (this.loadingViewer != null) {
      this.loadingViewer.dispose();
    }
    if (this.noSubscriptionViewer != null) {
      this.noSubscriptionViewer.dispose();
    }
    if (this.agentModeViewer != null) {
      this.agentModeViewer.dispose();
    }
    super.dispose();
  }

  /**
   * Layout the view.
   */
  public void layout(boolean changed, boolean all) {
    parent.layout(changed, all);
  }
}

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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jdt.annotation.Nullable;
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
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatCreateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatStep;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatStepStatus;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatStepTitles;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatTurnResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Turn;
import com.microsoft.copilot.eclipse.core.persistence.AbstractTurnData;
import com.microsoft.copilot.eclipse.core.persistence.ConversationPersistenceManager;
import com.microsoft.copilot.eclipse.core.persistence.ConversationXmlData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.EditAgentRoundData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ReplyData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatCompletionService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.viewers.AfterLoginWelcomeViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.AgentModeViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.BeforeLoginWelcomeViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.ChatHistoryViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.LoadingViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.NoSubscriptionViewer;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * A view that displays chat messages.
 */
public class ChatView extends ViewPart implements ChatProgressListener, MessageListener, NewConversationListener {
  // service
  private ChatServiceManager chatServiceManager;
  private ConversationPersistenceManager persistenceManager;

  private Composite parent;
  private TopBanner topBanner;
  private Composite contentWrapper;
  private ActionBar actionBar;
  private ChatContentViewer chatContentViewer;
  private Composite loadingViewer;
  private Composite noSubscriptionViewer;
  private Composite beforeLoginWelcomeViewer;
  private Composite afterLoginWelcomeViewer;
  private Composite agentModeViewer;
  private boolean hasHistory = false;
  private String conversationId = "";
  private ConversationState conversationState = ConversationState.NEW_CONVERSATION;
  private Set<CompletableFuture<?>> conversationFutures = new HashSet<>();
  private IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
  private DragReferenceManager dragReferenceManager;

  // Chat history related fields
  private ChatHistoryViewer chatHistoryViewer;
  private boolean isChatHistoryVisible = false;

  @Override
  public void createPartControl(Composite parent) {
    this.parent = parent;
    GridLayout layout = new GridLayout(1, true);
    layout.verticalSpacing = 0;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.marginBottom = 10;
    parent.setLayout(layout);
    parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    parent.setData(CssConstants.CSS_ID_KEY, "chat-container");

    this.chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
    if (this.chatServiceManager == null) {
      createLoadingPage();
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
          ChatView.this.persistenceManager = ChatView.this.chatServiceManager.getPersistenceManager();
          chatServiceManager.getUserPreferenceService().bindChatView(ChatView.this);
          chatServiceManager.getAgentToolService().bindChatView(ChatView.this);
          chatServiceManager.getFileToolService().bindFileChangeSummaryBar(ChatView.this);
          Job.getJobManager().removeJobChangeListener(this);
        }
      };
      Job.getJobManager().addJobChangeListener(adapter);
    } else {
      this.persistenceManager = this.chatServiceManager.getPersistenceManager();
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

    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, event -> {
      Object status = event.getProperty(IEventBroker.DATA);
      if (status != null && status instanceof CopilotStatusResult statusResult) {
        if (statusResult.isNotSignedIn()) {
          this.conversationId = "";
        }
        buildViewFor(statusResult.getStatus());
      }
    });

    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_HIDE_CHAT_HISTORY, event -> {
      hideChatHistory();
    });

    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_SHOW_CHAT_HISTORY, event -> {
      showChatHistory();
    });

    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_HISTORY_BACK_CLICKED, event -> {
      hideChatHistory();
    });

    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_HISTORY_CONVERSATION_SELECTED, event -> {
      Object conversationData = event.getProperty(IEventBroker.DATA);
      ConversationXmlData conversation = conversationData instanceof ConversationXmlData
          ? (ConversationXmlData) conversationData
          : null;

      if (conversation != null && StringUtils.equals(conversation.getConversationId(), conversationId)) {
        // Selected conversation is the current conversation, just hide history and early return.
        hideChatHistory();
        return;
      }

      clearCurrentConversation();

      if (conversation == null) {
        // Handle "New Chat" selection.
        hideChatHistory();
        return;
      }

      // Load the full conversation data
      persistenceManager.loadConversation(conversation.getConversationId()).thenAccept(historyConversation -> {
        SwtUtils.invokeOnDisplayThreadAsync(() -> {
          // Set the conversation ID and state for history-based conversation
          ChatView.this.conversationId = historyConversation.getConversationId();
          ChatView.this.conversationState = ConversationState.NEW_HISTORY_BASED_CONVERSATION;

          // Ensure we have a chat content viewer
          if (!hasHistory) {
            hasHistory = true;
            createConversationPage();
          }

          // Clear existing content by recreating the chat content viewer
          if (chatContentViewer != null) {
            chatContentViewer.dispose();
            createConversationPage();
          }

          // Update the title in top banner
          if (topBanner != null && !topBanner.isDisposed()) {
            String displayTitle = conversation.getTitle();
            if (displayTitle != null && !displayTitle.trim().isEmpty()) {
              topBanner.updateTitle(displayTitle);
            }
          }

          // Restore all turns from the conversation
          if (historyConversation.getTurns() != null && !historyConversation.getTurns().isEmpty()) {
            for (AbstractTurnData turn : historyConversation.getTurns()) {
              restoreTurn(turn);
            }
          }

          // Hide chat history and show restored conversation
          hideChatHistory();
        }, contentWrapper);
      }).exceptionally(ex -> {
        CopilotCore.LOGGER.error("Failed to load conversation: " + conversation.getConversationId(), ex);
        SwtUtils.invokeOnDisplayThreadAsync(() -> {
          hideChatHistory();
        }, contentWrapper);
        return null;
      });
    });
  }

  /**
   * Build the view for the given status.
   *
   * @param status the status
   */
  public void buildViewFor(String status) {
    if (chatServiceManager == null) {
      return;
    }
    this.hasHistory = false;

    switch (status) {
      case CopilotStatusResult.LOADING:
        createLoadingPage();
        break;
      case CopilotStatusResult.NOT_SIGNED_IN:
        createBeforeLoginWelcomePage();
        break;
      case CopilotStatusResult.NOT_AUTHORIZED:
        createNoSubscriptionPage();
        break;
      default:
        createChatPage(chatServiceManager.getUserPreferenceService().getActiveChatMode());
        break;
    }
    this.parent.requestLayout();
  }

  /**
   * Build the view for the given chat mode.
   *
   * @param chatMode the chat mode
   */
  public void buildViewFor(ChatMode chatMode) {
    if (this.chatServiceManager.getAuthStatusManager().isNotSignedIn()) {
      createBeforeLoginWelcomePage();
      return;
    }
    if (chatMode == null) {
      return;
    }

    if (!hasHistory) {
      createChatPage(chatMode);
    }
    refreshActionBarTextViewerAndButtons();
    this.parent.requestLayout();
    setFocus();
  }

  private void createChatPage(ChatMode chatMode) {
    disposeComposite(topBanner);
    disposeComposite(contentWrapper);

    getDragReferenceManager().attach(parent);

    switch (chatMode) {
      case Ask:
        createChatModeView();
        break;
      case Agent:
      default:
        createAgentModeView();
        break;
    }
  }

  private void createAgentModeView() {
    // upper bar
    this.topBanner = new TopBanner(parent, SWT.NONE);
    this.topBanner.registerNewConversationListener(this);

    createOrReuseContentWrapper();

    if (hasHistory) {
      createConversationPage();
    } else {
      createAgentModePage();
    }

    // input field
    if (this.actionBar == null || this.actionBar.isDisposed()) {
      createActionBar();
    } else {
      // re-register the action bar if it already exists
      this.topBanner.registerNewConversationListener(this.actionBar);

      // This is a necessary step since we use actionBar cache when switching
      // chat modes and the topBanner & contentWrapper will on the bottom of the
      // actionBar if we use cache.
      if (this.contentWrapper != null && !this.contentWrapper.isDisposed()) {
        this.actionBar.moveBelow(this.contentWrapper);
      }
    }
  }

  private void createChatModeView() {
    // upper bar
    this.topBanner = new TopBanner(parent, SWT.NONE);
    this.topBanner.registerNewConversationListener(this);

    createOrReuseContentWrapper();

    if (hasHistory) {
      createConversationPage();
    } else {
      createAfterLoginWelcomePage();
    }

    // input field
    if (this.actionBar == null || this.actionBar.isDisposed()) {
      createActionBar();
    } else {
      // re-register the action bar if it already exists
      this.topBanner.registerNewConversationListener(this.actionBar);

      // This is a necessary step since we use actionBar cache when switching
      // chat modes and the topBanner & contentWrapper will on the bottom of the
      // actionBar if we use cache.
      if (this.contentWrapper != null && !this.contentWrapper.isDisposed()) {
        this.actionBar.moveBelow(this.contentWrapper);
      }
    }
  }

  /**
   * Keep the composite itself but dispose all children of the given composite.
   *
   * @param composite the composite to dispose children from
   */
  private void disposeChildren(Composite composite) {
    if (composite == null || composite.isDisposed()) {
      return;
    }
    for (Control child : composite.getChildren()) {
      child.dispose();
    }
  }

  /**
   * Dispose a composite and all its children.
   *
   * @param composite the composite to dispose, can be null
   */
  private void disposeComposite(@Nullable Composite composite) {
    if (composite == null || composite.isDisposed()) {
      return;
    }

    composite.dispose();
  }

  private void createOrReuseContentWrapper() {
    if (this.contentWrapper != null && !this.contentWrapper.isDisposed()) {
      // If contentWrapper is already created, we can reuse it.
      // This is a necessary step since we use contentWrapper cache when switching
      // chat modes and the topBanner will on the bottom of the
      // contentWrapper if we use cache.
      this.topBanner.moveAbove(this.contentWrapper);
    } else {
      createContentWrapper();
    }
  }

  private void createContentWrapper() {
    this.contentWrapper = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, true);
    layout.marginWidth = 0;
    layout.marginRight = 10;
    layout.marginHeight = 0;
    layout.marginBottom = 10;
    layout.verticalSpacing = 0;
    this.contentWrapper.setLayout(layout);
    this.contentWrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    this.contentWrapper.setData(CssConstants.CSS_ID_KEY, "chat-content-wrapper");
  }

  private void createActionBar() {
    this.actionBar = new ActionBar(parent, SWT.NONE, chatServiceManager);
    this.actionBar.registerMessageListener(this);
    this.topBanner.registerNewConversationListener(this.actionBar);
  }

  private void createLoadingPage() {
    clearChatView(parent);
    this.loadingViewer = new LoadingViewer(parent, SWT.NONE);
    this.loadingViewer.requestLayout();
  }

  /**
   * Create a conversation page.
   */
  private void createConversationPage() {
    clearChatView(this.contentWrapper);
    this.chatContentViewer = new ChatContentViewer(this.contentWrapper, SWT.NONE, this.chatServiceManager);
    this.chatContentViewer.requestLayout();
  }

  /**
   * Create a welcome page.
   */
  private void createAfterLoginWelcomePage() {
    clearChatView(this.contentWrapper);
    this.afterLoginWelcomeViewer = new AfterLoginWelcomeViewer(this.contentWrapper, SWT.NONE);
    this.afterLoginWelcomeViewer.requestLayout();
  }

  private void createBeforeLoginWelcomePage() {
    clearChatView(parent);
    this.beforeLoginWelcomeViewer = new BeforeLoginWelcomeViewer(parent, SWT.NONE);
    this.beforeLoginWelcomeViewer.requestLayout();
  }

  private void createNoSubscriptionPage() {
    clearChatView(parent);
    this.noSubscriptionViewer = new NoSubscriptionViewer(parent, SWT.NONE);
    this.noSubscriptionViewer.requestLayout();
  }

  private void createAgentModePage() {
    clearChatView(this.contentWrapper);
    this.agentModeViewer = new AgentModeViewer(this.contentWrapper, SWT.NONE);
    this.agentModeViewer.requestLayout();
  }

  private void refreshActionBarTextViewerAndButtons() {
    this.actionBar.refreshChatInputTextViewer();
    this.actionBar.updateButtonsLayout();
  }

  private void clearChatView(Composite composite) {
    disposeChildren(composite);
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
        // Update conversation ID when a new conversation is created
        String newConversationId = value.getConversationId();

        // Update persistence based on conversation state
        persistenceManager.updateConversationIdToHistoryRecord(newConversationId, this.conversationId);

        // Set the new conversation ID and update state
        this.conversationId = newConversationId;
        this.conversationState = ConversationState.CONTINUED_CONVERSATION;

        // Cache conversation progress on begin
        persistenceManager.cacheConversationProgress(this.conversationId, value);
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

        // Cache conversation progress on report
        if (persistenceManager != null) {
          persistenceManager.cacheConversationProgress(this.conversationId, value);
        }
        break;
      case end:
        if (this.chatContentViewer != null) {
          this.chatContentViewer.processTurnEvent(value);
          this.actionBar.resetSendButton();
          this.topBanner.updateTitle(value.getSuggestedTitle());
        }

        // Persist final conversation state and conversation title on end
        if (persistenceManager != null) {
          persistenceManager.persistConversationProgress(this.conversationId, value);
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void setFocus() {
    ChatEventsManager p = CopilotCore.getPlugin().getChatEventsManager();
    if (p != null && p.chatProgressListeners.isEmpty()) {
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
    CopilotModel activeModel = chatServiceManager.getModelService().getActiveModel();
    String chatModeName = chatServiceManager.getUserPreferenceService().getActiveChatMode().toString();
    ChatMode activeChatMode = chatServiceManager.getUserPreferenceService().getActiveChatMode();

    if (!(this.hasHistory)) {
      this.hasHistory = true;
      createConversationPage();
    }

    ReferencedFileService fileService = chatServiceManager.getReferencedFileService();
    // Clean up any non-existent files before sending the message
    fileService.cleanupNonExistentFiles();

    IFile currentFile = fileService.getCurrentFile();
    List<IResource> references = fileService.getReferencedFiles();

    if (conversationState == ConversationState.CONTINUED_CONVERSATION) {
      // Continue existing conversation - persist user message and send to existing conversation
      if (persistenceManager != null) {
        persistenceManager.persistUserTurnInfo(conversationId, workDoneToken, processedMessage, activeModel,
            activeChatMode.toString(), currentFile, references);
      }

      CompletableFuture<ChatTurnResult> addConversationFuture = ls.addConversationTurn(workDoneToken, conversationId,
          processedMessage, references, currentFile, activeModel, chatModeName);
      conversationFutures.add(addConversationFuture);

      addConversationFuture.exceptionally(ex -> {
        if (ex instanceof CancellationException) {
          return null;
        }
        CopilotCore.LOGGER.error("Error sending message to existing conversation with exception: ", ex);
        displayErrorAndResetSendButton(workDoneToken, ex.getMessage());
        return null;
      });
    } else {
      // Create new conversation (either brand new or based on history)
      List<Turn> turns = null;

      if (conversationState == ConversationState.NEW_HISTORY_BASED_CONVERSATION) {
        // Load turns from the history conversation and persist user turn with current conversation ID
        turns = persistenceManager.loadConversationTurns(this.conversationId);
        persistenceManager.persistUserTurnInfo(this.conversationId, workDoneToken, processedMessage, activeModel,
            activeChatMode.toString(), currentFile, references);
      } else if (conversationState == ConversationState.NEW_CONVERSATION) {
        // Generate a temporary ID for brand new conversation and persist user turn
        this.conversationId = UUID.randomUUID().toString();
        persistenceManager.persistUserTurnInfo(this.conversationId, workDoneToken, processedMessage, activeModel,
            activeChatMode.toString(), currentFile, references);
      }

      CompletableFuture<ChatCreateResult> createConversationFuture = ls.createConversation(workDoneToken,
          processedMessage, references, currentFile, turns, activeModel, chatModeName);
      conversationFutures.add(createConversationFuture);

      createConversationFuture.thenAccept(result -> {
        // Update the temporary conversation ID to the real conversation ID returned by the server
        String newConversationId = result.getConversationId();
        persistenceManager.updateConversationIdToHistoryRecord(newConversationId, this.conversationId);
      }).exceptionally(ex -> {
        if (ex instanceof CancellationException) {
          return null;
        }
        CopilotCore.LOGGER.error("Error creating new conversation with exception: ", ex);
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
    }, parent);
  }

  private void clearCurrentConversation() {
    this.onCancel();
    this.hasHistory = false;
    this.conversationId = "";
    this.conversationState = ConversationState.NEW_CONVERSATION;
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
    clearCurrentConversation();
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

  public ActionBar getActionBar() {
    return this.actionBar;
  }

  /**
   * Register a new conversation listener to the action bar.
   */
  public void registerNewConversationListenerToTheTopBanner(NewConversationListener listener) {
    this.topBanner.registerNewConversationListener(listener);
  }

  /**
   * create or return the existing DragReferenceManager.
   */
  private DragReferenceManager getDragReferenceManager() {
    if (dragReferenceManager == null) {
      dragReferenceManager = new DragReferenceManager(this, this.chatServiceManager.getReferencedFileService());
    }
    return dragReferenceManager;
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
    if (dragReferenceManager != null) {
      dragReferenceManager.dispose();
      dragReferenceManager = null;
    }
    super.dispose();
  }

  /**
   * Layout the view.
   */
  public void layout(boolean changed, boolean all) {
    parent.layout(changed, all);
  }

  /**
   * Show the chat history list.
   */
  public void showChatHistory() {
    if (isChatHistoryVisible || contentWrapper == null || contentWrapper.isDisposed()) {
      return;
    }

    // Hide main content inside the contentWrapper so history can fill it
    for (Control child : contentWrapper.getChildren()) {
      Object ld = child.getLayoutData();
      if (ld instanceof GridData gd) {
        gd.exclude = true;
      }
      child.setVisible(false);
    }

    // Hide action bar and exclude it from layout so the history fills all space
    if (actionBar != null && !actionBar.isDisposed()) {
      Object ld = actionBar.getLayoutData();
      if (ld instanceof GridData gd) {
        gd.exclude = true;
      }
      actionBar.setVisible(false);
    }

    // Hide FileChangeSummaryBar if it exists
    FileChangeSummaryBar fileChangeSummaryBar = chatServiceManager.getFileToolService().getFileChangeSummaryBar();
    if (fileChangeSummaryBar != null && !fileChangeSummaryBar.isDisposed()) {
      Object ld = fileChangeSummaryBar.getLayoutData();
      if (ld instanceof GridData gd) {
        gd.exclude = true;
      }
      fileChangeSummaryBar.setVisible(false);
    }

    // Get conversations from persistence service
    ConversationPersistenceManager persistenceManager = CopilotUi.getPlugin().getChatServiceManager()
        .getPersistenceManager();
    List<ConversationXmlData> conversations = persistenceManager.listConversations();

    // Use the current conversation ID for current conversation detection
    String currentConversationId = this.conversationId;

    chatHistoryViewer = new ChatHistoryViewer(contentWrapper, SWT.NONE, conversations, currentConversationId);
    chatHistoryViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    isChatHistoryVisible = true;
    contentWrapper.requestLayout();
  }

  /**
   * Hide the chat history and show the main content.
   */
  public void hideChatHistory() {
    if (!isChatHistoryVisible || contentWrapper == null || contentWrapper.isDisposed()) {
      return;
    }

    if (chatHistoryViewer != null && !chatHistoryViewer.isDisposed()) {
      chatHistoryViewer.dispose();
      chatHistoryViewer = null;
    }

    // Show main content inside the contentWrapper and render them back in layout
    for (Control child : contentWrapper.getChildren()) {
      Object ld = child.getLayoutData();
      if (ld instanceof GridData gd) {
        gd.exclude = false;
      }
      child.setVisible(true);
    }

    // Show action bar and render it back in layout
    if (actionBar != null && !actionBar.isDisposed()) {
      Object ld = actionBar.getLayoutData();
      if (ld instanceof GridData gd) {
        gd.exclude = false;
      }
      actionBar.setVisible(true);
    }

    // Show FileChangeSummaryBar if it exists
    FileChangeSummaryBar fileChangeSummaryBar = chatServiceManager.getFileToolService().getFileChangeSummaryBar();
    if (fileChangeSummaryBar != null && !fileChangeSummaryBar.isDisposed()) {
      Object ld = fileChangeSummaryBar.getLayoutData();
      if (ld instanceof GridData gd) {
        gd.exclude = false;
      }
      fileChangeSummaryBar.setVisible(true);
    }

    isChatHistoryVisible = false;
    contentWrapper.requestLayout();
  }

  /**
   * Restore a single turn from persisted conversation data.
   *
   * @param turn the turn data to restore
   */
  private void restoreTurn(AbstractTurnData turn) {
    if (turn == null || chatContentViewer == null) {
      return;
    }

    // Create user turn widget and populate with user message
    if (turn instanceof UserTurnData userTurn) {
      if (userTurn.getMessage() == null || StringUtils.isNotBlank(userTurn.getMessage().getText())) {
        BaseTurnWidget userTurnWidget = chatContentViewer.getLatestOrCreateNewTurnWidget(turn.getTurnId(), false, true);
        userTurnWidget.appendMessage(userTurn.getMessage().getText());
        userTurnWidget.notifyTurnEnd();
        return;
      }
    } else if (turn instanceof CopilotTurnData copilotTurn) {
      BaseTurnWidget copilotTurnWidget = chatContentViewer.getLatestOrCreateNewTurnWidget(turn.getTurnId(), true, true);
      ReplyData replyData = copilotTurn.getReply();

      if ((replyData != null && StringUtils.isNotBlank(replyData.getText()))) {
        copilotTurnWidget.appendMessage(replyData.getText());
      }

      if (replyData.getEditAgentRounds() != null && !replyData.getEditAgentRounds().isEmpty()) {
        for (EditAgentRoundData round : replyData.getEditAgentRounds()) {
          // Append each round's reply text.
          if (round.getReply() != null && !round.getReply().isEmpty()) {
            copilotTurnWidget.appendMessage(round.getReply());
          }

          // Concatenate tool call statuses from all rounds.
          if (round.getToolCalls() != null && !round.getToolCalls().isEmpty()) {
            for (ToolCallData toolCallData : round.getToolCalls()) {
              // Convert TurnData.ToolCallData to AgentToolCall
              AgentToolCall agentToolCall = persistenceManager.getDataFactory()
                  .convertToolCallDataToAgentToolCall(toolCallData);
              copilotTurnWidget.appendToolCallStatus(agentToolCall);
            }
          }
        }
      }

      copilotTurnWidget.notifyTurnEnd();
    }
  }
}

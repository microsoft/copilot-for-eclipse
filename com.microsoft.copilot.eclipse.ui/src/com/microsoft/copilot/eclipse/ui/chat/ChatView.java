package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatMode;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatModeManager;
import com.microsoft.copilot.eclipse.core.chat.ChatEventsManager;
import com.microsoft.copilot.eclipse.core.chat.ChatProgressListener;
import com.microsoft.copilot.eclipse.core.chat.CustomChatModeManager;
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
import com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent.CodingAgentMessageRequestParams;
import com.microsoft.copilot.eclipse.core.persistence.AbstractTurnData;
import com.microsoft.copilot.eclipse.core.persistence.ConversationPersistenceManager;
import com.microsoft.copilot.eclipse.core.persistence.ConversationXmlData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.AgentMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.EditAgentRoundData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ReplyData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatCompletionService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.viewers.AfterLoginWelcomeViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.AgentModeViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.BeforeLoginWelcomeViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.ChatHistoryViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.LoadingViewer;
import com.microsoft.copilot.eclipse.ui.chat.viewers.NoSubscriptionViewer;
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
  private HandoffContainer handoffContainer;
  private ActionBar actionBar;
  private ChatContentViewer chatContentViewer;
  private Composite loadingViewer;
  private Composite noSubscriptionViewer;
  private Composite beforeLoginWelcomeViewer;
  private Composite afterLoginWelcomeViewer;
  private Composite agentModeViewer;
  private boolean hasHistory = false;
  private String conversationId = "";
  private String subagentConversationId = null;
  private ConversationState conversationState = ConversationState.NEW_CONVERSATION;
  private Set<CompletableFuture<?>> conversationFutures = new HashSet<>();
  private IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
  private DragReferenceManager dragReferenceManager;

  // Chat history related fields
  private ChatHistoryViewer chatHistoryViewer;
  private boolean isChatHistoryVisible = false;

  // Event handlers for cleanup
  private EventHandler chatOnSendHandler;
  private EventHandler chatMessageSendHandler;
  private EventHandler authStatusChangedHandler;
  private EventHandler hideChatHistoryHandler;
  private EventHandler showChatHistoryHandler;
  private EventHandler historyBackClickedHandler;
  private EventHandler historyConversationSelectedHandler;
  private EventHandler conversationTitleUpdatedHandler;
  private EventHandler codingAgentMessageHandler;

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
          initializeChatServices();
          Job.getJobManager().removeJobChangeListener(this);
        }
      };
      Job.getJobManager().addJobChangeListener(adapter);
    } else {
      initializeChatServices();
    }

    this.chatOnSendHandler = event -> {
      Object params = event.getProperty(IEventBroker.DATA);
      if (params != null && params instanceof Map properties) {
        String workDoneToken = UUID.randomUUID().toString();
        String previousInput = (String) properties.get("previousInput");
        boolean needCreateUserTurn = (boolean) properties.get("needCreateUserTurn");
        onSendInternal(workDoneToken, previousInput, null, null, needCreateUserTurn);
      }
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_ON_SEND, this.chatOnSendHandler);

    this.chatMessageSendHandler = event -> {
      Object params = event.getProperty(IEventBroker.DATA);
      if (params != null && params instanceof Map properties) {
        String workDoneToken = (String) properties.get("workDoneToken");
        String message = (String) properties.get("message");
        String agentSlug = (String) properties.get("agentSlug");
        String agentJobWorkspaceFolder = (String) properties.get("agentJobWorkspaceFolder");
        boolean createNewTurn = (boolean) properties.get("createNewTurn");
        onSendInternal(workDoneToken, message, agentSlug, agentJobWorkspaceFolder, createNewTurn);
      }
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_MESSAGE_SEND, this.chatMessageSendHandler);

    this.authStatusChangedHandler = event -> {
      Object status = event.getProperty(IEventBroker.DATA);
      if (status != null && status instanceof CopilotStatusResult statusResult) {
        if (statusResult.isNotSignedIn()) {
          // Reset status
          this.conversationId = "";
          conversationState = ConversationState.NEW_CONVERSATION;
          isChatHistoryVisible = false;
        }
        buildViewFor(statusResult.getStatus());
      }
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, this.authStatusChangedHandler);

    this.hideChatHistoryHandler = event -> {
      hideChatHistory();
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_HIDE_CHAT_HISTORY, this.hideChatHistoryHandler);

    this.showChatHistoryHandler = event -> {
      showChatHistory();
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_SHOW_CHAT_HISTORY, this.showChatHistoryHandler);

    this.historyBackClickedHandler = event -> {
      hideChatHistory();
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_HISTORY_BACK_CLICKED, this.historyBackClickedHandler);

    this.historyConversationSelectedHandler = event -> {
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

            // Restore the mode from the last user turn
            restoreModeFromLastUserTurn(historyConversation.getTurns());
          }

          // Scroll to bottom after restoring all turns
          SwtUtils.invokeOnDisplayThreadAsync(this::scrollContentToBottom, chatContentViewer);

          // Hide chat history and show restored conversation
          hideChatHistory();
        }, contentWrapper);
      }).exceptionally(ex -> {
        CopilotCore.LOGGER.error("Failed to load conversation: " + conversation.getConversationId(), ex);
        SwtUtils.invokeOnDisplayThreadAsync(this::hideChatHistory, contentWrapper);
        return null;
      });
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_HISTORY_CONVERSATION_SELECTED,
        this.historyConversationSelectedHandler);

    this.conversationTitleUpdatedHandler = event -> {
      Object titleData = event.getProperty(IEventBroker.DATA);
      if (titleData instanceof String newTitle && StringUtils.isNotEmpty(newTitle) && topBanner != null
          && !topBanner.isDisposed()) {
        topBanner.updateTitle(newTitle);
      }
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_CONVERSATION_TITLE_UPDATED,
        this.conversationTitleUpdatedHandler);

    this.codingAgentMessageHandler = event -> {
      Object messageData = event.getProperty(IEventBroker.DATA);
      if (messageData instanceof CodingAgentMessageRequestParams params) {
        handleCodingAgentMessage(params);
      }
    };
    this.eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_CODING_AGENT_MESSAGE, this.codingAgentMessageHandler);
  }

  /**
   * Initialize chat services and bind the chat view to various service components.
   * This method sets up the persistence manager, binds the chat view to user preference,
   * agent tool, and file tool services, and triggers view building based on auth status.
   */
  private void initializeChatServices() {
    this.persistenceManager = this.chatServiceManager.getPersistenceManager();
    chatServiceManager.getUserPreferenceService().bindChatView(this);
    chatServiceManager.getAgentToolService().bindChatView(this);
    chatServiceManager.getFileToolService().bindFileChangeSummaryBar(this);

    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      String authStatus = chatServiceManager.getAuthStatusManager().getCopilotStatus();
      if (authStatus != null) {
        buildViewFor(authStatus);
      }
    }, parent);
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

    // Show handoffs when mode changes if there's history, otherwise hide
    if (handoffContainer != null && !handoffContainer.isDisposed()) {
      if (hasHistory) {
        handoffContainer.show();
      } else {
        handoffContainer.hide();
      }
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
      // Always use default Agent mode page for welcome screen
      createAgentModePage();
    }

    // input field - Create ActionBar FIRST
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

    // Create HandoffContainer AFTER ActionBar (so actionBar exists when passed to constructor)
    if (this.handoffContainer == null || this.handoffContainer.isDisposed()) {
      createHandoffContainer();
    }
    if (this.actionBar != null && !this.actionBar.isDisposed()) {
      // Ensure handoffContainer is positioned before actionBar
      this.handoffContainer.moveAbove(this.actionBar);
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

  private void createHandoffContainer() {
    this.handoffContainer = new HandoffContainer(parent, chatServiceManager, actionBar, this);
    this.handoffContainer.setVisible(false);
    GridData gd = (GridData) this.handoffContainer.getLayoutData();
    if (gd != null) {
      gd.exclude = true;
    }
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

        // Handle subagent conversation ID management
        if (StringUtils.isNotBlank(value.getParentTurnId())) {
          // Entering a subagent context - store the subagent conversation ID
          this.subagentConversationId = value.getConversationId();
        } else {
          // Not in subagent context - update the main conversation ID
          String newConversationId = value.getConversationId();

          // Update persistence only if conversation ID changed
          if (!StringUtils.equals(newConversationId, this.conversationId)) {
            try {
              persistenceManager.updateConversationIdToHistoryRecord(newConversationId, this.conversationId).get();
            } catch (InterruptedException | ExecutionException e) {
              CopilotCore.LOGGER.error("Error updating conversation ID in persistence manager: ", e);
            }

            // Set the new conversation ID and update state
            this.conversationId = newConversationId;
            this.conversationState = ConversationState.CONTINUED_CONVERSATION;
          }
        }

        // Cache conversation progress on begin
        persistenceManager.cacheConversationProgress(this.conversationId, value);

        // Hide handoff container when new turn starts
        Display.getDefault().asyncExec(() -> {
          if (handoffContainer != null && !handoffContainer.isDisposed()) {
            handoffContainer.hide();
          }
        });
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

        // If exiting subagent context (no parentTurnId), clear the subagent conversation ID
        if (StringUtils.isBlank(value.getParentTurnId()) && this.subagentConversationId != null) {
          this.subagentConversationId = null;
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

        // Show handoff container when turn finishes
        Display.getDefault().asyncExec(() -> {
          if (handoffContainer != null && !handoffContainer.isDisposed()) {
            handoffContainer.show();
          }
        });
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

  private void onSendInternal(String workDoneToken, String message, String agentSlug, String agentJobWorkspaceFolder,
      boolean createNewTurn) {
    String processedMessage = replaceWorkspaceCommand(message);

    // Persist the user input to history
    chatServiceManager.getUserPreferenceService().addInputToHistory(processedMessage);

    final ChatMode activeChatMode = chatServiceManager.getUserPreferenceService().getActiveChatMode();

    // Get mode information
    String activeModeId = chatServiceManager.getUserPreferenceService().getActiveModeNameOrId();

    // Determine chat mode name and custom mode ID for LSP
    String chatModeName;
    String customChatModeId = null;

    // Check if it's a custom mode
    if (CustomChatModeManager.INSTANCE.isCustomMode(activeModeId)) {
      chatModeName = ChatMode.Agent.toString(); // "Agent"
      customChatModeId = activeModeId; // "file://..."
    } else {
      // Check if it's a built-in mode
      BuiltInChatMode builtInMode = BuiltInChatModeManager.INSTANCE.getBuiltInModeByDisplayName(activeModeId);
      if (builtInMode != null) {
        // For Ask mode, use "Ask" with no custom ID
        if (BuiltInChatMode.ASK_MODE_NAME.equalsIgnoreCase(builtInMode.getDisplayName())) {
          chatModeName = ChatMode.Ask.toString(); // "Ask"
          customChatModeId = null;
        } else {
          // For other built-in modes (Agent, Plan), use "Agent" with built-in mode ID as custom ID
          chatModeName = ChatMode.Agent.toString(); // "Agent"
          customChatModeId = builtInMode.getId(); // "Agent" or "Plan"
        }
      } else {
        // Fallback to enum
        chatModeName = activeChatMode.toString();
        customChatModeId = null;
      }
    }

    if (!(this.hasHistory)) {
      this.hasHistory = true;
      createConversationPage();
    }

    ReferencedFileService fileService = chatServiceManager.getReferencedFileService();
    // Clean up any non-existent files before sending the message
    fileService.cleanupNonExistentFiles();

    IFile currentFile = fileService.getCurrentFile();
    List<IResource> references = fileService.getReferencedFiles();

    final CopilotLanguageServerConnection ls = CopilotCore.getPlugin().getCopilotLanguageServer();
    final CopilotModel activeModel = chatServiceManager.getModelService().getActiveModel();

    if (conversationState == ConversationState.CONTINUED_CONVERSATION) {
      // Continue existing conversation - persist user message and send to existing conversation
      if (persistenceManager != null) {
        persistenceManager.persistUserTurnInfo(conversationId, workDoneToken, processedMessage, activeModel,
            chatModeName, customChatModeId, currentFile, references);
      }

      CompletableFuture<ChatTurnResult> addConversationFuture = ls.addConversationTurn(workDoneToken, conversationId,
          processedMessage, references, currentFile, activeModel, chatModeName, customChatModeId, agentSlug,
          agentJobWorkspaceFolder);
      conversationFutures.add(addConversationFuture);

      addConversationFuture.thenAccept(result -> {
        // Render and persist model information in the Copilot turn widget
        if (result != null && StringUtils.isNotBlank(result.getModelName())
            && !UiConstants.GITHUB_COPILOT_CODING_AGENT_SLUG.equals(result.getAgentSlug())) {
          renderModelInfoInTurnWidget(result.getTurnId(), result.getModelName(), result.getBillingMultiplier());

          // Persist model information
          if (persistenceManager != null) {
            persistenceManager.persistModelInfo(result.getConversationId(), result.getTurnId(), result.getModelName(),
                result.getBillingMultiplier());
          }
        }
      }).exceptionally(th -> {
        if (!ConversationUtils.isConversationCancellationThrowable(th)) {
          CopilotCore.LOGGER.error("Error sending message to existing conversation with exception: ", th);
          displayErrorAndResetSendButton(workDoneToken, th.getMessage());
        }
        return null;
      });
    } else {
      // Create new conversation (either brand new or based on history)
      List<Turn> turns = null;

      if (conversationState == ConversationState.NEW_HISTORY_BASED_CONVERSATION) {
        // Load turns from the history conversation and persist user turn with current conversation ID
        turns = persistenceManager.loadConversationTurns(this.conversationId);
        persistenceManager.persistUserTurnInfo(this.conversationId, workDoneToken, processedMessage, activeModel,
            chatModeName, customChatModeId, currentFile, references);
      } else if (conversationState == ConversationState.NEW_CONVERSATION) {
        // Generate a temporary ID for brand new conversation and persist user turn
        this.conversationId = UUID.randomUUID().toString();
        persistenceManager.persistUserTurnInfo(this.conversationId, workDoneToken, processedMessage, activeModel,
            chatModeName, customChatModeId, currentFile, references);
      }

      CompletableFuture<ChatCreateResult> createConversationFuture = null;
      if (StringUtils.isBlank(agentSlug)) {
        createConversationFuture = ls.createConversation(workDoneToken, processedMessage, references, currentFile,
            turns, activeModel, chatModeName, customChatModeId);
      } else {
        // For conversations sending to agents, include agentSlug and specify the target agentJobWorkspaceFolder
        createConversationFuture = ls.createConversation(workDoneToken, processedMessage, references, currentFile,
            turns, activeModel, chatModeName, customChatModeId, agentSlug, agentJobWorkspaceFolder);
      }
      conversationFutures.add(createConversationFuture);

      createConversationFuture.thenAccept(result -> {
        // Update the temporary conversation ID to the real conversation ID returned by the server
        String newConversationId = result.getConversationId();
        try {
          persistenceManager.updateConversationIdToHistoryRecord(newConversationId, this.conversationId).get();
        } catch (InterruptedException | ExecutionException e) {
          CopilotCore.LOGGER.error("Error updating conversation ID in persistence manager: ", e);
        }

        // Render model information in the Copilot turn widget
        if (result != null && StringUtils.isNotBlank(result.getModelName())
            && !UiConstants.GITHUB_COPILOT_CODING_AGENT_SLUG.equals(result.getAgentSlug())) {
          renderModelInfoInTurnWidget(result.getTurnId(), result.getModelName(), result.getBillingMultiplier());

          // Persist model information
          if (persistenceManager != null) {
            persistenceManager.persistModelInfo(newConversationId, result.getTurnId(), result.getModelName(),
                result.getBillingMultiplier());
          }
        }
      }).exceptionally(th -> {
        if (!ConversationUtils.isConversationCancellationThrowable(th)) {
          CopilotCore.LOGGER.error("Error creating new conversation with exception: ", th);
          displayErrorAndResetSendButton(workDoneToken, th.getMessage());
        }
        return null;
      });
    }

    if (createNewTurn) {
      // TODO: Move to createPartControl...eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_ON_SEND...(line 114)
      // after the refactor.
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
      message = Messages.chat_warnWidget_defaultErrorMsg;
    }
    String content = String.format(Messages.chat_chatContentView_errorTemplate, message, workDoneToken);
    SwtUtils.invokeOnDisplayThread(() -> {
      chatContentViewer.renderErrorMessage(content);
      actionBar.resetSendButton();
    }, parent);
  }

  private void handleCodingAgentMessage(CodingAgentMessageRequestParams params) {
    if (params == null || this.chatContentViewer == null) {
      return;
    }

    if (!StringUtils.equals(params.getConversationId(), this.conversationId)) {
      return;
    }

    // Persist the agent message to conversation history
    if (persistenceManager != null) {
      // TODO: We currently only have GitHub Copilot Coding Agent, need to extend for other agents in the future
      persistenceManager.addCodingAgentMessage(params, UiConstants.GITHUB_COPILOT_CODING_AGENT_SLUG);
    }

    SwtUtils.invokeOnDisplayThread(() -> {
      if (this.chatContentViewer != null && !this.chatContentViewer.isDisposed()) {
        BaseTurnWidget turnWidget = this.chatContentViewer.getTurnWidget(params.getTurnId());
        if (turnWidget != null && !turnWidget.isDisposed()) {
          turnWidget.createAgentMessageWidget(params);
        }
      }
    }, parent);
  }

  private void clearCurrentConversation() {
    this.onCancel();
    this.hasHistory = false;
    this.conversationId = "";
    this.conversationState = ConversationState.NEW_CONVERSATION;
    this.chatServiceManager.getReferencedFileService().updateReferencedFiles(List.of());
    SwtUtils.invokeOnDisplayThreadAsync(this.chatServiceManager.getFileToolService()::disposeFileChangeSummaryBar);
  }

  @Override
  public void onCancel() {
    // Clear subagent conversation ID on cancel
    this.subagentConversationId = null;

    if (persistenceManager != null && StringUtils.isNotBlank(this.conversationId)) {
      persistenceManager.persistCachedConversation(this.conversationId);
    }
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

    // Hide handoff container when creating a new conversation
    Display.getDefault().asyncExec(() -> {
      if (handoffContainer != null && !handoffContainer.isDisposed()) {
        handoffContainer.hide();
      }
    });

    setFocus();
  }

  /**
   * Get the current conversation ID.
   */
  public String getConversationId() {
    return this.conversationId;
  }

  /**
   * Get the current subagent conversation ID, or null if not in a subagent context.
   */
  public String getSubagentConversationId() {
    return this.subagentConversationId;
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

    // Cancel any pending conversation futures
    clearCurrentConversation();

    // Unsubscribe all event handlers
    if (this.eventBroker != null) {
      if (chatOnSendHandler != null) {
        this.eventBroker.unsubscribe(this.chatOnSendHandler);
        chatOnSendHandler = null;
      }
      if (chatMessageSendHandler != null) {
        this.eventBroker.unsubscribe(this.chatMessageSendHandler);
        chatMessageSendHandler = null;
      }
      if (authStatusChangedHandler != null) {
        this.eventBroker.unsubscribe(this.authStatusChangedHandler);
        authStatusChangedHandler = null;
      }
      if (hideChatHistoryHandler != null) {
        this.eventBroker.unsubscribe(this.hideChatHistoryHandler);
        hideChatHistoryHandler = null;
      }
      if (showChatHistoryHandler != null) {
        this.eventBroker.unsubscribe(this.showChatHistoryHandler);
        showChatHistoryHandler = null;
      }
      if (historyBackClickedHandler != null) {
        this.eventBroker.unsubscribe(this.historyBackClickedHandler);
        historyBackClickedHandler = null;
      }
      if (historyConversationSelectedHandler != null) {
        this.eventBroker.unsubscribe(this.historyConversationSelectedHandler);
        historyConversationSelectedHandler = null;
      }
      if (conversationTitleUpdatedHandler != null) {
        this.eventBroker.unsubscribe(this.conversationTitleUpdatedHandler);
        conversationTitleUpdatedHandler = null;
      }
      if (codingAgentMessageHandler != null) {
        this.eventBroker.unsubscribe(this.codingAgentMessageHandler);
        codingAgentMessageHandler = null;
      }
    }

    if (this.chatServiceManager != null) {
      if (this.chatServiceManager.getUserPreferenceService() != null) {
        this.chatServiceManager.getUserPreferenceService().unbindChatView();
      }
      if (this.chatServiceManager.getAgentToolService() != null) {
        this.chatServiceManager.getAgentToolService().unbindChatView();
      }
      if (this.chatServiceManager.getFileToolService() != null) {
        this.chatServiceManager.getFileToolService().unbindFileChangeSummaryBar();
      }
      this.chatServiceManager = null;
    }
    if (this.actionBar != null) {
      this.actionBar.unregisterMessageListener(this);
      this.actionBar.dispose();
      this.actionBar = null;
    }
    if (this.topBanner != null) {
      this.topBanner.unregisterNewConversationListener(this);
      this.topBanner.dispose();
      this.topBanner = null;
    }
    if (this.handoffContainer != null) {
      this.handoffContainer.dispose();
      this.handoffContainer = null;
    }
    if (this.chatHistoryViewer != null && !this.chatHistoryViewer.isDisposed()) {
      this.chatHistoryViewer.dispose();
      this.chatHistoryViewer = null;
    }
    if (this.chatContentViewer != null) {
      this.chatContentViewer.dispose();
      this.chatContentViewer = null;
    }
    if (this.afterLoginWelcomeViewer != null) {
      this.afterLoginWelcomeViewer.dispose();
      this.afterLoginWelcomeViewer = null;
    }
    if (this.beforeLoginWelcomeViewer != null) {
      this.beforeLoginWelcomeViewer.dispose();
      this.beforeLoginWelcomeViewer = null;
    }
    if (this.loadingViewer != null) {
      this.loadingViewer.dispose();
      this.loadingViewer = null;
    }
    if (this.noSubscriptionViewer != null) {
      this.noSubscriptionViewer.dispose();
      this.noSubscriptionViewer = null;
    }
    if (this.agentModeViewer != null) {
      this.agentModeViewer.dispose();
      this.agentModeViewer = null;
    }
    if (this.contentWrapper != null && !this.contentWrapper.isDisposed()) {
      this.contentWrapper.dispose();
      this.contentWrapper = null;
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
   * Scroll the chat content viewer to the bottom.
   */
  public void scrollContentToBottom() {
    if (chatContentViewer == null || chatContentViewer.isDisposed()) {
      return;
    }

    chatContentViewer.getDisplay().asyncExec(() -> {
      if (chatContentViewer.isDisposed()) {
        return;
      }
      chatContentViewer.refreshScrollerLayout();
      ScrollBar verticalBar = chatContentViewer.getVerticalBar();
      if (verticalBar != null && !verticalBar.isDisposed()) {
        chatContentViewer.setOrigin(0, verticalBar.getMaximum());
      }
    });
  }

  /**
   * Render model information in the Copilot turn widget.
   *
   * @param turnId the turn ID
   * @param conversationId the conversation ID to use for persistence
   * @param modelName the model name
   * @param billingMultiplier the billing multiplier
   */
  private void renderModelInfoInTurnWidget(String turnId, String modelName, double billingMultiplier) {
    BaseTurnWidget turnWidget = this.chatContentViewer.getTurnWidget(turnId);
    if (turnWidget instanceof CopilotTurnWidget copilotWidget) {
      copilotWidget.renderModelInfo(modelName, billingMultiplier);

      // Refresh the scroller layout to ensure the footer is visible
      SwtUtils.invokeOnDisplayThreadAsync(() -> this.chatContentViewer.refreshScrollerLayout(), this.chatContentViewer);
    }
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

      if (replyData == null) {
        return;
      }

      if (StringUtils.isNotBlank(replyData.getText())) {
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

      // Restore any error messages widgets from the reply data
      if (replyData.getErrorMessages() != null && !replyData.getErrorMessages().isEmpty()) {
        for (ErrorMessageData errorMessageData : replyData.getErrorMessages()) {
          ErrorData errorData = errorMessageData.getError();
          SwtUtils.invokeOnDisplayThread(() -> {
            String errorMessage = errorData != null ? errorData.getMessage() : Messages.chat_warnWidget_defaultErrorMsg;
            int errorCode = errorData != null ? errorData.getCode() : 0;

            copilotTurnWidget.createWarnDialog(errorMessage, errorCode);
          }, parent);
        }
      }

      // Restore any agent messages from the reply data
      if (replyData.getAgentMessages() != null && !replyData.getAgentMessages().isEmpty()) {
        for (AgentMessageData agentMessageData : replyData.getAgentMessages()) {
          // TODO: We currently only have GitHub Copilot Coding Agent, need to extend for other agents in the future
          if (StringUtils.equals(agentMessageData.getAgentSlug(), UiConstants.GITHUB_COPILOT_CODING_AGENT_SLUG)) {
            SwtUtils.invokeOnDisplayThread(() -> {
              // Create CodingAgentMessageRequestParams from the persisted data
              CodingAgentMessageRequestParams params = new CodingAgentMessageRequestParams();
              params.setTitle(agentMessageData.getTitle());
              params.setDescription(agentMessageData.getDescription());
              params.setPrLink(agentMessageData.getPrLink());
              params.setConversationId(this.conversationId);
              params.setTurnId(turn.getTurnId());

              copilotTurnWidget.createAgentMessageWidget(params);
            }, parent);
          }
        }
      }

      copilotTurnWidget.notifyTurnEnd();

      // Restore model info footer if model name is present
      // This must be done AFTER notifyTurnEnd() to ensure footer appears at the bottom
      if (StringUtils.isNotBlank(replyData.getModelName())) {
        renderModelInfoInTurnWidget(turn.getTurnId(), replyData.getModelName(), replyData.getBillingMultiplier());
      }
    }
  }

  /**
   * Restore the mode from the last user turn in the conversation.
   *
   * @param turns the list of turns to search for the last user turn
   */
  private void restoreModeFromLastUserTurn(List<AbstractTurnData> turns) {
    if (turns == null || turns.isEmpty() || chatServiceManager == null) {
      return;
    }

    // Find the last user turn by iterating backwards
    UserTurnData lastUserTurn = null;
    for (int i = turns.size() - 1; i >= 0; i--) {
      if (turns.get(i) instanceof UserTurnData userTurn) {
        lastUserTurn = userTurn;
        break;
      }
    }

    if (lastUserTurn == null) {
      return;
    }

    String chatMode = lastUserTurn.getChatMode();
    String customChatModeId = lastUserTurn.getCustomChatModeId();

    // Restore the mode based on chatMode and customChatModeId
    if (StringUtils.isNotBlank(customChatModeId)) {
      // Custom mode or built-in mode with custom ID
      if (CustomChatModeManager.INSTANCE.isCustomMode(customChatModeId)) {
        // It's a custom mode
        chatServiceManager.getUserPreferenceService().setActiveChatMode(customChatModeId);
      } else {
        // It's a built-in mode (Agent/Plan) stored as customChatModeId
        BuiltInChatMode builtInMode = BuiltInChatModeManager.INSTANCE.getBuiltInModeById(customChatModeId);
        if (builtInMode != null) {
          chatServiceManager.getUserPreferenceService().setActiveChatMode(builtInMode.getDisplayName());
        }
      }
    } else if (StringUtils.isNotBlank(chatMode)) {
      // Fall back to chatMode for backward compatibility or Ask mode
      try {
        ChatMode mode = ChatMode.valueOf(chatMode);
        if (mode == ChatMode.Ask) {
          chatServiceManager.getUserPreferenceService().setActiveChatMode(BuiltInChatMode.ASK_MODE_NAME);
        } else if (mode == ChatMode.Agent) {
          chatServiceManager.getUserPreferenceService().setActiveChatMode(BuiltInChatMode.AGENT_MODE_NAME);
        }
      } catch (IllegalArgumentException e) {
        CopilotCore.LOGGER.error("Unknown chat mode: " + chatMode, e);
      }
    }
  }
}

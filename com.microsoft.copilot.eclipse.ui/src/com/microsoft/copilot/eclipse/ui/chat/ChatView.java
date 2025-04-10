package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.ViewPart;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ChatEventsManager;
import com.microsoft.copilot.eclipse.core.chat.ChatProgressListener;
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
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
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

  @Override
  public void createPartControl(Composite parent) {
    this.parent = parent;
    parent.setLayout(new GridLayout(1, true));
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
          ChatView.this.chatServiceManager.getChatModeService().bindChatView(ChatView.this);
          ChatView.this.chatServiceManager.getAgentToolService().bindChatView(ChatView.this);
          Job.getJobManager().removeJobChangeListener(this);
        }
      };
      Job.getJobManager().addJobChangeListener(adapter);
    } else {
      this.chatServiceManager.getAuthStatusService().bindChatView(this);
      this.chatServiceManager.getChatModeService().bindChatView(this);
      this.chatServiceManager.getAgentToolService().bindChatView(this);
    }
  }

  /**
   * Build the view for the given status.
   *
   * @param status the status
   */
  public void buildViewFor(String status) {
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
        showChatPage(chatServiceManager.getChatModeService().getActiveChatMode());
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
    this.onNewConversation();
    disposeChildren(parent);

    showChatPage(chatMode);
    this.parent.layout();
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
    createMainSection(new GridData(SWT.FILL, SWT.CENTER, true, true));
    createLoadingPage();
  }

  private void showBeforeLoginPage() {
    createMainSection(new GridData(SWT.FILL, SWT.CENTER, true, true));
    createBeforeLoginWelcomePage();
  }

  private void showNoSubscriptionPage() {
    createMainSection(new GridData(SWT.FILL, SWT.CENTER, true, true));
    createNoSubscriptionPage();
  }

  private void showAgentModePage() {
    // upper bar
    this.topBanner = new TopBanner(parent, SWT.NONE);
    this.topBanner.registerNewConversationListener(this);

    // main section
    createMainSection(new GridData(SWT.FILL, SWT.FILL, true, true));

    if (hasHistory) {
      createConversationPage();
    } else {
      createAgentModePage();
    }

    // input field
    this.actionBar = new ActionBar(parent, SWT.NONE, chatServiceManager);
    this.actionBar.registerMessageListener(this);
    this.topBanner.registerNewConversationListener(this.actionBar);
  }

  private void showAfterLoginPage() {
    // upper bar
    this.topBanner = new TopBanner(parent, SWT.NONE);
    this.topBanner.registerNewConversationListener(this);

    // main section
    createMainSection(new GridData(SWT.FILL, SWT.FILL, true, true));

    if (hasHistory) {
      createConversationPage();
    } else {
      createAfterLoginWelcomePage();
    }

    // input field
    this.actionBar = new ActionBar(parent, SWT.NONE, chatServiceManager);
    this.actionBar.registerMessageListener(this);
    this.topBanner.registerNewConversationListener(this.actionBar);
  }

  private void disposeChildren(Composite composite) {
    if (composite == null || composite.isDisposed()) {
      return;
    }
    for (Control child : composite.getChildren()) {
      child.dispose();
    }
  }

  private void createMainSection(GridData gridData) {
    this.mainSection = new Composite(parent, SWT.NONE);
    this.mainSection.setLayout(new GridLayout(1, true));
    this.mainSection.setLayoutData(gridData);
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
    this.beforeLoginWelcomeViewer = new BeforeLoginWelcomeViewer(this.mainSection, SWT.NONE);
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
          this.chatContentViewer.createNewTurn(value.getTurnId(), true);
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
        if (this.chatContentViewer != null && value.getCancellationReason() == null) {
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
  }

  @Override
  public void onSend(String workDoneToken, String message, List<IFile> files) {
    CopilotLanguageServerConnection ls = CopilotCore.getPlugin().getCopilotLanguageServer();
    CopilotModel activeModel = chatServiceManager.getCopilotModelService().getActiveModel();
    String modelName = activeModel == null ? null : activeModel.getModelFamily();
    String chatModeName = chatServiceManager.getChatModeService().getActiveChatMode().toString();
    if (!(this.hasHistory)) {
      this.hasHistory = true;
      createConversationPage();
    }
    if (conversationId == null || conversationId.isEmpty()) {
      // create a new conversation
      CompletableFuture<ChatCreateResult> createConversationFuture = ls.createConversation(workDoneToken, message,
          files, modelName, chatModeName);
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
          message, files, modelName, chatModeName);
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

    // TODO: what turn ID to use when we don't have the response yet?
    this.chatContentViewer.startNewTurn(workDoneToken, message);
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
    this.hasHistory = false;
    this.conversationId = "";
    this.onCancel();
    ChatMode chatMode = chatServiceManager.getChatModeService().getActiveChatMode();
    if (chatMode != null && chatMode.equals(ChatMode.Agent)) {
      createAgentModePage();
    } else {
      createAfterLoginWelcomePage();
    }
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

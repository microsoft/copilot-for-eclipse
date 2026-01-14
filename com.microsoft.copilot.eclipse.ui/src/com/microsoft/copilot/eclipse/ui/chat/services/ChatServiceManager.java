package com.microsoft.copilot.eclipse.ui.chat.services;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.service.IChatServiceManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.persistence.ConversationPersistenceManager;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService;

/**
 * Manager for chat services.
 */
public class ChatServiceManager implements IChatServiceManager {

  private CopilotLanguageServerConnection lsConnection;
  private AuthStatusManager authStatusManager;

  private ChatCompletionService chatCompletionService;
  private ModelService modelService;
  private ByokService byokService;
  private UserPreferenceService userPreferenceService;
  private AvatarService avatarService;
  private AgentToolService agentToolService;
  private FileToolService fileToolService;
  private ReferencedFileService referencedFileService;
  private McpConfigService mcpConfigService;
  private McpExtensionPointManager mcpExtensionPointManager;

  private McpRuntimeLogger mcpRuntimeLogger;
  private ConversationPersistenceManager persistenceManager;
  private TodoListService todoListService;

  /**
   * Constructor for the ChatServiceManager.
   */
  public ChatServiceManager() {
    this.lsConnection = CopilotCore.getPlugin().getCopilotLanguageServer();
    this.authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
    chatCompletionService = new ChatCompletionService(this.lsConnection, this.authStatusManager);
    modelService = new ModelService(this.lsConnection, this.authStatusManager);
    userPreferenceService = new UserPreferenceService(this.lsConnection, this.authStatusManager);
    avatarService = new AvatarService(this.authStatusManager);
    agentToolService = new AgentToolService(this.lsConnection);
    fileToolService = new FileToolService(this.lsConnection);
    referencedFileService = new ReferencedFileService();
    mcpConfigService = new McpConfigService();
    mcpExtensionPointManager = new McpExtensionPointManager(mcpConfigService);
    mcpRuntimeLogger = new McpRuntimeLogger();
    persistenceManager = new ConversationPersistenceManager(this.authStatusManager);
  }

  /**
   * Get the authentication status manager.
   */
  public AuthStatusManager getAuthStatusManager() {
    return authStatusManager;
  }

  /**
   * Get the chat command service of Ask Mode.
   *
   * @return the chat command service of Ask Mode
   */
  public ChatCompletionService getChatCompletionService() {
    return chatCompletionService;
  }

  public CopilotLanguageServerConnection getLanguageServerConnection() {
    return lsConnection;
  }

  /**
   * Lazy load the BYOK service. This service only needed by byokPreferencePage. So it should not be initialized in
   * activation step.
   *
   * @return the BYOK service
   */
  public ByokService getByokService() {
    if (byokService == null && this.lsConnection != null) {
      byokService = new ByokService(this.lsConnection);
    }
    return byokService;
  }

  /**
   * Get the avatar service.
   *
   * @return the avatar service
   */
  public AvatarService getAvatarService() {
    return avatarService;
  }

  /**
   * Get the copilot model service.
   *
   * @return the copilot model service
   */
  public UserPreferenceService getUserPreferenceService() {
    return userPreferenceService;
  }

  /**
   * Get the model service.
   *
   * @return the model service
   */
  public ModelService getModelService() {
    return modelService;
  }

  /**
   * Get the agent tool service.
   *
   * @return the agent tool service
   */
  public AgentToolService getAgentToolService() {
    return agentToolService;
  }

  /**
   * Get the edit file tool service.
   *
   * @return the edit file tool service
   */
  public FileToolService getFileToolService() {
    return fileToolService;
  }

  @Override
  public McpConfigService getMcpConfigService() {
    return mcpConfigService;
  }

  /**
   * Get the persistence manager.
   *
   * @return the persistence manager
   */
  public ConversationPersistenceManager getPersistenceManager() {
    return persistenceManager;
  }

  /**
   * Lazy load the todo list service. This service is only needed when the chat view is created.
   *
   * @return the todo list service
   */
  public TodoListService getTodoListService() {
    if (todoListService == null && this.lsConnection != null) {
      todoListService = new TodoListService(this.lsConnection);
    }
    return todoListService;
  }

  @Override
  public ReferencedFileService getReferencedFileService() {
    return referencedFileService;
  }

  /**
   * Get the MCP extension point manager.
   */
  public McpExtensionPointManager getMcpExtensionPointManager() {
    return mcpExtensionPointManager;
  }

  /**
   * Dispose of the chat services.
   */
  public void dispose() {
    this.avatarService.dispose();
    this.chatCompletionService.dispose();
    this.modelService.dispose();
    this.userPreferenceService.dispose();
    this.agentToolService.dispose();
    this.referencedFileService.dispose();
    this.mcpConfigService.dispose();
    if (this.byokService != null) {
      this.byokService.dispose();
    }
    if (this.todoListService != null) {
      this.todoListService.dispose();
    }
  }
}

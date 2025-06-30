package com.microsoft.copilot.eclipse.ui.chat.services;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.service.IChatServiceManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService;

/**
 * Manager for chat services.
 */
public class ChatServiceManager implements IChatServiceManager {

  private CopilotLanguageServerConnection lsConnection;
  private AuthStatusManager authStatusManager;

  private ChatCompletionService chatCompletionService;
  private UserPreferenceService userPreferenceService;
  private AvatarService avatarService;
  private AuthStatusService authStatusService;
  private AgentToolService agentToolService;
  private FileToolService fileToolService;
  private ReferencedFileService referencedFileService;
  private McpToolService mcpToolService;
  private McpRuntimeLogger mcpRuntimeLogger;

  /**
   * Constructor for the ChatServiceManager.
   */
  public ChatServiceManager() {
    this.lsConnection = CopilotCore.getPlugin().getCopilotLanguageServer();
    this.authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
    chatCompletionService = new ChatCompletionService(this.lsConnection, this.authStatusManager);
    userPreferenceService = new UserPreferenceService(this.lsConnection, this.authStatusManager);
    avatarService = new AvatarService(this.authStatusManager);
    authStatusService = new AuthStatusService(this.authStatusManager);
    agentToolService = new AgentToolService(this.lsConnection);
    fileToolService = new FileToolService();
    referencedFileService = new ReferencedFileService();
    mcpToolService = new McpToolService();
    mcpRuntimeLogger = new McpRuntimeLogger();
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
   * Get the avatar service.
   *
   * @return the avatar service
   */
  public AvatarService getAvatarService() {
    return avatarService;
  }

  public AuthStatusService getAuthStatusService() {
    return authStatusService;
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

  public McpToolService getMcpToolService() {
    return mcpToolService;
  }

  @Override
  public ReferencedFileService getReferencedFileService() {
    return referencedFileService;
  }

  /**
   * Dispose of the chat services.
   */
  public void dispose() {
    this.avatarService.dispose();
    this.chatCompletionService.dispose();
    this.userPreferenceService.dispose();
    this.authStatusService.dispose();
    this.agentToolService.dispose();
    this.referencedFileService.dispose();
    this.mcpToolService.dispose();
  }

}

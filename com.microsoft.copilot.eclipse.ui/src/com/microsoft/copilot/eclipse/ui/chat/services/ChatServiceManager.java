package com.microsoft.copilot.eclipse.ui.chat.services;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.chat.tools.EditFileToolService;

/**
 * Manager for chat services.
 */
public class ChatServiceManager {

  private CopilotLanguageServerConnection lsConnection;
  private AuthStatusManager authStatusManager;

  private SlashCommandService slashCommandService;
  private CopilotModelService copilotModelService;
  private ChatModeService chatModeService;
  private AvatarService avatarService;
  private AuthStatusService authStatusService;
  private AgentToolService agentToolService;
  private EditFileToolService editFileToolService;

  /**
   * Constructor for the ChatServiceManager.
   */
  public ChatServiceManager() {
    this.lsConnection = CopilotCore.getPlugin().getCopilotLanguageServer();
    this.authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
    slashCommandService = new SlashCommandService(this.lsConnection, this.authStatusManager);
    copilotModelService = new CopilotModelService(this.lsConnection, this.authStatusManager);
    chatModeService = new ChatModeService(this.lsConnection, this.authStatusManager);
    avatarService = new AvatarService(this.authStatusManager);
    authStatusService = new AuthStatusService(this.authStatusManager);
    agentToolService = new AgentToolService(this.lsConnection);
    editFileToolService = new EditFileToolService();
  }

  /**
   * Get the authentication status manager.
   */
  public AuthStatusManager getAuthStatusManager() {
    return authStatusManager;
  }

  /**
   * Get the slash command service.
   *
   * @return the slash command service
   */
  public SlashCommandService getSlashCommandService() {
    return slashCommandService;
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
  public CopilotModelService getCopilotModelService() {
    return copilotModelService;
  }

  /**
   * Get the chat mode service.
   *
   * @return the chat mode service
   */
  public ChatModeService getChatModeService() {
    return chatModeService;
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
  public EditFileToolService getEditFileToolService() {
    return editFileToolService;
  }

  /**
   * Dispose of the chat services.
   */
  public void dispose() {
    this.avatarService.dispose();
    this.slashCommandService.dispose();
    this.copilotModelService.dispose();
    this.chatModeService.dispose();
    this.authStatusService.dispose();
    this.agentToolService.dispose();
    this.editFileToolService.dispose();
  }

}

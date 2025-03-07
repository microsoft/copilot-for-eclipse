package com.microsoft.copilot.eclipse.ui.chat.services;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;

/**
 * Manager for chat services.
 */
public class ChatServiceManager {

  private CopilotLanguageServerConnection lsConnection;
  private AuthStatusManager authStatusManager;

  private SlashCommandService slashCommandService;
  private CopilotModelService copilotModelService;
  private AvatarService avatarService;
  private AuthStatusService authStatusService;

  /**
   * Constructor for the ChatServiceManager.
   */
  public ChatServiceManager() {
    this.lsConnection = CopilotCore.getPlugin().getCopilotLanguageServer();
    this.authStatusManager = CopilotCore.getPlugin().getAuthStatusManager();
    slashCommandService = new SlashCommandService(this.lsConnection, this.authStatusManager);
    copilotModelService = new CopilotModelService(this.lsConnection, this.authStatusManager);
    avatarService = new AvatarService(this.authStatusManager);
    authStatusService = new AuthStatusService(this.authStatusManager);
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
   * Dispose of the chat services.
   */
  public void dispose() {
    this.avatarService.dispose();
    this.slashCommandService.dispose();
    this.copilotModelService.dispose();
    this.authStatusService.dispose();
  }

}

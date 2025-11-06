package com.microsoft.copilot.eclipse.core.chat.service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.WorkspaceFolder;

import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;

/**
 * Service interface for managing custom chat modes.
 */
public interface ICustomModeService {
  /**
   * Load custom modes from remote service.
   *
   * @return a future containing the list of custom modes
   */
  CompletableFuture<List<CustomChatMode>> loadCustomModes();

  /**
   * Delete a custom mode from the remote service.
   *
   * @param id the id of the mode to delete
   * @return a future indicating completion
   */
  CompletableFuture<Void> deleteCustomMode(String id);

  /**
   * Create a custom mode in a specific workspace folder.
   *
   * @param workspaceFolder the workspace folder where the mode should be created
   * @param displayName the display name
   * @return a future containing the created custom mode
   */
  CompletableFuture<CustomChatMode> createCustomModeInWorkspaceFolder(WorkspaceFolder workspaceFolder,
      String displayName);
}

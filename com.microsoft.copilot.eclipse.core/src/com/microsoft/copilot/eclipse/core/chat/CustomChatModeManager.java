package com.microsoft.copilot.eclipse.core.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.service.FileBasedCustomModeService;
import com.microsoft.copilot.eclipse.core.chat.service.ICustomModeService;

/**
 * Manager for custom chat modes. Custom modes are loaded from and stored as .agent.md files in .github/agents
 * directory.
 */
public enum CustomChatModeManager {
  INSTANCE;

  private static final String SEPARATOR_PREFIX = "---";
  private List<CustomChatMode> customModes;
  private final ICustomModeService customModeService;

  CustomChatModeManager() {
    this.customModeService = new FileBasedCustomModeService();
    this.customModes = new CopyOnWriteArrayList<>();
    // Load custom modes from files on initialization
    loadCustomModesSync();
  }

  /**
   * Load custom modes synchronously on initialization. This is called in the constructor to ensure modes are available
   * immediately.
   */
  private void loadCustomModesSync() {
    try {
      customModes = customModeService.loadCustomModes().get();
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to load custom modes on initialization", e);
      customModes = new CopyOnWriteArrayList<>();
    }
  }

  /**
   * Get all custom modes.
   *
   * @return list of custom modes
   */
  public List<CustomChatMode> getCustomModes() {
    return new ArrayList<>(customModes);
  }

  /**
   * Synchronize custom modes from the remote service. TODO: This should be called when the user signs in or when modes
   * need to be refreshed.
   *
   * @return a future indicating completion
   * @throws RuntimeException if the remote service call fails
   */
  public CompletableFuture<Void> syncCustomModesFromService() {
    return customModeService.loadCustomModes().thenAccept(modes -> {
      customModes = modes;
    });
  }

  /**
   * Update an existing custom mode.
   *
   * @param mode the mode to update
   */
  public void updateCustomMode(CustomChatMode mode) {
    for (int i = 0; i < customModes.size(); i++) {
      if (customModes.get(i).getId().equals(mode.getId())) {
        customModes.set(i, mode);
        return;
      }
    }
  }

  /**
   * Delete a custom mode.
   *
   * @param id the id of the mode to delete
   * @return a future indicating completion
   * @throws RuntimeException if the remote service call fails
   */
  public CompletableFuture<Void> deleteCustomMode(String id) {
    // Call file service to delete the custom mode
    return customModeService.deleteCustomMode(id).thenRun(() -> {
      customModes.removeIf(mode -> mode.getId().equals(id));
    });
  }

  /**
   * Get a custom mode by ID.
   *
   * @param id the mode id
   * @return the custom mode or null if not found
   */
  public CustomChatMode getCustomModeById(String id) {
    return customModes.stream().filter(mode -> mode.getId().equals(id)).findFirst().orElse(null);
  }

  /**
   * Check if a mode ID is a custom mode.
   *
   * @param id the mode id
   * @return true if it's a custom mode
   */
  public boolean isCustomMode(String id) {
    if (id == null) {
      return false;
    }
    // Check if the ID matches any custom mode
    return customModes.stream().anyMatch(mode -> mode.getId().equals(id));
  }

  /**
   * Get the custom mode service.
   *
   * @return the custom mode service
   */
  public ICustomModeService getCustomModeService() {
    return customModeService;
  }
}

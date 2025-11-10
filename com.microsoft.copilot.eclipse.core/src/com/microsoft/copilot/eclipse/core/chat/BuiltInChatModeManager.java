package com.microsoft.copilot.eclipse.core.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.copilot.eclipse.core.chat.service.BuiltInChatModeService;

/**
 * Singleton manager for built-in chat modes. Built-in modes are loaded once from the LSP API at startup.
 */
public enum BuiltInChatModeManager {
  INSTANCE;

  private final BuiltInChatModeService service;
  private List<BuiltInChatMode> builtInModes;

  BuiltInChatModeManager() {
    this.service = new BuiltInChatModeService();
    this.builtInModes = new CopyOnWriteArrayList<>();
    loadModesSync();
  }

  private void loadModesSync() {
    try {
      List<BuiltInChatMode> modes = service.loadBuiltInModes().get();
      this.builtInModes = new CopyOnWriteArrayList<>(modes);
    } catch (Exception e) {
      // Initialize with empty list on failure
      this.builtInModes = new CopyOnWriteArrayList<>();
    }
  }

  public List<BuiltInChatMode> getBuiltInModes() {
    return new ArrayList<>(builtInModes);
  }

  /**
   * Retrieves a built-in chat mode by its display name.
   *
   * @param displayName the display name of the mode to retrieve (case-insensitive)
   * @return the built-in chat mode with the matching display name, or null if not found
   */
  public BuiltInChatMode getBuiltInModeByDisplayName(String displayName) {
    return builtInModes.stream().filter(mode -> mode.getDisplayName().equalsIgnoreCase(displayName)).findFirst()
        .orElse(null);
  }
}
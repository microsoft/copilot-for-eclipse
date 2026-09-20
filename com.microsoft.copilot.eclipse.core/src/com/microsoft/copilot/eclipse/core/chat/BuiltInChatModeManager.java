// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared snapshot of built-in chat modes discovered asynchronously by the chat lifecycle.
 */
public enum BuiltInChatModeManager {
  INSTANCE;

  private volatile List<BuiltInChatMode> builtInModes = List.of();

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

  /**
   * Retrieves a built-in chat mode by its ID.
   *
   * @param id the ID of the mode to retrieve
   * @return the built-in chat mode with the matching ID, or null if not found
   */
  public BuiltInChatMode getBuiltInModeById(String id) {
    return builtInModes.stream().filter(mode -> mode.getId().equals(id)).findFirst().orElse(null);
  }

  /**
   * Publishes a completed discovery after its owner has validated the account and lifecycle.
   *
   * @param modes the modes applicable to the current account
   */
  public void updateModes(List<BuiltInChatMode> modes) {
    builtInModes = List.copyOf(modes);
  }
}
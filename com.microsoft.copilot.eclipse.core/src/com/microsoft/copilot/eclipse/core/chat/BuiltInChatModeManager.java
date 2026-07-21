// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.microsoft.copilot.eclipse.core.chat.service.BuiltInChatModeService;

/**
 * Singleton manager for asynchronously loaded built-in chat modes.
 */
public final class BuiltInChatModeManager {

  public static final BuiltInChatModeManager INSTANCE = new BuiltInChatModeManager();

  private final BuiltInChatModeService service;
  private volatile List<BuiltInChatMode> builtInModes;
  private long loadGeneration;

  private BuiltInChatModeManager() {
    this(new BuiltInChatModeService());
  }

  BuiltInChatModeManager(BuiltInChatModeService service) {
    this.service = service;
    this.builtInModes = List.of();
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
   * Reloads built-in chat modes from the LSP API. This should be called when the user switches
   * to ensure the latest modes are available for the current user context.
   *
   * @return a future that completes after this load has been processed; stale results may be ignored
   */
  public CompletableFuture<Void> reloadModes() {
    final long requestGeneration;
    synchronized (this) {
      requestGeneration = ++loadGeneration;
    }

    return service.loadBuiltInModes().thenAccept(modes -> {
      synchronized (this) {
        if (requestGeneration == loadGeneration) {
          builtInModes = List.copyOf(modes);
        }
      }
    });
  }

  /**
   * Clears cached built-in modes and prevents in-flight loads from publishing stale results.
   */
  public synchronized void clearModes() {
    loadGeneration++;
    builtInModes = List.of();
  }
}

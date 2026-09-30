// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationModesParams;

/**
 * Shared snapshot of built-in chat modes discovered asynchronously by the chat lifecycle.
 */
public enum BuiltInChatModeManager {
  INSTANCE;

  private static final List<String> ALLOWED_BUILTIN_NAMES = Arrays.asList(BuiltInChatMode.ASK_MODE_NAME,
      BuiltInChatMode.AGENT_MODE_NAME, BuiltInChatMode.PLAN_MODE_NAME, BuiltInChatMode.DEBUGGER_MODE_NAME);

  private volatile List<BuiltInChatMode> builtInModes = List.of();

  /**
   * Loads built-in modes using the owning chat lifecycle's language-server connection. The LSP requires workspace
   * folders even though built-in modes do not depend on workspace context.
   *
   * @param lsConnection the connection used by the chat services
   * @return the discovered built-in modes
   */
  public CompletableFuture<List<BuiltInChatMode>> loadBuiltInModes(CopilotLanguageServerConnection lsConnection) {
    if (lsConnection == null) {
      return CompletableFuture.completedFuture(new ArrayList<>());
    }
    ConversationModesParams params = new ConversationModesParams(Collections.emptyList());

    return lsConnection.listConversationModes(params).thenApply(conversationModes -> {
      List<BuiltInChatMode> modes = new ArrayList<>();

      for (ConversationMode mode : conversationModes) {
        if (mode == null || !mode.isBuiltIn()) {
          continue;
        }
        // Exclude InlineAgent kind — it is not a user-facing chat mode
        if (BuiltInChatMode.INLINE_AGENT_KIND.equalsIgnoreCase(mode.getKind())) {
          continue;
        }
        // Filter to only allowed built-in modes by name (case-insensitive)
        if (ALLOWED_BUILTIN_NAMES.stream().anyMatch(name -> name.equalsIgnoreCase(mode.getName()))) {
          BuiltInChatMode builtIn = convertToBuiltInChatMode(mode);
          if (builtIn != null) {
            modes.add(builtIn);
          }
        }
      }

      return modes;
    });
  }

  private BuiltInChatMode convertToBuiltInChatMode(ConversationMode mode) {
    try {
      return new BuiltInChatMode(mode);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to convert built-in mode: " + mode.getId(), e);
      return null;
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
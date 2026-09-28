// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatMode;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationModesParams;

/**
 * Service for loading built-in chat modes from the LSP API. Built-in modes include Ask, Agent, and Plan.
 */
public class BuiltInChatModeService {

  private static final List<String> ALLOWED_BUILTIN_NAMES = Arrays.asList(BuiltInChatMode.ASK_MODE_NAME,
      BuiltInChatMode.AGENT_MODE_NAME, BuiltInChatMode.PLAN_MODE_NAME, BuiltInChatMode.DEBUGGER_MODE_NAME);

  /**
   * Loads built-in modes from the LSP API. Only modes with names in ALLOWED_BUILTIN_NAMES are returned.
   *
   * <p>Note: The LSP requires workspace folders to be passed even for loading built-in modes. While built-in modes
   * don't depend on workspace context, the LSP API enforces this parameter.
   */
  public CompletableFuture<List<BuiltInChatMode>> loadBuiltInModes() {
    return loadBuiltInModes(CopilotCore.getPlugin().getCopilotLanguageServer());
  }

  /**
   * Loads built-in modes using the owning chat lifecycle's language-server connection.
   *
   * @param lspConnection the connection used by the chat services
   * @return the discovered built-in modes
   */
  public CompletableFuture<List<BuiltInChatMode>> loadBuiltInModes(
      CopilotLanguageServerConnection lspConnection) {
    if (lspConnection == null) {
      return CompletableFuture.completedFuture(new ArrayList<>());
    }
    ConversationModesParams params = new ConversationModesParams(Collections.emptyList());

    return lspConnection.listConversationModes(params).thenApply(conversationModes -> {
      List<BuiltInChatMode> builtInModes = new ArrayList<>();

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
            builtInModes.add(builtIn);
          }
        }
      }

      return builtInModes;
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
}
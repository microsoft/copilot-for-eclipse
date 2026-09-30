// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationModesParams;

@ExtendWith(MockitoExtension.class)
class BuiltInChatModeManagerTests {

  @Mock
  private CopilotLanguageServerConnection mockConnection;

  @Test
  void testLoadBuiltInModes_inlineAgentSkippedFromAgentModes() {
    ConversationMode agentMode = createBuiltInMode("Agent", "Agent", "Agent",
        "Advanced agent mode with access to tools and capabilities");
    ConversationMode inlineAgentMode = createBuiltInMode("InlineAgent", "Agent", "InlineAgent",
        "Agent mode with a restricted tool set for inline editing");

    when(mockConnection.listConversationModes(any(ConversationModesParams.class)))
        .thenReturn(CompletableFuture.completedFuture(new ConversationMode[] { agentMode, inlineAgentMode }));

    List<BuiltInChatMode> builtInModes = BuiltInChatModeManager.INSTANCE.loadBuiltInModes(mockConnection).join();

    assertEquals(1, builtInModes.size());

    BuiltInChatMode builtInMode = builtInModes.get(0);
    assertNotNull(builtInMode);
    assertEquals("Agent", builtInMode.getId());
    assertEquals("Agent", builtInMode.getDisplayName());
    assertEquals("Agent", builtInMode.getKind());
  }

  @Test
  void testLoadBuiltInModes_onlyAllowedBuiltInNames() {
    ConversationMode ask = createBuiltInMode("ask", "aSk", "Ask", "Ask mode");
    ConversationMode debugger = createBuiltInMode("debugger", "Debugger", "Debugger", "Debugger mode");
    ConversationMode custom = createBuiltInMode("custom", "Plan", "Plan", "Custom mode");
    custom.setBuiltIn(false);
    ConversationMode unknown = createBuiltInMode("unknown", "Unknown", "Unknown", "Unknown mode");

    when(mockConnection.listConversationModes(any(ConversationModesParams.class)))
        .thenReturn(CompletableFuture.completedFuture(new ConversationMode[] {
            null, custom, unknown, ask, debugger }));

    List<BuiltInChatMode> modes = BuiltInChatModeManager.INSTANCE.loadBuiltInModes(mockConnection).join();

    assertEquals(List.of("ask", "debugger"), modes.stream().map(BuiltInChatMode::getId).toList());
  }

  @Test
  void testLoadBuiltInModes_nullConnectionReturnsEmptyList() {
    assertEquals(List.of(), BuiltInChatModeManager.INSTANCE.loadBuiltInModes(null).join());
  }

  @Test
  void testLoadBuiltInModes_conversionFailureSkipsMode() {
    ConversationMode invalid = mock(ConversationMode.class);
    when(invalid.isBuiltIn()).thenReturn(true);
    when(invalid.getName()).thenReturn("Ask");
    when(invalid.getId()).thenReturn("invalid");
    when(invalid.getCustomTools()).thenThrow(new IllegalArgumentException("invalid tools"));
    ConversationMode agent = createBuiltInMode("agent", "Agent", "Agent", "Agent mode");
    when(mockConnection.listConversationModes(any(ConversationModesParams.class)))
        .thenReturn(CompletableFuture.completedFuture(new ConversationMode[] { invalid, agent }));

    List<BuiltInChatMode> modes = BuiltInChatModeManager.INSTANCE.loadBuiltInModes(mockConnection).join();

    assertEquals(List.of("agent"), modes.stream().map(BuiltInChatMode::getId).toList());
  }

  @Test
  void testLoadBuiltInModes_rpcFailurePropagates() {
    IllegalStateException failure = new IllegalStateException("RPC failed");
    when(mockConnection.listConversationModes(any(ConversationModesParams.class)))
        .thenReturn(CompletableFuture.failedFuture(failure));

    CompletionException exception = assertThrows(CompletionException.class,
        () -> BuiltInChatModeManager.INSTANCE.loadBuiltInModes(mockConnection).join());

    assertSame(failure, exception.getCause());
  }

  private ConversationMode createBuiltInMode(String id, String name, String kind, String description) {
    ConversationMode mode = new ConversationMode();
    mode.setId(id);
    mode.setName(name);
    mode.setKind(kind);
    mode.setBuiltIn(true);
    mode.setDescription(description);
    return mode;
  }
}

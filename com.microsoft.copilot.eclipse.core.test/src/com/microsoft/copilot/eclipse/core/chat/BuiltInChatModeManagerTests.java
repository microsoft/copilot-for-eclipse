// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

  @BeforeEach
  void setUp() {
    BuiltInChatModeManager.INSTANCE.updateModes(List.of());
  }

  @AfterEach
  void clearSnapshot() {
    BuiltInChatModeManager.INSTANCE.updateModes(List.of());
  }

  @Test
  void testLoadBuiltInModes_inlineAgentSkippedFromAgentModes() {
    ConversationMode agentMode = createConversationMode("Agent", "Agent", "Agent");
    ConversationMode inlineAgentMode = createConversationMode("InlineAgent", "Agent", "InlineAgent");

    when(mockConnection.listConversationModes(any(ConversationModesParams.class)))
        .thenReturn(CompletableFuture.completedFuture(new ConversationMode[] { agentMode, inlineAgentMode }));

    List<BuiltInChatMode> builtInModes = BuiltInChatModeManager.INSTANCE.loadBuiltInModes(mockConnection).join();

    assertEquals(1, builtInModes.size());
    BuiltInChatMode builtInMode = builtInModes.get(0);
    assertEquals("Agent", builtInMode.getId());
    assertEquals("Agent", builtInMode.getDisplayName());
    assertEquals("Agent", builtInMode.getKind());
  }

  @Test
  void testLoadBuiltInModes_successDoesNotPublishSnapshot() {
    BuiltInChatMode publishedMode = createBuiltInChatMode("Ask");
    BuiltInChatModeManager.INSTANCE.updateModes(List.of(publishedMode));
    ConversationMode loadedMode = createConversationMode("Agent", "Agent", "Agent");
    when(mockConnection.listConversationModes(any(ConversationModesParams.class)))
        .thenReturn(CompletableFuture.completedFuture(new ConversationMode[] { loadedMode }));

    List<BuiltInChatMode> result = BuiltInChatModeManager.INSTANCE.loadBuiltInModes(mockConnection).join();

    assertEquals(List.of("Agent"), result.stream().map(BuiltInChatMode::getId).toList());
    assertEquals(List.of(publishedMode), BuiltInChatModeManager.INSTANCE.getBuiltInModes());
  }

  @Test
  void testLoadBuiltInModes_failureDoesNotReplaceSnapshot() {
    BuiltInChatMode publishedMode = createBuiltInChatMode("Ask");
    BuiltInChatModeManager.INSTANCE.updateModes(List.of(publishedMode));
    IllegalStateException failure = new IllegalStateException("mode discovery failed");
    when(mockConnection.listConversationModes(any(ConversationModesParams.class)))
        .thenReturn(CompletableFuture.failedFuture(failure));

    CompletionException exception = assertThrows(CompletionException.class,
        () -> BuiltInChatModeManager.INSTANCE.loadBuiltInModes(mockConnection).join());

    assertSame(failure, exception.getCause());
    assertEquals(List.of(publishedMode), BuiltInChatModeManager.INSTANCE.getBuiltInModes());
  }

  @Test
  void testLoadBuiltInModes_nullConnectionReturnsEmptyWithoutPublishing() {
    BuiltInChatMode publishedMode = createBuiltInChatMode("Ask");
    BuiltInChatModeManager.INSTANCE.updateModes(List.of(publishedMode));

    assertEquals(List.of(), BuiltInChatModeManager.INSTANCE.loadBuiltInModes(null).join());
    assertEquals(List.of(publishedMode), BuiltInChatModeManager.INSTANCE.getBuiltInModes());
    verifyNoInteractions(mockConnection);
  }

  @Test
  void testLoadBuiltInModes_conversionFailureSkipsMode() {
    ConversationMode invalidMode = mock(ConversationMode.class);
    when(invalidMode.isBuiltIn()).thenReturn(true);
    when(invalidMode.getKind()).thenReturn("Agent");
    when(invalidMode.getName()).thenReturn("Agent");
    when(invalidMode.getId()).thenReturn("invalid");
    when(invalidMode.getDescription()).thenThrow(new IllegalArgumentException("invalid mode"));
    when(mockConnection.listConversationModes(any(ConversationModesParams.class)))
        .thenReturn(CompletableFuture.completedFuture(new ConversationMode[] { invalidMode }));

    assertEquals(List.of(), BuiltInChatModeManager.INSTANCE.loadBuiltInModes(mockConnection).join());
  }

  @Test
  void testUpdateModes_publishesDefensiveSnapshot() {
    BuiltInChatMode mode = createBuiltInChatMode("Plan");
    List<BuiltInChatMode> source = new ArrayList<>(List.of(mode));

    BuiltInChatModeManager.INSTANCE.updateModes(source);
    source.clear();
    List<BuiltInChatMode> snapshot = BuiltInChatModeManager.INSTANCE.getBuiltInModes();
    snapshot.clear();

    assertEquals(List.of(mode), BuiltInChatModeManager.INSTANCE.getBuiltInModes());
  }

  private BuiltInChatMode createBuiltInChatMode(String name) {
    return new BuiltInChatMode(createConversationMode(name, name, name));
  }

  private ConversationMode createConversationMode(String id, String name, String kind) {
    ConversationMode mode = new ConversationMode();
    mode.setId(id);
    mode.setName(name);
    mode.setKind(kind);
    mode.setBuiltIn(true);
    mode.setDescription(name + " description");
    return mode;
  }
}

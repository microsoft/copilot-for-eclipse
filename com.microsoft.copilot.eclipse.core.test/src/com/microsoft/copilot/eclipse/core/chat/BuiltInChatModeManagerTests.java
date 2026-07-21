// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.chat.service.BuiltInChatModeService;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;

@ExtendWith(MockitoExtension.class)
class BuiltInChatModeManagerTests {

  @Mock
  private BuiltInChatModeService mockService;

  private BuiltInChatModeManager manager;

  @BeforeEach
  void setUp() {
    manager = new BuiltInChatModeManager(mockService);
  }

  @Test
  void testReloadModes_pendingLoad_returnsImmediatelyAndPublishesOnCompletion() {
    CompletableFuture<List<BuiltInChatMode>> pendingModes = new CompletableFuture<>();
    when(mockService.loadBuiltInModes()).thenReturn(pendingModes);

    CompletableFuture<Void> reload = assertTimeoutPreemptively(Duration.ofSeconds(1),
        manager::reloadModes);

    assertFalse(reload.isDone());
    assertTrue(manager.getBuiltInModes().isEmpty());

    BuiltInChatMode agentMode = createBuiltInMode(BuiltInChatMode.AGENT_MODE_NAME);
    pendingModes.complete(List.of(agentMode));
    reload.join();

    assertEquals(List.of(agentMode), manager.getBuiltInModes());
  }

  @Test
  void testReloadModes_olderRequestCompletesLast_keepsLatestResult() {
    CompletableFuture<List<BuiltInChatMode>> olderModes = new CompletableFuture<>();
    CompletableFuture<List<BuiltInChatMode>> latestModes = new CompletableFuture<>();
    when(mockService.loadBuiltInModes()).thenReturn(olderModes, latestModes);

    CompletableFuture<Void> olderReload = manager.reloadModes();
    CompletableFuture<Void> latestReload = manager.reloadModes();

    BuiltInChatMode agentMode = createBuiltInMode(BuiltInChatMode.AGENT_MODE_NAME);
    latestModes.complete(List.of(agentMode));
    latestReload.join();
    assertEquals(List.of(agentMode), manager.getBuiltInModes());

    olderModes.complete(List.of(createBuiltInMode(BuiltInChatMode.ASK_MODE_NAME)));
    olderReload.join();

    assertEquals(List.of(agentMode), manager.getBuiltInModes());
  }

  @Test
  void testClearModes_inFlightReloadCompletes_keepsCacheEmpty() {
    BuiltInChatMode agentMode = createBuiltInMode(BuiltInChatMode.AGENT_MODE_NAME);
    CompletableFuture<List<BuiltInChatMode>> pendingModes = new CompletableFuture<>();
    when(mockService.loadBuiltInModes())
        .thenReturn(CompletableFuture.completedFuture(List.of(agentMode)), pendingModes);
    manager.reloadModes().join();
    assertEquals(List.of(agentMode), manager.getBuiltInModes());

    CompletableFuture<Void> reload = manager.reloadModes();
    manager.clearModes();
    assertTrue(manager.getBuiltInModes().isEmpty());

    pendingModes.complete(List.of(createBuiltInMode(BuiltInChatMode.ASK_MODE_NAME)));
    reload.join();

    assertTrue(manager.getBuiltInModes().isEmpty());
  }

  private BuiltInChatMode createBuiltInMode(String name) {
    ConversationMode mode = new ConversationMode();
    mode.setId(name);
    mode.setName(name);
    mode.setKind(name);
    mode.setDescription(name + " mode");
    return new BuiltInChatMode(mode);
  }
}

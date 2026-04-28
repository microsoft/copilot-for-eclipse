// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationAgent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;

class ChatCompletionServiceTest {

  private static CopilotLanguageServerConnection mockLsConnection;

  private static AuthStatusManager mockAuthStatusManager;

  private static ChatCompletionService chatCompletionService;
  private static MockedStatic<CopilotUi> copilotUiMock;

  @BeforeAll
  static void setUp() {
    // Initialize the mocks
    mockLsConnection = Mockito.mock(CopilotLanguageServerConnection.class);
    mockAuthStatusManager = Mockito.mock(AuthStatusManager.class);

    // Mock CopilotUi.getPlugin() so the constructor can register its preference listener
    CopilotUi mockPlugin = mock(CopilotUi.class);
    LanguageServerSettingManager mockSettingManager = mock(LanguageServerSettingManager.class);
    when(mockPlugin.getLanguageServerSettingManager()).thenReturn(mockSettingManager);
    copilotUiMock = Mockito.mockStatic(CopilotUi.class);
    copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);

    ConversationTemplate template = new ConversationTemplate("test", null, null,
        List.of(CopilotScope.CHAT_PANEL), null);
    ConversationTemplate[] templates = new ConversationTemplate[] { template };
    when(mockLsConnection.listConversationTemplates(any())).thenReturn(CompletableFuture.completedFuture(templates));
    when(mockLsConnection.listConversationAgents())
        .thenReturn(CompletableFuture.completedFuture(new ConversationAgent[0]));
    when(mockAuthStatusManager.getCopilotStatus()).thenReturn(CopilotStatusResult.OK);
    chatCompletionService = new ChatCompletionService(mockLsConnection, mockAuthStatusManager);
    try {
      Job.getJobManager().join(ChatCompletionService.REFRESH_JOB_FAMILY, null);
    } catch (InterruptedException e) {
      // ignore
    }
  }

  @AfterAll
  static void tearDown() {
    if (copilotUiMock != null) {
      copilotUiMock.close();
    }
  }

  @Test
  void testConstructor() {
    boolean result = chatCompletionService.isTempaltesReady();
    assertTrue(result);
  }

  @Test
  void testInitConversationTemplates() throws Exception {
    assertEquals(1, chatCompletionService.getFilteredTemplates(ChatMode.Ask).length);
  }

  @Test
  void testIsBrokenCommand() {
    assertTrue(chatCompletionService.isBrokenCommand("/tes", 4));
    assertFalse(chatCompletionService.isBrokenCommand("/tes", 3));
  }

  @Test
  void testIsCommand() {
    assertTrue(chatCompletionService.isCommand("/test"));
    assertFalse(chatCompletionService.isCommand("/invalid"));
  }

  @Test
  void testGetFilteredTemplates() {
    assertNotNull(chatCompletionService.getFilteredTemplates(ChatMode.Ask));
  }
}
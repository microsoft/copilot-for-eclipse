// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

class ChatCompletionServiceTest {

  @Mock
  private static CopilotLanguageServerConnection mockLsConnection;

  @Mock
  private static ConversationTemplate mockTemplate;

  @Mock
  private static AuthStatusManager mockAuthStatusManager;

  private static ChatCompletionService chatCompletionService;

  @BeforeAll
  static void setUp() {
    // Initialize the mocks
    mockLsConnection = Mockito.mock(CopilotLanguageServerConnection.class);
    mockTemplate = Mockito.mock(ConversationTemplate.class);
    mockAuthStatusManager = Mockito.mock(AuthStatusManager.class);
    ConversationTemplate[] templates = new ConversationTemplate[] { mockTemplate };
    when(mockLsConnection.listConversationTemplates()).thenReturn(CompletableFuture.completedFuture(templates));
    when(mockTemplate.getScopes()).thenReturn(List.of(CopilotScope.CHAT_PANEL));
    when(mockTemplate.getId()).thenReturn("test");
    when(mockAuthStatusManager.getCopilotStatus()).thenReturn(CopilotStatusResult.OK);
    chatCompletionService = new ChatCompletionService(mockLsConnection, mockAuthStatusManager);
    Job[] jobs = Job.getJobManager().find(ChatCompletionService.INIT_JOB_FAMILY);
    for (Job job : jobs) {
      try {
        job.join();
      } catch (InterruptedException e) {
        continue;
      }
    }
  }

  @Test
  void testConstructor() {
    boolean result = chatCompletionService.isTempaltesReady();
    assertTrue(result);
  }

  @Test
  void testInitConversationTemplates() throws Exception {
    assertEquals(1, chatCompletionService.getTemplates().length);
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
  void testGetTemplates() {
    assertNotNull(chatCompletionService.getTemplates());
  }
}
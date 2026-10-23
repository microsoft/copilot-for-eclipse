// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4e.LanguageServerWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CopilotLanguageServerConnectionTests {

  @Mock
  private LanguageServerWrapper languageServerWrapper;

  private CopilotLanguageServerConnection connection;

  @BeforeEach
  void setUp() {
    connection = new CopilotLanguageServerConnection(languageServerWrapper);
  }

  @Test
  void testCreateConversation_noActiveModel_failsWithoutRequest() {
    CompletableFuture<?> result = connection.createConversation("token", "hello", List.of(), null, null, null, null,
        null, null, "Ask", null, null, null, null, null, null, null);

    assertTrue(result.isCompletedExceptionally());
    verify(languageServerWrapper, never()).execute(any());
  }

  @Test
  void testAddConversationTurn_noActiveModel_failsWithoutRequest() {
    CompletableFuture<?> result = connection.addConversationTurn("token", "conversation", "hello", List.of(), null,
        null, null, null, null, "Ask", null, null, null, null, null);

    assertTrue(result.isCompletedExceptionally());
    verify(languageServerWrapper, never()).execute(any());
  }
}

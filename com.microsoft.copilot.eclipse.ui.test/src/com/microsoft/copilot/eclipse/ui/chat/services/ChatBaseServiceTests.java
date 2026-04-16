// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;

class ChatBaseServiceTests {

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @Mock
  private AuthStatusManager mockAuthStatusManager;

  @Mock
  private CopilotCore mockCopilotCore;

  private TestChatBaseService chatBaseService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    chatBaseService = new TestChatBaseService(mockLsConnection, mockAuthStatusManager);
  }

  @Test
  void persistUserPreference_WhenNotSignedIn_ShouldSkipLspInteraction() {
    // Arrange
    when(mockAuthStatusManager.isSignedIn()).thenReturn(false);

    // Act
    chatBaseService.persistUserPreference();

    // Assert
    verify(mockAuthStatusManager).isSignedIn();
    verifyNoInteractions(mockLsConnection); // LSP connection should not be used
  }

  /**
   * Test implementation of ChatBaseService for testing purposes
   */
  private static class TestChatBaseService extends ChatBaseService {
    public TestChatBaseService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
      super(lsConnection, authStatusManager);
    }

    public void persistUserPreference() {
      super.persistUserPreference();
    }
  }
}
package com.microsoft.copilot.eclipse.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AuthStatusResult;

@ExtendWith(MockitoExtension.class)
class AuthStatusManagerTests {

  @Mock
  CopilotLanguageServerConnection mockConnection;
  AuthStatusManager authStatusManager;

  @BeforeEach
  public void setUp() {
    authStatusManager = new AuthStatusManager(mockConnection);
  }

  @Test
  void testAuthStatusResultOnSuccess() {
    AuthStatusResult expectedResult = new AuthStatusResult();
    expectedResult.setStatus(AuthStatusResult.OK);
    when(mockConnection.checkStatus(false)).thenReturn(CompletableFuture.completedFuture(expectedResult));

    authStatusManager.checkStatus();

    assertEquals(AuthStatusResult.OK, authStatusManager.getAuthStatusResult().getStatus());

  }

}

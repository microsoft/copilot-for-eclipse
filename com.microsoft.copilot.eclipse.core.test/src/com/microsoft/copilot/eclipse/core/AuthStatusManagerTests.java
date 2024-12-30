package com.microsoft.copilot.eclipse.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

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
  void testCopilotStatusResultOnSuccess() {
    CopilotStatusResult expectedResult = new CopilotStatusResult();
    expectedResult.setStatus(CopilotStatusResult.OK);
    when(mockConnection.checkStatus(false)).thenReturn(CompletableFuture.completedFuture(expectedResult));

    authStatusManager.checkStatus();

    assertEquals(CopilotStatusResult.OK, authStatusManager.getCopilotStatus());
  }

  @Test
  void testCheckStatusOK() throws InterruptedException {
    String mockedUser = "mockedUser";
    // Arrange
    CopilotStatusResult expectedResult = new CopilotStatusResult();
    expectedResult.setStatus(CopilotStatusResult.OK);
    expectedResult.setUser(mockedUser);
    CompletableFuture<CopilotStatusResult> future = new CompletableFuture<>();
    when(mockConnection.checkStatus(false)).thenReturn(future);
    future.complete(expectedResult);

    authStatusManager.checkStatus();

    // Assert final status is OK
    assertEquals(CopilotStatusResult.OK, authStatusManager.getCopilotStatus());
  }

  @Test
  void testCheckStatusError() {
    CompletableFuture<CopilotStatusResult> future = new CompletableFuture<>();
    future.completeExceptionally(new CompletionException(new Exception("Some other error")));

    when(mockConnection.checkStatus(false)).thenReturn(future);

    authStatusManager.checkStatus();

    assertEquals(CopilotStatusResult.ERROR, authStatusManager.getCopilotStatus());
  }

}

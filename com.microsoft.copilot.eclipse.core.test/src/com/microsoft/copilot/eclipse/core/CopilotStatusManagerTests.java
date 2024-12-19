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
class CopilotStatusManagerTests {

  @Mock
  CopilotLanguageServerConnection mockConnection;
  CopilotStatusManager copilotStatusManager;

  @BeforeEach
  public void setUp() {
    copilotStatusManager = new CopilotStatusManager(mockConnection);
  }

  @Test
  void testCopilotStatusResultOnSuccess() {
    CopilotStatusResult expectedResult = new CopilotStatusResult();
    expectedResult.setStatus(CopilotStatusResult.OK);
    when(mockConnection.checkStatus(false)).thenReturn(CompletableFuture.completedFuture(expectedResult));

    copilotStatusManager.checkStatus();

    assertEquals(CopilotStatusResult.OK, copilotStatusManager.getCopilotStatusResult().getStatus());
  }

  @Test
  void testCheckStatusLoadingWithDelay() throws InterruptedException {
    String mockedUser = "mockedUser";
    // Arrange
    CopilotStatusResult expectedResult = new CopilotStatusResult();
    expectedResult.setStatus(CopilotStatusResult.OK);
    expectedResult.setUser(mockedUser);
    CompletableFuture<CopilotStatusResult> future = new CompletableFuture<>();

    when(mockConnection.checkStatus(false)).thenReturn(future);

    // Act
    copilotStatusManager.checkStatus();

    // Assert initial status is LOADING
    assertEquals(CopilotStatusResult.LOADING, copilotStatusManager.getCopilotStatusResult().getStatus());

    future.complete(expectedResult);

    // Assert final status is OK
    assertEquals(CopilotStatusResult.OK, copilotStatusManager.getCopilotStatusResult().getStatus());
    assertEquals(mockedUser, copilotStatusManager.getCopilotStatusResult().getUser());
  }

  @Test
  void testCheckStatusError() {
    CompletableFuture<CopilotStatusResult> future = new CompletableFuture<>();
    future.completeExceptionally(new CompletionException(new Exception("Some other error")));

    when(mockConnection.checkStatus(false)).thenReturn(future);

    copilotStatusManager.checkStatus();

    assertEquals(CopilotStatusResult.ERROR, copilotStatusManager.getCopilotStatusResult().getStatus());
  }

}

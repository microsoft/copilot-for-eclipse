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

  @Test
  void testCheckStatusLoadingWithDelay() throws InterruptedException {
    String mockedUser = "mockedUser";
    // Arrange
    AuthStatusResult expectedResult = new AuthStatusResult();
    expectedResult.setStatus(AuthStatusResult.OK);
    expectedResult.setUser(mockedUser);
    CompletableFuture<AuthStatusResult> future = new CompletableFuture<>();

    when(mockConnection.checkStatus(false)).thenReturn(future);

    // Act
    authStatusManager.checkStatus();

    // Assert initial status is LOADING
    assertEquals(AuthStatusResult.LOADING, authStatusManager.getAuthStatusResult().getStatus());

    future.complete(expectedResult);

    // Assert final status is OK
    assertEquals(AuthStatusResult.OK, authStatusManager.getAuthStatusResult().getStatus());
    assertEquals(mockedUser, authStatusManager.getAuthStatusResult().getUser());
  }

  @Test
  void testCheckStatusError() {
    CompletableFuture<AuthStatusResult> future = new CompletableFuture<>();
    future.completeExceptionally(new CompletionException(new Exception("Some other error")));

    when(mockConnection.checkStatus(false)).thenReturn(future);

    authStatusManager.checkStatus();

    assertEquals(AuthStatusResult.ERROR, authStatusManager.getAuthStatusResult().getStatus());
  }

}

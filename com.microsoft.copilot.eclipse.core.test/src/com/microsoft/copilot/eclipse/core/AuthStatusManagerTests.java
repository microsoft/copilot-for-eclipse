// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;

@ExtendWith(MockitoExtension.class)
class AuthStatusManagerTests {

  @Mock
  CopilotLanguageServerConnection mockConnection;
  AuthStatusManager authStatusManager;

  @BeforeEach
  void setUp() {
    authStatusManager = new AuthStatusManager(mockConnection);
  }

  @Test
  void testCheckStatus_AccountSwitchWithSameStatus_NotifiesOncePerAccount() {
    when(mockConnection.checkQuota()).thenReturn(CompletableFuture.completedFuture(CheckQuotaResult.empty()));
    List<String> accounts = new ArrayList<>();
    List<CopilotStatusResult> notifications = new ArrayList<>();
    authStatusManager.addCopilotAuthStatusListener(status -> accounts.add(status.getUser()));
    authStatusManager.addCopilotAuthStatusListener(notifications::add);
    CopilotStatusResult alice = new CopilotStatusResult();
    alice.setUser("alice");
    alice.setStatus(CopilotStatusResult.OK);
    CopilotStatusResult bob = new CopilotStatusResult();
    bob.setUser("bob");
    bob.setStatus(CopilotStatusResult.OK);
    when(mockConnection.checkStatus(false))
        .thenReturn(CompletableFuture.completedFuture(alice), CompletableFuture.completedFuture(bob));

    authStatusManager.checkStatus();
    authStatusManager.checkStatus();
    authStatusManager.checkStatus();

    assertEquals(List.of("alice", "bob"), accounts);
    assertEquals("alice", notifications.get(0).getUser(), "Queued notifications retain their original identity");
    assertEquals("bob", authStatusManager.getUserName());
  }

  @Test
  void testSignInInitiate() throws InterruptedException, ExecutionException {
    SignInInitiateResult mockResult = mock(SignInInitiateResult.class);
    when(mockResult.isAlreadySignedIn()).thenReturn(true);
    when(mockConnection.signInInitiate()).thenReturn(CompletableFuture.completedFuture(mockResult));

    SignInInitiateResult result = authStatusManager.signInInitiate();

    assertEquals(CopilotStatusResult.OK, authStatusManager.getCopilotStatus());
    assertEquals(mockResult, result);
  }

  @Test
  void testSignInConfirm() throws InterruptedException, ExecutionException {
    String userCode = "testUserCode";
    String mockedUser = "mockedUser";
    CopilotStatusResult mockResult = mock(CopilotStatusResult.class);
    when(mockResult.getUser()).thenReturn(mockedUser);
    when(mockResult.getStatus()).thenReturn(CopilotStatusResult.OK);
    when(mockConnection.signInConfirm(userCode)).thenReturn(CompletableFuture.completedFuture(mockResult));
    when(mockConnection.checkQuota()).thenReturn(CompletableFuture.completedFuture(CheckQuotaResult.empty()));

    CopilotStatusResult result = authStatusManager.signInConfirm(userCode);

    assertEquals(mockedUser, authStatusManager.getUserName());
    assertEquals(CopilotStatusResult.OK, authStatusManager.getCopilotStatus());
    assertEquals(mockResult, result);
  }

  @Test
  void testSignOut() throws InterruptedException, ExecutionException {
    CopilotStatusResult mockResult = mock(CopilotStatusResult.class);
    when(mockResult.getStatus()).thenReturn(CopilotStatusResult.NOT_SIGNED_IN);
    when(mockConnection.signOut()).thenReturn(CompletableFuture.completedFuture(mockResult));

    CopilotStatusResult result = authStatusManager.signOut();

    assertTrue(authStatusManager.getUserName().isEmpty());
    assertEquals(CopilotStatusResult.NOT_SIGNED_IN, authStatusManager.getCopilotStatus());
    assertEquals(mockResult, result);
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
  void testCheckStatusOk() throws InterruptedException {
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
    assertEquals(mockedUser, authStatusManager.getUserName());
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

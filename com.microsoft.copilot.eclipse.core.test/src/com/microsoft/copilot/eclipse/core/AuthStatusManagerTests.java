package com.microsoft.copilot.eclipse.core;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AuthStatusResult;

@RunWith(MockitoJUnitRunner.class)
public class AuthStatusManagerTests {

  @Mock
  CopilotLanguageServerConnection mockConnection;
  AuthStatusManager authStatusManager;

  @Before
  public void setUp() {
    authStatusManager = new AuthStatusManager(mockConnection);
  }

  @Test
  public void testAuthStatusResultOnSuccess() {
    AuthStatusResult expectedResult = new AuthStatusResult();
    expectedResult.setStatus(AuthStatusResult.OK);
    when(mockConnection.checkStatus(false)).thenReturn(CompletableFuture.completedFuture(expectedResult));

    authStatusManager.checkStatus();

    assertEquals(AuthStatusResult.OK, authStatusManager.getAuthStatusResult().getStatus());

  }

}

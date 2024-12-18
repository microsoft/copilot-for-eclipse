package com.microsoft.copilot.eclipse.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AuthStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;

/**
 * Manager for the authentication status.
 */
public class AuthStatusManager {

  private CopilotLanguageServerConnection connection;

  private AuthStatusResult authStatusResult;

  private static final int CHECK_STATUS_TIMEOUT_MILLIS = 3000;
  
  /**
   * Constructor for the AuthStatusManager.
   *
   * @param connection the connection to the language server.
   */
  public AuthStatusManager(CopilotLanguageServerConnection connection) {
    this.connection = connection;
    this.authStatusResult = new AuthStatusResult();
    this.authStatusResult.setStatus(AuthStatusResult.LOADING);
  }

  /**
   * Initiate the sign in process.

   * @throws ExecutionException if the sign in initiate process fails due to an execution error
   * @throws InterruptedException if the sign in initiate process is interrupted
   */
  public SignInInitiateResult signInInitiate() throws InterruptedException, ExecutionException {
    SignInInitiateResult result = connection.signInInitiate().get();
    if (result.isAlreadySignedIn()) {
      this.authStatusResult.setStatus(AuthStatusResult.OK);
    }
    return result;
  }

  /**
   * Confirm the sign in process.

   * @throws ExecutionException if the sign in process fails due to an execution error
   * @throws InterruptedException if the sign in process is interrupted
   */
  public AuthStatusResult signInConfirm(String userCode) throws InterruptedException, ExecutionException {
    AuthStatusResult result = connection.signInConfirm(userCode).get();
    if (result.isSignedIn()) {
      this.authStatusResult.setStatus(AuthStatusResult.OK);
      this.authStatusResult.setUser(result.getUser());
    }
    return result;
  }

  /**
   * Sign out from the GitHub Copilot.

   * @throws ExecutionException if the sign out process fails due to an execution error
   * @throws InterruptedException if the sign out process is interrupted
   */
  public AuthStatusResult signOut() throws InterruptedException, ExecutionException {
    AuthStatusResult result = connection.signOut().get();
    if (!result.isSignedIn()) {
      this.authStatusResult.setStatus(AuthStatusResult.NOT_SIGNED_IN);
    }
    return result;
  }

  /**
   * Check the login status for current machine.
   */
  public void checkStatus() {
    CompletableFuture<AuthStatusResult> statusFuture = this.connection.checkStatus(false);

    statusFuture.orTimeout(CHECK_STATUS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).thenAccept(result -> {
      this.authStatusResult = result;
    }).exceptionally(ex -> {
      // TODO: log & send telemetry
      this.authStatusResult.setStatus(AuthStatusResult.ERROR);
      
      return null;
    });
  }

  public AuthStatusResult getAuthStatusResult() {
    return this.authStatusResult;
  }
}

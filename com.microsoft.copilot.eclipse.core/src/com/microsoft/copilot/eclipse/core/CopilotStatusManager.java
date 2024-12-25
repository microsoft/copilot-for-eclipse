package com.microsoft.copilot.eclipse.core;

import java.util.concurrent.ExecutionException;

import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;

/**
 * Manager for the authentication status.
 */
public class CopilotStatusManager {

  private CopilotLanguageServerConnection connection;

  private CopilotStatusResult copilotStatusResult;

  /**
   * Constructor for the CopilotStatusManager.
   *
   * @param connection the connection to the language server.
   */
  public CopilotStatusManager(CopilotLanguageServerConnection connection) {
    this.connection = connection;
    this.copilotStatusResult = new CopilotStatusResult();
    this.copilotStatusResult.setStatus(CopilotStatusResult.OK);
  }

  /**
   * Initiate the sign in process.
   *
   * @throws ExecutionException if the sign in initiate process fails due to an execution error
   * @throws InterruptedException if the sign in initiate process is interrupted
   */
  public SignInInitiateResult signInInitiate() throws InterruptedException, ExecutionException {
    SignInInitiateResult result = connection.signInInitiate().get();
    if (result.isAlreadySignedIn()) {
      this.copilotStatusResult.setStatus(CopilotStatusResult.OK);
    }
    return result;
  }

  /**
   * Confirm the sign in process.
   *
   * @throws ExecutionException if the sign in process fails due to an execution error
   * @throws InterruptedException if the sign in process is interrupted
   */
  public CopilotStatusResult signInConfirm(String userCode) throws InterruptedException, ExecutionException {
    CopilotStatusResult result = connection.signInConfirm(userCode).get();
    if (result.isSignedIn()) {
      this.copilotStatusResult.setStatus(CopilotStatusResult.OK);
      this.copilotStatusResult.setUser(result.getUser());
    }
    return result;
  }

  /**
   * Sign out from the GitHub Copilot.
   *
   * @throws ExecutionException if the sign out process fails due to an execution error
   * @throws InterruptedException if the sign out process is interrupted
   */
  public CopilotStatusResult signOut() throws InterruptedException, ExecutionException {
    CopilotStatusResult result = connection.signOut().get();
    if (!result.isSignedIn()) {
      this.copilotStatusResult.setStatus(CopilotStatusResult.NOT_SIGNED_IN);
    }
    return result;
  }
  
  /**
   * Set the status to OK.
   */
  public CopilotStatusResult setCompletionDone() {
    this.copilotStatusResult.setStatus(CopilotStatusResult.OK);
    return this.copilotStatusResult;
  }

  /**
   * Check the login status for current machine.
   */
  public void checkStatus() {
    this.connection.checkStatus(false).thenAccept(result -> {
      this.copilotStatusResult = result;
    }).exceptionally(ex -> {
      CopilotCore.LOGGER.log(LogLevel.ERROR, ex);
      this.copilotStatusResult.setStatus(CopilotStatusResult.ERROR);

      return null;
    });
  }

  /**
   * Get the current status of the copilot.
   */
  public String getCopilotStatus() {
    if (this.copilotStatusResult == null) {
      return CopilotStatusResult.LOADING;
    }
    return this.copilotStatusResult.getStatus();
  }
}

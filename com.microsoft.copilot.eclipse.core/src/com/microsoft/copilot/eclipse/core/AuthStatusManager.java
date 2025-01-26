package com.microsoft.copilot.eclipse.core;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;

/**
 * Manager for the authentication status.
 */
public class AuthStatusManager {

  private CopilotLanguageServerConnection connection;
  private Set<CopilotAuthStatusListener> copilotAuthStatusListeners;
  private CopilotStatusResult copilotStatusResult;

  /**
   * Constructor for the AuthStatusManager.
   *
   * @param connection the connection to the language server.
   */
  public AuthStatusManager(CopilotLanguageServerConnection connection) {
    this.connection = connection;
    this.copilotAuthStatusListeners = new LinkedHashSet<>();
    this.copilotStatusResult = new CopilotStatusResult();
    setCopilotStatus(CopilotStatusResult.LOADING);
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
      setCopilotStatus(CopilotStatusResult.OK);
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
      this.copilotStatusResult.setUser(result.getUser());
    }

    setCopilotStatus(result.getStatus());
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
    setCopilotStatus(result.getStatus());
    return result;
  }

  /**
   * Set the CopilotStatusResult string to the given status and notify the listeners.
   */
  public CopilotStatusResult setCopilotStatus(String newCopilotStatusResult) {
    if (!Objects.equals(this.copilotStatusResult.getStatus(), newCopilotStatusResult)) {
      this.copilotStatusResult.setStatus(newCopilotStatusResult);
      onDidCopilotStatusChange(this.copilotStatusResult);
    }
    return this.copilotStatusResult;
  }

  /**
   * Check the authentication status for current machine.
   */
  public void checkStatus() {
    this.connection.checkStatus(false).handle((result, ex) -> {
      if (ex != null) {
        CopilotCore.LOGGER.error(ex);
        setCopilotStatus(CopilotStatusResult.ERROR);
      } else {
        setCopilotStatus(result.getStatus());
        this.copilotStatusResult.setUser(result.getUser());
      }
      onDidCopilotStatusChange(this.copilotStatusResult);
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

  /**
   * Get the name of the login user.
   */
  public String getUserName() {
    if (this.copilotStatusResult == null) {
      return "";
    }
    if (this.copilotStatusResult.getUser() == null) {
      return "";
    }
    return this.copilotStatusResult.getUser();
  }

  /**
   * Add a listener for the authentication status.
   */
  public void addCopilotAuthStatusListener(CopilotAuthStatusListener listener) {
    this.copilotAuthStatusListeners.add(listener);
  }

  /**
   * Remove the listener for the authentication status.
   */
  public void removeCopilotAuthStatusListener(CopilotAuthStatusListener listener) {
    this.copilotAuthStatusListeners.remove(listener);
  }

  public boolean isNotSignedInOrNotAuthorized() {
    return this.copilotStatusResult.isNotSignedIn() || this.copilotStatusResult.isNotAuthorized();
  }

  private void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult) {
    if (!this.copilotAuthStatusListeners.isEmpty()) {
      for (CopilotAuthStatusListener listener : this.copilotAuthStatusListeners) {
        listener.onDidCopilotStatusChange(copilotStatusResult);
      }
    }
  }
}

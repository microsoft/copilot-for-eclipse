package com.microsoft.copilot.eclipse.core;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AuthStatusResult;

/**
 * Manager for the authentication status.
 */
public class AuthStatusManager {

  private CopilotLanguageServerConnection connection;

  private AuthStatusResult authStatusResult;

  /**
   * Constructor for the AuthStatusManager.
   *
   * @param connection the connection to the language server.
   */
  public AuthStatusManager(CopilotLanguageServerConnection connection) {
    this.connection = connection;
    this.authStatusResult = new AuthStatusResult();
    this.authStatusResult.setStatus(AuthStatusResult.NOT_SIGNED_IN);
  }

  /**
   * Check the login status for current machine.
   */
  public void checkStatus() {
    this.connection.checkStatus(false).thenAccept(result -> {
      this.authStatusResult = result;
    }).exceptionally(ex -> {
      // TODO: log & send telemetry
      this.authStatusResult.setStatus(AuthStatusResult.ERROR);
      return null;
    });
  }

  public AuthStatusResult getAuthStatusResult() {
    return authStatusResult;
  }
}

package com.microsoft.copilot.eclipse.core.lsp;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CheckStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyAcceptedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyRejectedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyShownParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NullParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInConfirmParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;

/**
 * Language Server for Copilot agent.
 */
@SuppressWarnings({ "restriction" })
public class CopilotLanguageServerConnection {

  public static final String SERVER_ID = "com.microsoft.copilot.eclipse.ls";

  private LanguageServerWrapper languageServerWrapper;

  /**
   * Constructor for the CopilotLanguageServer.
   *
   * @param languageServerWrapper the language server wrapper.
   */
  public CopilotLanguageServerConnection(LanguageServerWrapper languageServerWrapper) {
    this.languageServerWrapper = languageServerWrapper;
  }

  /**
   * Connect the document to the language server. The LSP4E will take care of all the document lifecycle events after
   * that.
   */
  public void connectDocument(IDocument document) throws IOException {
    this.languageServerWrapper.connectDocument(document);
  }

  /**
   * Disconnect the document from the language server.
   */
  public void disconnectDocument(URI uri) {
    this.languageServerWrapper.disconnect(uri);
  }

  /**
   * Get the document version for the given URI.
   */
  public int getDocumentVersion(URI uri) {
    return this.languageServerWrapper.getTextDocumentVersion(uri);
  }

  /**
   * Check the login status for current machine.
   */
  public CompletableFuture<CopilotStatusResult> checkStatus(Boolean localCheckOnly) {
    Function<LanguageServer, CompletableFuture<CopilotStatusResult>> fn = server -> {
      CheckStatusParams param = new CheckStatusParams();
      param.setLocalChecksOnly(localCheckOnly);
      return ((CopilotLanguageServer) server).checkStatus(param);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Get single completion for the given parameters.
   */
  public CompletableFuture<CompletionResult> getCompletions(CompletionParams params) {
    Function<LanguageServer, CompletableFuture<CompletionResult>> fn = server -> ((CopilotLanguageServer) server)
        .getCompletions(params);
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Update the configuration for the language server.
   */
  public void updateConfig(DidChangeConfigurationParams params) {
    this.languageServerWrapper.sendNotification(server -> server.getWorkspaceService().didChangeConfiguration(params));
  }

  /**
   * Please use the {@link CopilotStatusManager#signInInitiate()} method instead.
   * </p>
   * Initiate the sign in process.
   */
  public CompletableFuture<SignInInitiateResult> signInInitiate() {
    Function<LanguageServer, CompletableFuture<SignInInitiateResult>> fn = (server) -> ((CopilotLanguageServer) server)
        .signInInitiate(new NullParams());
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Please use the {@link AuthStatusManager#signInConfirm()} method instead.
   * </p>
   * Confirm the sign in process.
   */
  public CompletableFuture<CopilotStatusResult> signInConfirm(String userCode) {
    Function<LanguageServer, CompletableFuture<CopilotStatusResult>> fn = (server) -> {
      SignInConfirmParams param = new SignInConfirmParams(userCode);
      return ((CopilotLanguageServer) server).signInConfirm(param);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Please use the {@link AuthStatusManager#signOut()} method instead.
   * </p>
   * Sign out from the GitHub Copilot.
   */
  public CompletableFuture<CopilotStatusResult> signOut() {
    Function<LanguageServer, CompletableFuture<CopilotStatusResult>> fn = (server) -> ((CopilotLanguageServer) server)
        .signOut(new NullParams());
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Notify the language server that the completion was shown.
   */
  public CompletableFuture<String> notifyShown(NotifyShownParams params) {
    Function<LanguageServer, CompletableFuture<String>> fn = server -> ((CopilotLanguageServer) server)
        .notifyShown(params);
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Notify the language server that the completion was accepted.
   */
  public CompletableFuture<String> notifyAccepted(NotifyAcceptedParams params) {
    Function<LanguageServer, CompletableFuture<String>> fn = server -> ((CopilotLanguageServer) server)
        .notifyAccepted(params);
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Notify the language server that the completion was rejected.
   */
  public CompletableFuture<String> notifyRejected(NotifyRejectedParams params) {
    Function<LanguageServer, CompletableFuture<String>> fn = server -> ((CopilotLanguageServer) server)
        .notifyRejected(params);
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Stop the language server.
   */
  public void stop() {
    this.languageServerWrapper.stop();
  }

}

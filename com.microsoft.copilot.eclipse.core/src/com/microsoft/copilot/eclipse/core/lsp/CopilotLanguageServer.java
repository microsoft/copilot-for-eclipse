package com.microsoft.copilot.eclipse.core.lsp;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.lsp.protocol.AuthStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CheckStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyAcceptedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyRejectedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyShownParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NullParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInConfirmParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;

/**
 * Interface for Copilot Language Server.
 */
public interface CopilotLanguageServer extends LanguageServer {

  /**
   * Check the login status for current machine.
   */
  @JsonRequest
  CompletableFuture<AuthStatusResult> checkStatus(CheckStatusParams param);

  /**
   * Get single completion for the given parameters.
   */
  @JsonRequest
  CompletableFuture<CompletionResult> getCompletions(CompletionParams params);

  /**
   * Initiate the sign in process.
   */
  @JsonRequest
  CompletableFuture<SignInInitiateResult> signInInitiate(NullParams param);

  /**
   * Confirm the sign in process.
   */
  @JsonRequest
  CompletableFuture<AuthStatusResult> signInConfirm(SignInConfirmParams param);

  /**
   * Sign out the current user.
   */
  @JsonRequest
  CompletableFuture<AuthStatusResult> signOut(NullParams params);

  /**
   * Notify the language server that the completion was shown.
   */
  @JsonRequest
  CompletableFuture<String> notifyShown(NotifyShownParams params);

  /**
   * Notify the language server that the completion was accepted.
   */
  @JsonRequest
  CompletableFuture<String> notifyAccepted(NotifyAcceptedParams params);

  /**
   * Notify the language server that the completion was rejected.
   */
  @JsonRequest
  CompletableFuture<String> notifyRejected(NotifyRejectedParams params);
}

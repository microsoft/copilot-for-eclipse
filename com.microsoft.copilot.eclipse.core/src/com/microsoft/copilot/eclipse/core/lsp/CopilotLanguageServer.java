package com.microsoft.copilot.eclipse.core.lsp;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.lsp.protocol.AuthStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CheckStatusParams;

/**
 * Interface for Copilot Language Server.
 */
public interface CopilotLanguageServer extends LanguageServer {

  /**
   * Check the login status for current machine.
   */
  @JsonRequest
  CompletableFuture<AuthStatusResult> checkStatus(CheckStatusParams param);

}

package com.microsoft.copilot.eclipse.core.lsp;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.lsp.protocol.AuthStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CheckStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;

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
  public CompletableFuture<AuthStatusResult> checkStatus(Boolean localCheckOnly) {
    Function<LanguageServer, CompletableFuture<AuthStatusResult>> fn = server -> {
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
   * Stop the language server.
   */
  public void stop() {
    this.languageServerWrapper.stop();
  }

}

package com.microsoft.copilot.eclipse.core.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressParamsAdapter;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatReferenceTypeAdapter;

/**
 * Builder for Copilot Language Server.
 */
public class CopilotLauncherBuilder<T extends LanguageServer> extends Launcher.Builder<T> {

  /**
   * Create a new CopilotLauncherBuilder.
   */
  public CopilotLauncherBuilder() {
    this.configureGson(gsonBuilder -> gsonBuilder.registerTypeAdapterFactory(new ChatProgressParamsAdapter.Factory())
        .registerTypeAdapterFactory(new ChatReferenceTypeAdapter.Factory()));
  }

}

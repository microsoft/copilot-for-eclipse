// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatReferenceTypeAdapter;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ProgressParamsAdapter;

/**
 * Builder for Copilot Language Server.
 */
public class CopilotLauncherBuilder<T extends LanguageServer> extends Launcher.Builder<T> {

  /**
   * Create a new CopilotLauncherBuilder.
   */
  public CopilotLauncherBuilder() {
    this.configureGson(gsonBuilder -> gsonBuilder.registerTypeAdapterFactory(new ProgressParamsAdapter.Factory())
        .registerTypeAdapterFactory(new ChatReferenceTypeAdapter.Factory()));
  }

}

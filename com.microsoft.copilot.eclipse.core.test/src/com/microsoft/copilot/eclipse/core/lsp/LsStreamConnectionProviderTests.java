// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.InitializationOptions;

class LsStreamConnectionProviderTests {

  @Test
  void testInitializationOptions() {
    LsStreamConnectionProvider provider = new LsStreamConnectionProvider();

    InitializationOptions options = (InitializationOptions) provider.getInitializationOptions(null);

    assertEquals(LsStreamConnectionProvider.EDITOR_NAME, options.getEditorInfo().getName());
    assertEquals(LsStreamConnectionProvider.EDITOR_PLUGIN_NAME, options.getEditorPluginInfo().getName());
  }

  @Test
  void testStartLanguageServer() throws IOException {
    LsStreamConnectionProvider provider = new LsStreamConnectionProvider();
    try {
      provider.start();
    } finally {
      provider.stop();
    }
  }
}

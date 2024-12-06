package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.InitializationOptions;

public class LsStreamConnectionProviderTests {

  @Test
  public void testInitializationOptions() {
    LsStreamConnectionProvider provider = new LsStreamConnectionProvider();

    InitializationOptions options = (InitializationOptions) provider.getInitializationOptions(null);

    assertEquals(LsStreamConnectionProvider.EDITOR_NAME, options.getEditorInfo().getName());
    assertEquals(LsStreamConnectionProvider.EDITOR_PLUGIN_NAME, options.getEditorPluginInfo().getName());
  }

  @Test
  public void testStartLanguageServer() throws IOException {
    LsStreamConnectionProvider provider = new LsStreamConnectionProvider();
    try {
      provider.start();
    } finally {
      provider.stop();
    }
  }
}

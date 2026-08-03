// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.InitializationOptions;

class LsStreamConnectionProviderTests {

  private static final String LEGACY_WORKSPACE_CONTEXT_PREFERENCE = "workspaceContextEnabled";
  private static final String UI_PREFERENCE_NODE = "com.microsoft.copilot.eclipse.ui";

  @Test
  void testInitializationOptions() {
    LsStreamConnectionProvider provider = new LsStreamConnectionProvider();

    InitializationOptions options = (InitializationOptions) provider.getInitializationOptions(null);

    assertEquals(LsStreamConnectionProvider.EDITOR_NAME, options.getEditorInfo().getName());
    assertEquals(LsStreamConnectionProvider.EDITOR_PLUGIN_NAME, options.getEditorPluginInfo().getName());
  }

  @Test
  void testInitializationIgnoresLegacyWorkspaceContextPreference() {
    IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(UI_PREFERENCE_NODE);
    String previousValue = preferences.get(LEGACY_WORKSPACE_CONTEXT_PREFERENCE, null);
    preferences.putBoolean(LEGACY_WORKSPACE_CONTEXT_PREFERENCE, true);

    try {
      LsStreamConnectionProvider provider = new LsStreamConnectionProvider();

      InitializationOptions options = (InitializationOptions) provider.getInitializationOptions(null);

      assertFalse(options.getCopilotCapabilities().isWatchedFiles());
    } finally {
      if (previousValue == null) {
        preferences.remove(LEGACY_WORKSPACE_CONTEXT_PREFERENCE);
      } else {
        preferences.put(LEGACY_WORKSPACE_CONTEXT_PREFERENCE, previousValue);
      }
    }
  }

  @Test
  void testStartLanguageServer() throws IOException {
    TestableLsStreamConnectionProvider provider = new TestableLsStreamConnectionProvider();
    Process process;
    try {
      provider.start();
      process = provider.process();
      assertNotNull(process, "starting the provider must create a language server process");
    } finally {
      provider.stop();
    }

    assertFalse(process.isAlive(), "the language server process must not outlive stop()");
  }

  /**
   * A language server that inherits this JVM's stderr keeps that file descriptor open for as long as
   * it lives. When the server outlives the IDE - which happens because LSP4E tears servers down
   * asynchronously - whoever reads the other end of that stream waits forever; in a Tycho test fork
   * that reader is Maven, and the build hangs long after the tests have finished.
   */
  @Test
  void testCreateProcessBuilderDoesNotInheritErrorStream() {
    LsStreamConnectionProvider provider = new LsStreamConnectionProvider();
    provider.setCommands(List.of("copilot-language-server", "--stdio"));

    ProcessBuilder builder = provider.createProcessBuilder();

    assertNotEquals(ProcessBuilder.Redirect.INHERIT, builder.redirectError());
  }

  /**
   * Exposes the language server process so tests can assert on the real operating system process.
   */
  private static final class TestableLsStreamConnectionProvider extends LsStreamConnectionProvider {
    Process process() {
      return getProcess();
    }
  }
}

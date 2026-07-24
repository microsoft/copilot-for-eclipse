// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

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
    LsStreamConnectionProvider provider = new LsStreamConnectionProvider();
    try {
      provider.start();
    } finally {
      provider.stop();
    }
  }
}

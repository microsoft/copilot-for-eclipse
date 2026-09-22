// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InitializationOptions;

class LsStreamConnectionProviderTests {

  private static final String LEGACY_WORKSPACE_CONTEXT_PREFERENCE = "workspaceContextEnabled";
  private static final String UI_PREFERENCE_NODE = "com.microsoft.copilot.eclipse.ui";
  private static final long TERMINATION_GRACE_MS = 2000L;

  @Test
  void testHandleMessage_Initialized_EmitsOncePerProviderAndIgnoresStoppedProviders() {
    IEventBroker broker = mock(IEventBroker.class);
    IEclipseContext context = mock(IEclipseContext.class);
    when(context.get(IEventBroker.class)).thenReturn(broker);
    when(broker.post(eq(CopilotEventConstants.TOPIC_LANGUAGE_SERVER_INITIALIZED), anyLong())).thenReturn(true);
    try (MockedStatic<EclipseContextFactory> contexts = mockStatic(EclipseContextFactory.class)) {
      contexts.when(() -> EclipseContextFactory.getServiceContext(any(BundleContext.class))).thenReturn(context);
      LanguageServer server = mock(LanguageServer.class);
      NotificationMessage message = new NotificationMessage();
      LsStreamConnectionProvider first = new LsStreamConnectionProvider();
      message.setMethod("didChangeStatus");
      first.handleMessage(message, server, null);
      message.setMethod("initialized");
      first.handleMessage(message, server, null);
      first.handleMessage(message, server, null);
      first.stop();
      first.handleMessage(message, server, null);
      LsStreamConnectionProvider stopped = new LsStreamConnectionProvider();
      stopped.stop();
      stopped.handleMessage(message, server, null);
      new LsStreamConnectionProvider().handleMessage(message, server, null);

      ArgumentCaptor<Long> incarnations = ArgumentCaptor.forClass(Long.class);
      verify(broker, times(2)).post(eq(CopilotEventConstants.TOPIC_LANGUAGE_SERVER_INITIALIZED),
          incarnations.capture());
      assertTrue(incarnations.getAllValues().get(1) > incarnations.getAllValues().get(0));
    }
  }

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
    Process process = null;
    try {
      provider.start();
      process = provider.process();
      assertNotNull(process, "starting the provider must create a language server process");
    } finally {
      provider.stop();
      forceTerminate(process);
    }

    assertFalse(process.isAlive(), "the language server must not outlive the test");
  }

  /**
   * Makes sure the language server is really gone before the test JVM exits.
   *
   * <p>LSP4E's {@code stop()} only asks the process to terminate and returns without waiting, and on
   * Unix that request can be ignored. A surviving server still holds the stderr file descriptor it
   * inherited from this JVM; inside a Tycho test fork that descriptor is Maven's pipe, so the
   * Maven-side stream pump waits for an EOF that never arrives and the build hangs long after the
   * tests have passed. Killing the process here keeps that failure mode out of CI without changing
   * how the plugin manages language servers at runtime.
   *
   * @param process the language server process, or {@code null} if none was created
   */
  private static void forceTerminate(Process process) {
    if (process == null) {
      return;
    }
    process.destroy();
    if (awaitExit(process)) {
      return;
    }
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
    // destroyForcibly() only delivers the signal; without waiting, isAlive() can still be true.
    awaitExit(process);
  }

  private static boolean awaitExit(Process process) {
    try {
      return process.waitFor(TERMINATION_GRACE_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /**
   * Exposes the language server process so the test can assert on the real operating system process.
   */
  private static final class TestableLsStreamConnectionProvider extends LsStreamConnectionProvider {
    Process process() {
      return getProcess();
    }
  }
}

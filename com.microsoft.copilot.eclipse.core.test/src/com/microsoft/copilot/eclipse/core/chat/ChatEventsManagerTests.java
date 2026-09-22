// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.McpSamplingConfig;

class ChatEventsManagerTests {

  private ChatEventsManager chatEventsManager;

  @BeforeEach
  void setUp() {
    chatEventsManager = new ChatEventsManager();
  }

  @Test
  void getMcpSamplingConfig_returnsDefaultConfigWhenNoProviderRegistered() {
    McpSamplingConfig config = chatEventsManager.getMcpSamplingConfig("test-server");

    assertFalse(config.alwaysAllow());
    assertFalse(config.alwaysDeny());
    assertEquals(List.of(), config.allowedModels());
  }

  @Test
  void getMcpSamplingConfig_delegatesToRegisteredProvider() {
    McpSamplingConfigProvider provider = mock(McpSamplingConfigProvider.class);
    McpSamplingConfig expected = new McpSamplingConfig(true, false, List.of());
    when(provider.getMcpSamplingConfig("test-server")).thenReturn(expected);

    chatEventsManager.registerMcpSamplingConfigProvider(provider);

    assertEquals(expected, chatEventsManager.getMcpSamplingConfig("test-server"));
  }

  @Test
  void getMcpSamplingConfig_fallsBackToDefaultAfterUnregister() {
    McpSamplingConfigProvider provider = mock(McpSamplingConfigProvider.class);
    chatEventsManager.registerMcpSamplingConfigProvider(provider);
    chatEventsManager.unregisterMcpSamplingConfigProvider(provider);

    McpSamplingConfig config = chatEventsManager.getMcpSamplingConfig("test-server");

    assertFalse(config.alwaysAllow());
  }
}

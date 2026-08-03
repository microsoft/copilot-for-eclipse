// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.lsp4e.LanguageServerWrapper;
import org.junit.jupiter.api.Test;

/**
 * Exception telemetry is driven by a platform log listener, so it can be triggered by an error that
 * was logged while the language server was starting up. Asking the wrapper for a server at that
 * moment starts one, and starting one takes {@code LanguageServerWrapper}'s locks in the opposite
 * order to shutdown - the two deadlock, the JVM never exits, and a Tycho test fork hangs until CI
 * gives up. Telemetry therefore has to observe the server without ever creating one.
 */
class ExceptionTelemetryTests {

  @Test
  void testSendExceptionTelemetryDoesNotStartAnInactiveLanguageServer() {
    LanguageServerWrapper wrapper = mock(LanguageServerWrapper.class);
    when(wrapper.isActive()).thenReturn(false);
    CopilotLanguageServerConnection connection = new CopilotLanguageServerConnection(wrapper);

    assertNull(connection.sendExceptionTelemetry(new IllegalStateException("boom")).join());

    // execute() would call getInitializedServer(), which starts the server.
    verify(wrapper, never()).execute(any());
  }
}

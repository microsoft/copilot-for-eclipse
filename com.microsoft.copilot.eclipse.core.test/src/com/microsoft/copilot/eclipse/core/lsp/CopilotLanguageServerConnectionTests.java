// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.protocol.TelemetryExceptionParams;

@ExtendWith(MockitoExtension.class)
class CopilotLanguageServerConnectionTests {

  @Mock
  private LanguageServerWrapper languageServerWrapper;
  @Mock
  private CopilotLanguageServer languageServer;

  private final AtomicReference<Function<LanguageServer, ? extends CompletableFuture<Void>>> sinkInitializer =
      new AtomicReference<>();
  private CopilotLanguageServerConnection connection;

  @BeforeEach
  void setUp() {
    when(languageServerWrapper.<Void>execute(any())).thenAnswer(invocation -> {
      sinkInitializer.set(invocation.getArgument(0));
      return new CompletableFuture<Void>();
    });
    connection = new CopilotLanguageServerConnection(languageServerWrapper);
  }

  @Test
  void testSendExceptionTelemetry_beforeInitialization_doesNotReenterWrapper() {
    assertNull(connection.sendExceptionTelemetry(new IllegalStateException("test")).join());

    verify(languageServerWrapper, times(1)).execute(any());
    verify(languageServer, never()).sendExceptionTelemetry(any());
  }

  @Test
  void testSendExceptionTelemetry_afterInitialization_usesCachedSink() {
    sinkInitializer.get().apply(languageServer).join();
    when(languageServer.sendExceptionTelemetry(any())).thenReturn(CompletableFuture.completedFuture(null));

    assertNull(connection.sendExceptionTelemetry(new IllegalStateException("test")).join());

    ArgumentCaptor<TelemetryExceptionParams> paramsCaptor = ArgumentCaptor.forClass(TelemetryExceptionParams.class);
    verify(languageServer).sendExceptionTelemetry(paramsCaptor.capture());
    verify(languageServerWrapper, times(1)).execute(any());
    assertEquals(1, paramsCaptor.getValue().getExceptionDetail().size());
  }

  @Test
  void testSendExceptionTelemetry_afterFailureOnActiveServerKeepsSink() {
    sinkInitializer.get().apply(languageServer).join();
    when(languageServerWrapper.isActive()).thenReturn(true);
    when(languageServer.sendExceptionTelemetry(any()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("transient")));

    assertNull(connection.sendExceptionTelemetry(new IllegalStateException("first")).join());
    assertNull(connection.sendExceptionTelemetry(new IllegalStateException("second")).join());

    verify(languageServer, times(2)).sendExceptionTelemetry(any());
  }

  @Test
  void testSendExceptionTelemetry_afterFailureOnStoppedServerClearsSink() {
    sinkInitializer.get().apply(languageServer).join();
    when(languageServerWrapper.isActive()).thenReturn(false);
    when(languageServer.sendExceptionTelemetry(any()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("closed")));

    assertNull(connection.sendExceptionTelemetry(new IllegalStateException("first")).join());
    assertNull(connection.sendExceptionTelemetry(new IllegalStateException("second")).join());

    verify(languageServer, times(1)).sendExceptionTelemetry(any());
  }

  @Test
  void testStop_clearsExceptionSinkBeforeStoppingWrapper() {
    sinkInitializer.get().apply(languageServer).join();

    connection.stop();
    assertNull(connection.sendExceptionTelemetry(new IllegalStateException("test")).join());

    verify(languageServerWrapper).stop();
    verify(languageServer, never()).sendExceptionTelemetry(any());
  }

  @Test
  void testStop_beforeInitializationPreventsLateSinkRegistration() {
    connection.stop();
    sinkInitializer.get().apply(languageServer).join();

    assertNull(connection.sendExceptionTelemetry(new IllegalStateException("test")).join());

    verify(languageServerWrapper).stop();
    verify(languageServer, never()).sendExceptionTelemetry(any());
  }

  @Test
  void testStop_isIdempotent() {
    connection.stop();
    connection.stop();

    verify(languageServerWrapper, times(1)).stop();
  }
}

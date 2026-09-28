// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;

class PreferenceShutdownTest {
  @TempDir
  Path directory;
  private final Display display = Display.getDefault();
  private final IEventBroker broker = mock(IEventBroker.class);
  private final CopilotLanguageServerConnection connection = mock(CopilotLanguageServerConnection.class);
  private final AuthStatusManager auth = mock(AuthStatusManager.class);
  private final ExecutorService worker = mock(ExecutorService.class);
  private final ScheduledExecutorService timer = mock(ScheduledExecutorService.class);
  private final Queue<Runnable> work = new ConcurrentLinkedQueue<>();
  private final AtomicLong clock = new AtomicLong();
  private final AtomicInteger quiesced = new AtomicInteger();
  private PreferenceStorage storage;
  private PreferenceShutdown shutdown;
  private EventHandler shutdownEvent;

  @BeforeEach
  void setUp() throws Exception {
    when(auth.isSignedIn()).thenReturn(true);
    when(auth.getUserName()).thenReturn("alice");
    ChatPersistence persistence = new ChatPersistence();
    persistence.setPath(directory.toString());
    when(connection.persistence()).thenReturn(CompletableFuture.completedFuture(persistence));
    Mockito.doAnswer(invocation -> {
      work.add(invocation.getArgument(0));
      return null;
    }).when(worker).execute(any(Runnable.class));
    when(broker.subscribe(eq(UIEvents.UILifeCycle.APP_SHUTDOWN_STARTED), any(EventHandler.class)))
        .thenAnswer(invocation -> {
          shutdownEvent = invocation.getArgument(1);
          return true;
        });
    onUi(() -> {
      storage = new PreferenceStorage(connection, auth, DisplayRealm.getRealm(display),
          worker, timer, clock::get, new PreferenceFileAccess());
      shutdown = new PreferenceShutdown(broker, display, storage, quiesced::incrementAndGet, clock::get);
      storage.initialize();
    });
    drainWork();
    onUi(() -> assertEquals(PreferenceStorage.State.READY, storage.getState()));
  }

  @AfterEach
  void tearDown() throws Exception {
    onUi(() -> {
      shutdown.dispose();
      storage.dispose();
    });
  }

  @Test
  void testWorkbenchShutdown_PendingSave_DispatchesSwtAndContinuesAsSoonAsSaved() throws Exception {
    AtomicBoolean actionProcessed = new AtomicBoolean();
    onUi(() -> {
      storage.update(preference -> preference.setChatModel("final"));
      display.asyncExec(() -> {
        actionProcessed.set(true);
        CompletableFuture.runAsync(this::drainWork);
      });
      shutdownEvent.handleEvent(null);
      assertTrue(actionProcessed.get());
      assertTrue(storage.beginShutdown().toCompletableFuture().getNow(false));
      assertEquals(1, quiesced.get());
      assertFalse(storage.update(preference -> preference.setChatModel("late")));
    });
    assertTrue(Files.readString(directory.resolve("alice").resolve("pref.json")).contains("\"chatModel\":\"final\""));
    verify(connection).persistence();
  }

  @Test
  void testWorkbenchShutdown_ExactDeadline_ContinuesWithoutCompletingWriter() throws Exception {
    AtomicBoolean beforeDeadline = new AtomicBoolean();
    AtomicBoolean afterDeadline = new AtomicBoolean();
    onUi(() -> {
      storage.update(preference -> preference.setChatModel("final"));
      display.asyncExec(() -> {
        clock.set(TimeUnit.MILLISECONDS.toNanos(1999));
        display.asyncExec(() -> {
          beforeDeadline.set(true);
          clock.set(TimeUnit.SECONDS.toNanos(2));
          display.asyncExec(() -> afterDeadline.set(true));
        });
      });
      shutdownEvent.handleEvent(null);
      assertTrue(beforeDeadline.get(), "Must keep dispatching before the deadline");
      assertFalse(afterDeadline.get(), "Must stop waiting at exactly two seconds");
      assertFalse(storage.beginShutdown().toCompletableFuture().isDone());
      shutdownEvent.handleEvent(null);
      assertEquals(1, quiesced.get(), "Repeated notifications cannot start another wait");
      storage.dispose();
    });
    drainWork();
    assertTrue(Files.readString(directory.resolve("alice").resolve("pref.json")).contains("\"chatModel\":\"final\""));
    onUi(() -> assertEquals(PreferenceStorage.State.DISPOSED, storage.getState()));
    verify(connection).persistence();
  }

  @Test
  void testWorkbenchShutdown_ReentrantNotificationAndDisposal_ReleasesOriginalExitOnce() throws Exception {
    onUi(() -> {
      storage.update(preference -> preference.setChatModel("final"));
      display.asyncExec(() -> {
        shutdownEvent.handleEvent(null);
        shutdown.dispose();
        storage.dispose();
      });
      shutdownEvent.handleEvent(null);
      assertEquals(1, quiesced.get());
      assertEquals(PreferenceStorage.State.DISPOSED, storage.getState());
    });
    drainWork();
    verify(broker).unsubscribe(any(EventHandler.class));
    verify(connection).persistence();
  }

  @Test
  void testWorkbenchShutdown_Clean_DoesNotDispatchOrWait() throws Exception {
    AtomicBoolean queued = new AtomicBoolean();
    onUi(() -> {
      display.asyncExec(() -> queued.set(true));
      shutdownEvent.handleEvent(null);
      assertFalse(queued.get());
      assertTrue(storage.beginShutdown().toCompletableFuture().getNow(false));
    });
    assertFalse(Files.exists(directory.resolve("alice").resolve("pref.json")));
    verify(connection).persistence();
  }

  @Test
  void testWorkbenchShutdown_QuiesceThrows_StillStopsPreferenceWorkAndContinuesExit() throws Exception {
    onUi(() -> {
      shutdown.dispose();
      shutdown = new PreferenceShutdown(broker, display, storage, () -> {
        throw new IllegalStateException("mode cleanup failed");
      }, clock::get);
      shutdownEvent.handleEvent(null);
      assertFalse(storage.update(preference -> preference.setChatModel("must-not-save")));
    });
    verify(connection).persistence();
  }

  @Test
  void testWorkbenchShutdown_DeadlineWake_ContinuesWithoutWriterAndCancelsTimer() throws Exception {
    AtomicInteger cancelled = new AtomicInteger();
    AtomicInteger delay = new AtomicInteger();
    AtomicBoolean elapsed = new AtomicBoolean();
    onUi(() -> {
      shutdown.dispose();
      shutdown = new PreferenceShutdown(broker, display, storage, quiesced::incrementAndGet, clock::get,
          (milliseconds, callback) -> {
            if (milliseconds >= 0) {
              delay.set(milliseconds);
              display.asyncExec(() -> {
                elapsed.set(true);
                callback.run();
              });
            } else {
              cancelled.incrementAndGet();
            }
          });
      storage.update(preference -> preference.setChatModel("unsaved"));
      shutdownEvent.handleEvent(null);
      assertTrue(elapsed.get());
      assertEquals(2000, delay.get());
      assertEquals(1, cancelled.get());
      assertFalse(storage.beginShutdown().toCompletableFuture().isDone());
    });
  }

  @Test
  void testWorkbenchShutdown_FileFailure_ContinuesEarlyAndKeepsOriginalTarget() throws Exception {
    Path target = directory.resolve("alice").resolve("pref.json");
    Files.createDirectories(target);
    Files.writeString(target.resolve("keep"), "original");
    onUi(() -> {
      storage.update(preference -> preference.setChatModel("unsaved"));
      display.asyncExec(() -> CompletableFuture.runAsync(this::drainWork));
      shutdownEvent.handleEvent(null);
      var result = storage.beginShutdown().toCompletableFuture();
      assertTrue(result.isDone());
      assertFalse(result.getNow(true));
    });
    assertEquals("original", Files.readString(target.resolve("keep")));
    verify(connection).persistence();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testWorkbenchShutdown_NoResolvedPath_DoesNotWaitOrStartRpc(boolean requested) throws Exception {
    CompletableFuture<ChatPersistence> pending = new CompletableFuture<>();
    when(connection.persistence()).thenReturn(pending);
    Mockito.clearInvocations(connection);
    onUi(() -> {
      shutdown.dispose();
      storage.dispose();
      storage = new PreferenceStorage(connection, auth, DisplayRealm.getRealm(display),
          worker, timer, clock::get, new PreferenceFileAccess());
      shutdown = new PreferenceShutdown(broker, display, storage, quiesced::incrementAndGet, clock::get);
      if (requested) {
        storage.initialize();
      }
    });
    drainWork();
    AtomicBoolean dispatched = new AtomicBoolean();
    onUi(() -> {
      display.asyncExec(() -> dispatched.set(true));
      shutdownEvent.handleEvent(null);
      assertFalse(dispatched.get());
      assertTrue(storage.beginShutdown().toCompletableFuture().getNow(false));
    });
    verify(connection, times(requested ? 1 : 0)).persistence();
  }

  private void drainWork() {
    Runnable next;
    while ((next = work.poll()) != null) {
      next.run();
    }
  }

  private void onUi(Runnable action) throws Exception {
    AtomicReference<Throwable> failure = new AtomicReference<>();
    display.syncExec(() -> {
      try {
        action.run();
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });
    if (failure.get() != null) {
      throw new AssertionError("UI callback failed", failure.get());
    }
  }
}

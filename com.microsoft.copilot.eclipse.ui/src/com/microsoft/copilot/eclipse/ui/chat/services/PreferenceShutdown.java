// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Participates in the committed workbench exit, after all shutdown veto opportunities have passed.
 * Eclipse sends this event synchronously before disposing services. A bounded nested SWT loop keeps
 * dispatching events while the shared writer finishes; it never vetoes or reissues a close request.
 * The budget bounds our waiting, not arbitrary OS I/O or other participants' event handlers.
 */
final class PreferenceShutdown {
  private static final long WAIT_NANOS = TimeUnit.SECONDS.toNanos(2);

  private final IEventBroker broker;
  private final Display display;
  private final PreferenceStorage storage;
  private final Runnable quiesce;
  private final LongSupplier clock;
  private final BiConsumer<Integer, Runnable> schedule;
  private final AtomicBoolean started = new AtomicBoolean();
  private final EventHandler listener = event -> shutdown();
  private volatile boolean disposed;

  PreferenceShutdown(IEventBroker broker, Display display, PreferenceStorage storage, Runnable quiesce,
      LongSupplier clock) {
    this(broker, display, storage, quiesce, clock, display::timerExec);
  }

  PreferenceShutdown(IEventBroker broker, Display display, PreferenceStorage storage, Runnable quiesce,
      LongSupplier clock, BiConsumer<Integer, Runnable> schedule) {
    this.broker = broker;
    this.display = display;
    this.storage = storage;
    this.quiesce = quiesce;
    this.clock = clock;
    this.schedule = schedule;
    if (broker == null || !broker.subscribe(UIEvents.UILifeCycle.APP_SHUTDOWN_STARTED, listener)) {
      CopilotCore.LOGGER.error(new IllegalStateException("Cannot subscribe to workbench preference shutdown"));
    }
  }

  private void shutdown() {
    if (disposed || !started.compareAndSet(false, true)) {
      return;
    }
    long start = clock.getAsLong();
    AtomicBoolean expired = new AtomicBoolean();
    Runnable deadline = () -> expired.set(true);
    try {
      CompletableFuture<Boolean> saved = storage.beginShutdown().toCompletableFuture();
      try {
        quiesce.run();
      } catch (RuntimeException exception) {
        CopilotCore.LOGGER.error("Failed to stop chat mode discovery during shutdown", exception);
      }
      if (!saved.isDone() && !display.isDisposed() && !disposed) {
        long remaining = WAIT_NANOS - (clock.getAsLong() - start);
        if (remaining > 0) {
          // Wake an idle nested event loop at the deadline, even when the writer never completes.
          schedule.accept((int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)), deadline);
          saved.whenComplete((result, failure) -> wake());
          while (!disposed && !display.isDisposed() && !saved.isDone() && !expired.get()
              && clock.getAsLong() - start < WAIT_NANOS) {
            // A writer wakeup can be consumed by dispatch; recheck before going back to sleep.
            if (!display.readAndDispatch() && !saved.isDone() && !disposed && !expired.get()
                && clock.getAsLong() - start < WAIT_NANOS) {
              display.sleep();
            }
          }
        }
      }
      if (!saved.getNow(false)) {
        CopilotCore.LOGGER.error(new IllegalStateException(
            "Exiting with unsaved chat preferences after failure or the two-second save budget"));
      }
    } catch (RuntimeException exception) {
      CopilotCore.LOGGER.error("Failed to finish saving chat preferences during shutdown; continuing exit", exception);
    } finally {
      if (!display.isDisposed()) {
        schedule.accept(-1, deadline);
      }
    }
  }

  private void wake() {
    if (disposed || display.isDisposed()) {
      return;
    }
    try {
      display.wake();
    } catch (SWTException exception) {
      if (exception.code != SWT.ERROR_DEVICE_DISPOSED) {
        throw exception;
      }
    }
  }

  void dispose() {
    if (disposed) {
      return;
    }
    wake();
    disposed = true;
    if (broker != null) {
      broker.unsubscribe(listener);
    }
  }
}

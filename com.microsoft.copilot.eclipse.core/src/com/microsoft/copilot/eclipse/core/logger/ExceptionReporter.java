// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.logger;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import com.microsoft.copilot.eclipse.core.Constants;

/**
 * Collects and dispatches best-effort exception reports without blocking the logging thread.
 */
public final class ExceptionReporter implements AutoCloseable {
  private static final int DEFAULT_QUEUE_CAPACITY = 32;
  private static final long SHUTDOWN_TIMEOUT_SECONDS = 1L;

  private final ThreadPoolExecutor executor;
  private final AtomicBoolean closed = new AtomicBoolean();
  private final Supplier<Consumer<Throwable>> sinkSupplier;
  private final ILogListener platformLogListener = this::handlePlatformLog;

  /**
   * Creates an exception reporter.
   *
   * @param sinkSupplier resolves the current destination for reports, or {@code null} while none is
   *        available. It is called on the thread that logged the exception, so it must not block.
   */
  public ExceptionReporter(Supplier<Consumer<Throwable>> sinkSupplier) {
    this(sinkSupplier, DEFAULT_QUEUE_CAPACITY);
  }

  ExceptionReporter(Supplier<Consumer<Throwable>> sinkSupplier, int queueCapacity) {
    this.sinkSupplier = Objects.requireNonNull(sinkSupplier);
    this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(queueCapacity), runnable -> {
          Thread thread = new Thread(runnable, "Copilot exception reporter");
          thread.setDaemon(true);
          return thread;
        }, new ThreadPoolExecutor.DiscardPolicy());
  }

  /**
   * Starts collecting exceptions from the Eclipse platform log.
   */
  public void start() {
    Platform.addLogListener(platformLogListener);
  }

  /**
   * Queues an exception report. Reports are silently discarded when no sink is available, when the
   * queue is full, or once the reporter is closed.
   *
   * @param exception the exception to report
   */
  public void report(Throwable exception) {
    Consumer<Throwable> currentSink = sinkSupplier.get();
    if (currentSink != null) {
      // The executor rejects work once close() has shut it down, and DiscardPolicy drops it
      // silently, so a report racing with close() cannot fail the thread that logged it.
      executor.execute(() -> currentSink.accept(exception));
    }
  }

  private void handlePlatformLog(IStatus status, String plugin) {
    if (status.getSeverity() != IStatus.ERROR || Constants.PLUGIN_ID.equals(plugin)) {
      return;
    }

    Throwable rawException = status.getException();
    if (rawException == null) {
      return;
    }

    Throwable currentException = rawException;
    do {
      for (StackTraceElement trace : currentException.getStackTrace()) {
        if (trace.getClassName().startsWith(Constants.PLUGIN_ID)) {
          report(rawException);
          return;
        }
      }
    } while ((currentException = currentException.getCause()) != null);
  }

  @Override
  public void close() {
    // Guards against a second close() paying the shutdown wait again.
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    Platform.removeLogListener(platformLogListener);
    executor.shutdownNow();
    try {
      executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.logger;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
  private final AtomicReference<Consumer<Throwable>> sink = new AtomicReference<>();
  private final ILogListener platformLogListener = this::handlePlatformLog;

  /**
   * Creates an exception reporter.
   */
  public ExceptionReporter() {
    this(DEFAULT_QUEUE_CAPACITY);
  }

  ExceptionReporter(int queueCapacity) {
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
   * Sets the destination for exception reports. Reports are discarded until a sink is available.
   *
   * @param reportSink consumes exceptions on the reporter thread
   */
  public void setSink(Consumer<Throwable> reportSink) {
    if (closed.get()) {
      return;
    }
    sink.set(Objects.requireNonNull(reportSink));
    // close() may have run between the check above and the set, in which case it already cleared the
    // sink; re-check so a late registration cannot resurrect it.
    if (closed.get()) {
      sink.set(null);
    }
  }

  /**
   * Queues an exception report. Reports are silently discarded when the queue is full or the
   * reporter is closed.
   *
   * @param exception the exception to report
   */
  public void report(Throwable exception) {
    Consumer<Throwable> currentSink = sink.get();
    if (currentSink != null) {
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
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    Platform.removeLogListener(platformLogListener);
    sink.set(null);
    executor.shutdownNow();
    try {
      executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

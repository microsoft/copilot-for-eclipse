// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class ExceptionReporterTests {

  @Test
  void testReport_dispatchesOnReporterThread() throws InterruptedException {
    Thread callerThread = Thread.currentThread();
    AtomicReference<Thread> reporterThread = new AtomicReference<>();
    CountDownLatch reported = new CountDownLatch(1);

    try (ExceptionReporter reporter = new ExceptionReporter()) {
      reporter.setSink(exception -> {
        reporterThread.set(Thread.currentThread());
        reported.countDown();
      });
      reporter.report(new IllegalStateException("test"));

      assertTrue(reported.await(5, TimeUnit.SECONDS));
      assertNotEquals(callerThread, reporterThread.get());
    }
  }

  @Test
  void testReport_withoutSinkIsDiscarded() {
    AtomicInteger reportCount = new AtomicInteger();
    try (ExceptionReporter reporter = new ExceptionReporter()) {
      reporter.report(new IllegalStateException("test"));
      reporter.setSink(exception -> reportCount.incrementAndGet());

      assertEquals(0, reportCount.get());
    }
  }

  @Test
  void testSetSink_afterCloseIsIgnored() {
    AtomicInteger reportCount = new AtomicInteger();
    ExceptionReporter reporter = new ExceptionReporter();
    reporter.close();

    reporter.setSink(exception -> reportCount.incrementAndGet());
    reporter.report(new IllegalStateException("test"));

    assertEquals(0, reportCount.get());
  }

  @Test
  void testReport_discardsWhenQueueIsFull() throws InterruptedException {
    AtomicInteger reportCount = new AtomicInteger();
    CountDownLatch firstReportStarted = new CountDownLatch(1);
    CountDownLatch releaseFirstReport = new CountDownLatch(1);
    CountDownLatch secondReportFinished = new CountDownLatch(1);

    try (ExceptionReporter reporter = new ExceptionReporter(1)) {
      reporter.setSink(exception -> {
        int count = reportCount.incrementAndGet();
        if (count == 1) {
          firstReportStarted.countDown();
          try {
            releaseFirstReport.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        } else {
          secondReportFinished.countDown();
        }
      });
      reporter.report(new IllegalStateException("first"));
      assertTrue(firstReportStarted.await(5, TimeUnit.SECONDS));

      reporter.report(new IllegalStateException("second"));
      reporter.report(new IllegalStateException("discarded"));
      releaseFirstReport.countDown();

      assertTrue(secondReportFinished.await(5, TimeUnit.SECONDS));
      assertEquals(2, reportCount.get());
    }
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TelemetryExceptionParamsTests {

  @Test
  void testConstructor_withSourceFileBuildsPath() {
    var params = new TelemetryExceptionParams(exceptionWithFrame("SampleClass.java"));

    assertEquals("com/microsoft/copilot/SampleClass.java", firstFrameFilename(params));
  }

  @Test
  void testConstructor_withoutSourceFileKeepsClassName() {
    var params = new TelemetryExceptionParams(exceptionWithFrame(null));

    assertEquals("com/microsoft/copilot/SampleClass", firstFrameFilename(params));
  }

  private static Throwable exceptionWithFrame(String fileName) {
    var exception = new IllegalStateException("boom");
    exception.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("com.microsoft.copilot.SampleClass", "run", fileName, 42) });
    return exception;
  }

  private static String firstFrameFilename(TelemetryExceptionParams params) {
    return params.getExceptionDetail().get(0).getStacktrace()[0].getFilename();
  }
}

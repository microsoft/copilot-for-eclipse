// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TelemetryExceptionParamsTests {

  @Test
  void testConstructor_normalClass_usesPackagePathAndSourceFile() {
    StackTraceElement element = new StackTraceElement("com.example.Example", "run", "Example.java", 42);

    assertEquals("com/example/Example.java", getTelemetryFilename(element));
  }

  @Test
  void testConstructor_nestedClass_usesTopLevelSourceFile() {
    StackTraceElement element = new StackTraceElement("com.example.Outer$Inner", "run", "Outer.java", 42);

    assertEquals("com/example/Outer.java", getTelemetryFilename(element));
  }

  @Test
  void testConstructor_unknownSource_usesClassDerivedSourceFile() {
    StackTraceElement element = new StackTraceElement("com.example.Outer$Inner", "run", null, 42);

    assertEquals("com/example/Outer.java", getTelemetryFilename(element));
  }

  private static String getTelemetryFilename(StackTraceElement element) {
    RuntimeException exception = new RuntimeException("test");
    exception.setStackTrace(new StackTraceElement[] { element });
    TelemetryExceptionParams params = new TelemetryExceptionParams(exception);
    return params.getExceptionDetail().get(0).getStacktrace()[0].getFilename();
  }
}

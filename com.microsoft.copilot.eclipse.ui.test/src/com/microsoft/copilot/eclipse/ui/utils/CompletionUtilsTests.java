// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CompletionUtilsTests {
  @Test
  void testReplaceTabsWithSpaces() {
    String input = "\tfoo\n\t\tbar";
    String expected = "  foo\n    bar";
    assertEquals(expected, CompletionUtils.replaceTabsWithSpaces(input, 2));
  }

  @Test
  void testNormalizeLineEndings() {
    String input = "line1\r\nline2\nline3\rline4";
    String expected = "line1\r\nline2\r\nline3\r\nline4";
    assertEquals(expected, CompletionUtils.normalizeLineEndings(input, "\r\n"));
  }

}

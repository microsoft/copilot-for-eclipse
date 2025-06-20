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

}

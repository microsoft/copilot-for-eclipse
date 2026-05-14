// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the private static parsing helpers in {@link ThinkingBlock}: {@code stripTrailingNewlines} and
 * {@code parseSections}. Exercises the helpers via reflection so the production visibility stays untouched.
 *
 * <p>Primary goal: guard CRLF handling so a {@code \r\n}-terminated body or title boundary does not leak a stray
 * {@code \r} into the rendered text.
 */
class ThinkingBlockParseTest {

  @Test
  void stripTrailingNewlines_stripsLfCrAndCrlf() throws Exception {
    assertEquals("body", invokeStripTrailingNewlines("body\n"));
    assertEquals("body", invokeStripTrailingNewlines("body\r"));
    assertEquals("body", invokeStripTrailingNewlines("body\r\n"));
    assertEquals("body", invokeStripTrailingNewlines("body\r\n\r\n"));
    assertEquals("body", invokeStripTrailingNewlines("body\n\r\n"));
    assertEquals("body", invokeStripTrailingNewlines("body"));
    assertEquals("", invokeStripTrailingNewlines(""));
    assertEquals("", invokeStripTrailingNewlines("\r\n\r\n"));
    // Internal newlines and leading whitespace must survive untouched.
    assertEquals("  code\n  more", invokeStripTrailingNewlines("  code\n  more\r\n"));
  }

  @Test
  void parseSections_handlesCrlfTitleBoundary() throws Exception {
    String raw = "intro body\r\n**Plan**\r\nplan body\r\n**Next**\r\nnext body\r\n";
    List<?> sections = invokeParseSections(raw);
    assertEquals(3, sections.size());

    assertNull(title(sections.get(0)));
    assertEquals("intro body", body(sections.get(0)));

    assertEquals("Plan", title(sections.get(1)));
    assertEquals("plan body", body(sections.get(1)));

    assertEquals("Next", title(sections.get(2)));
    assertEquals("next body", body(sections.get(2)));
  }

  @Test
  void parseSections_handlesLfOnlyTitleBoundary() throws Exception {
    // Existing LF behavior must remain unchanged.
    String raw = "intro\n**Plan**\nplan body";
    List<?> sections = invokeParseSections(raw);
    assertEquals(2, sections.size());
    assertNull(title(sections.get(0)));
    assertEquals("intro", body(sections.get(0)));
    assertEquals("Plan", title(sections.get(1)));
    assertEquals("plan body", body(sections.get(1)));
  }

  @Test
  void parseSections_inlineBoldNotTreatedAsTitle() throws Exception {
    // Inline bold at end of string should NOT be parsed as a section title.
    String raw = "combined with **The Ship of Theseus**";
    List<?> sections = invokeParseSections(raw);
    assertEquals(1, sections.size());
    assertNull(title(sections.get(0)));
    assertEquals("combined with **The Ship of Theseus**", body(sections.get(0)));
  }

  @Test
  void parseSections_inlineBoldFollowedByText() throws Exception {
    // Inline bold mid-line should remain as body text.
    String raw = "text with **bold** and more text";
    List<?> sections = invokeParseSections(raw);
    assertEquals(1, sections.size());
    assertNull(title(sections.get(0)));
    assertEquals("text with **bold** and more text", body(sections.get(0)));
  }

  @Test
  void parseSections_mixOfInlineBoldAndStandaloneTitle() throws Exception {
    // Inline bold on one line, standalone title on next line.
    String raw = "text with **inline bold** here\n**Standalone Title**\nbody after title";
    List<?> sections = invokeParseSections(raw);
    assertEquals(2, sections.size());
    assertNull(title(sections.get(0)));
    assertEquals("text with **inline bold** here", body(sections.get(0)));
    assertEquals("Standalone Title", title(sections.get(1)));
    assertEquals("body after title", body(sections.get(1)));
  }

  @Test
  void parseSections_titleAtStartOfText() throws Exception {
    String raw = "**First**\nbody one\n**Second**\nbody two";
    List<?> sections = invokeParseSections(raw);
    assertEquals(2, sections.size());
    assertEquals("First", title(sections.get(0)));
    assertEquals("body one", body(sections.get(0)));
    assertEquals("Second", title(sections.get(1)));
    assertEquals("body two", body(sections.get(1)));
  }

  private static String invokeStripTrailingNewlines(String input) throws Exception {
    Method m = ThinkingBlock.class.getDeclaredMethod("stripTrailingNewlines", String.class);
    m.setAccessible(true);
    return (String) m.invoke(null, input);
  }

  private static List<?> invokeParseSections(String raw) throws Exception {
    Method m = ThinkingBlock.class.getDeclaredMethod("parseSections", String.class);
    m.setAccessible(true);
    return (List<?>) m.invoke(null, raw);
  }

  private static String title(Object parsedSection) throws Exception {
    Method m = parsedSection.getClass().getDeclaredMethod("title");
    m.setAccessible(true);
    return (String) m.invoke(parsedSection);
  }

  private static String body(Object parsedSection) throws Exception {
    Method m = parsedSection.getClass().getDeclaredMethod("body");
    m.setAccessible(true);
    return (String) m.invoke(parsedSection);
  }
}

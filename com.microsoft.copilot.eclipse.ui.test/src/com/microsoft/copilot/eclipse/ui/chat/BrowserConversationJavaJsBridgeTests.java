// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pure unit tests for the static, side-effect-free members of
 * {@link BrowserConversationJavaJsBridge}: {@code escapeForJs} and the block-script builders. No
 * SWT {@code Display} or {@code Browser} is required. These lock the exact JavaScript emitted so
 * the "byte-identical output" contract of the Java&#8596;JS boundary cannot regress unnoticed.
 */
class BrowserConversationJavaJsBridgeTests {

  @Test
  void escapeForJsReturnsEmptyForNull() {
    assertEquals("", BrowserConversationJavaJsBridge.escapeForJs(null));
  }

  @ParameterizedTest(name = "escapeForJs({0}) = {1}")
  @MethodSource("escapeForJsCases")
  void escapeForJsEscapesCharactersCorrectly(String input, String expected) {
    assertEquals(expected, BrowserConversationJavaJsBridge.escapeForJs(input));
  }

  static Stream<Arguments> escapeForJsCases() {
    return Stream.of(
        Arguments.of("hello", "hello"),
        Arguments.of("it's", "it\\'s"),
        Arguments.of("back\\slash", "back\\\\slash"),
        Arguments.of("line1\nline2", "line1\\nline2"),
        Arguments.of("tab\there", "tab\\there"),
        Arguments.of("cr\r", "cr\\r"),
        Arguments.of("all\\\n\r\t'", "all\\\\\\n\\r\\t\\'")
    );
  }

  @ParameterizedTest(name = "insertBlockScript({0}, {1}) = {2}")
  @MethodSource("insertBlockScriptCases")
  void insertBlockScriptEmitsExactJs(String parentId, String html, String expected) {
    assertEquals(expected, BrowserConversationJavaJsBridge.insertBlockScript(parentId, html));
  }

  static Stream<Arguments> insertBlockScriptCases() {
    return Stream.of(
        Arguments.of("chat-container", "<div>hi</div>",
            "window.insertBlock('chat-container', '<div>hi</div>')"),
        Arguments.of("turn-1", "it's <b>bold</b>",
            "window.insertBlock('turn-1', 'it\\'s <b>bold</b>')"),
        Arguments.of("a\\b", "line1\nline2",
            "window.insertBlock('a\\\\b', 'line1\\nline2')")
    );
  }

  @ParameterizedTest(name = "insertBlockBeforeScript({0}, {1}, {2}) = {3}")
  @MethodSource("insertBlockBeforeScriptCases")
  void insertBlockBeforeScriptEmitsExactJs(String parentId, String html, String beforeId,
      String expected) {
    assertEquals(expected,
        BrowserConversationJavaJsBridge.insertBlockBeforeScript(parentId, html, beforeId));
  }

  static Stream<Arguments> insertBlockBeforeScriptCases() {
    return Stream.of(
        Arguments.of("turn-1-copilot", "<div>card</div>", "turn-1-model-info",
            "window.insertBlockBefore('turn-1-copilot', '<div>card</div>', 'turn-1-model-info')"),
        Arguments.of("p'1", "h'2", "b'3",
            "window.insertBlockBefore('p\\'1', 'h\\'2', 'b\\'3')")
    );
  }

  @ParameterizedTest(name = "replaceBlockScript({0}, {1}) = {2}")
  @MethodSource("replaceBlockScriptCases")
  void replaceBlockScriptEmitsExactJs(String blockId, String html, String expected) {
    assertEquals(expected, BrowserConversationJavaJsBridge.replaceBlockScript(blockId, html));
  }

  static Stream<Arguments> replaceBlockScriptCases() {
    return Stream.of(
        Arguments.of("turn-1-0", "<p>updated</p>",
            "window.replaceBlock('turn-1-0', '<p>updated</p>')"),
        Arguments.of("id'x", "a\tb",
            "window.replaceBlock('id\\'x', 'a\\tb')")
    );
  }

  @ParameterizedTest(name = "removeBlockScript({0}) = {1}")
  @MethodSource("removeBlockScriptCases")
  void removeBlockScriptEmitsExactJs(String blockId, String expected) {
    assertEquals(expected, BrowserConversationJavaJsBridge.removeBlockScript(blockId));
  }

  static Stream<Arguments> removeBlockScriptCases() {
    return Stream.of(
        Arguments.of("turn-1-confirm", "window.removeBlock('turn-1-confirm')"),
        Arguments.of("id'x", "window.removeBlock('id\\'x')")
    );
  }

  @ParameterizedTest(name = "updateThinkingBlockTitleScript({0}, {1}) = {2}")
  @MethodSource("updateThinkingBlockTitleScriptCases")
  void updateThinkingBlockTitleScriptEmitsExactJs(String blockId, String titleHtml,
      String expected) {
    assertEquals(expected,
        BrowserConversationJavaJsBridge.updateThinkingBlockTitleScript(blockId, titleHtml));
  }

  static Stream<Arguments> updateThinkingBlockTitleScriptCases() {
    return Stream.of(
        Arguments.of("t1-0", "<b>Planning</b>",
            "var b=document.getElementById('t1-0');"
                + "if(b){var s=b.querySelector('summary');if(s)s.innerHTML='<b>Planning</b>';}"),
        Arguments.of("t1-0", "it's done",
            "var b=document.getElementById('t1-0');"
                + "if(b){var s=b.querySelector('summary');if(s)s.innerHTML='it\\'s done';}")
    );
  }

  @ParameterizedTest(name = "updateThinkingBodyTextScript({0}, {1}) = {2}")
  @MethodSource("updateThinkingBodyTextScriptCases")
  void updateThinkingBodyTextScriptEmitsExactJs(String blockId, String bodyHtml, String expected) {
    assertEquals(expected,
        BrowserConversationJavaJsBridge.updateThinkingBodyTextScript(blockId, bodyHtml));
  }

  static Stream<Arguments> updateThinkingBodyTextScriptCases() {
    return Stream.of(
        Arguments.of("t1-1", "<p>x</p>",
            "var b=document.getElementById('t1-1');"
                + "if(b){var bd=b.querySelector('.thinking-body');if(bd){"
                + "bd.innerHTML='<p>x</p>';"
                + "var thr=60;if(bd.scrollHeight-(bd.scrollTop+bd.clientHeight)<=thr)"
                + "bd.scrollTop=bd.scrollHeight;}}"),
        Arguments.of("t1-1", "line1\nline2",
            "var b=document.getElementById('t1-1');"
                + "if(b){var bd=b.querySelector('.thinking-body');if(bd){"
                + "bd.innerHTML='line1\\nline2';"
                + "var thr=60;if(bd.scrollHeight-(bd.scrollTop+bd.clientHeight)<=thr)"
                + "bd.scrollTop=bd.scrollHeight;}}")
    );
  }

  @ParameterizedTest(name = "collapseThinkingBlockScript({0}, {1}) = {2}")
  @MethodSource("collapseThinkingBlockScriptCases")
  void collapseThinkingBlockScriptEmitsExactJs(String blockId, String bulbSvg, String expected) {
    assertEquals(expected,
        BrowserConversationJavaJsBridge.collapseThinkingBlockScript(blockId, bulbSvg));
  }

  static Stream<Arguments> collapseThinkingBlockScriptCases() {
    return Stream.of(
        Arguments.of("t1-2", "<svg><path/></svg>",
            "var b=document.getElementById('t1-2');"
                + "if(b){var d=b.querySelector('details');if(d)d.removeAttribute('open');"
                + "var s=b.querySelector('.thinking-spinner');"
                + "if(s){var icon=document.createElement('span');"
                + "icon.innerHTML='<svg><path/></svg>';"
                + "s.parentNode.replaceChild(icon.firstChild,s);}}"),
        // The SVG is embedded in a double-quoted innerHTML literal, so its quotes and apostrophes
        // are backslash-escaped for that context (not via escapeForJs).
        Arguments.of("t1-2", "<svg class=\"x\" data-y='z'/>",
            "var b=document.getElementById('t1-2');"
                + "if(b){var d=b.querySelector('details');if(d)d.removeAttribute('open');"
                + "var s=b.querySelector('.thinking-spinner');"
                + "if(s){var icon=document.createElement('span');"
                + "icon.innerHTML='<svg class=\\\"x\\\" data-y=\\'z\\'/>';"
                + "s.parentNode.replaceChild(icon.firstChild,s);}}")
    );
  }
}

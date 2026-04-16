// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class CompletionManagerLegacyTests {

  @Test
  void testGetGhostTexts1() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts(") {", "int[] arr, int low, int high) {", 0);
    assertEquals(1, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText ghostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals("int[] arr, int low, int high", ghostText.getText());
    assertEquals(0, ghostText.getModelOffset());
  }

  @Test
  void testGetGhostTexts2() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts(")", "int[] arr, int low, int high) {", 0);
    assertEquals(2, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText inlineGhostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals("int[] arr, int low, int high", inlineGhostText.getText());
    assertEquals(0, inlineGhostText.getModelOffset());

    assertTrue(EolGhostText.class.isInstance(ghostTexts.get(1)));
    EolGhostText eolGhostText = (EolGhostText) ghostTexts.get(1);
    assertEquals(" {", eolGhostText.getText());
    assertEquals(1, eolGhostText.getModelOffset());
  }

  @Test
  void testGetGhostTexts3() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts("int[] arr, int low, int high) {",
        "int[] arr, int low, int high) {", 0);
    assertTrue(ghostTexts.isEmpty());
  }

  @Test
  void testGetGhostTexts4() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts("]) {", "] arr, int low, int high) {", 0);
    assertEquals(1, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText ghostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals(" arr, int low, int high", ghostText.getText());
    assertEquals(1, ghostText.getModelOffset());
  }

  @Test
  void testGetGhostTexts5() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts("", "int[] arr, int low, int high) {", 0);
    assertEquals(1, ghostTexts.size());
    assertTrue(EolGhostText.class.isInstance(ghostTexts.get(0)));
    EolGhostText ghostText = (EolGhostText) ghostTexts.get(0);
    assertEquals("int[] arr, int low, int high) {", ghostText.getText());
    assertEquals(0, ghostText.getModelOffset());
  }

  @Test
  void testGetGhostTextWithNewLineCharacterOnTheTail() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts(") {\n", "(int[] arr, int low, int high) {",
        0);
    assertEquals(1, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText ghostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals("(int[] arr, int low, int high", ghostText.getText());
    assertEquals(0, ghostText.getModelOffset());
  }

  @Test
  void testGetGhostTextWithNewLineCharacterOnTheTail2() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts(") {\r", "(int[] arr, int low, int high) {",
        0);
    assertEquals(1, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText ghostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals("(int[] arr, int low, int high", ghostText.getText());
    assertEquals(0, ghostText.getModelOffset());
  }

  @Test
  void testGetGhostTextWithNewLineCharacterOnTheTail3() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts(") {\r\n",
        "(int[] arr, int low, int high) {", 0);
    assertEquals(1, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText ghostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals("(int[] arr, int low, int high", ghostText.getText());
    assertEquals(0, ghostText.getModelOffset());
  }

  @Test
  void testGetGhostTextWithTabCharacterOnTheTail() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts(") {\t", "(int[] arr, int low, int high) {",
        0);
    assertEquals(1, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText ghostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals("(int[] arr, int low, int high", ghostText.getText());
    assertEquals(0, ghostText.getModelOffset());
  }

  @Test
  void testGetGhostTextWithSpaceCharactersOnTheTail() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts(") {  \r\n",
        "(int[] arr, int low, int high) {", 0);
    assertEquals(1, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText ghostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals("(int[] arr, int low, int high", ghostText.getText());
    assertEquals(0, ghostText.getModelOffset());
  }

  @Test
  void testGetGhostTextWithChaoticCharactersOnTheTail() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts(") {\t\t\r\t\n\t\n\r\n",
        "(int[] arr, int low, int high) {", 0);
    assertEquals(1, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText ghostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals("(int[] arr, int low, int high", ghostText.getText());
    assertEquals(0, ghostText.getModelOffset());
  }

  @Test
  void testGetGhostTextWithWhiteSpaceCharactersOnTheTailWithoutCurlyBrace() {
    List<GhostText> ghostTexts = CompletionManagerLegacy.getGhostTexts("\t\t\r\t\n\t\n\r\n",
        "(int[] arr, int low, int high) {", 0);
    assertEquals(1, ghostTexts.size());
    assertTrue(InlineGhostText.class.isInstance(ghostTexts.get(0)));
    InlineGhostText ghostText = (InlineGhostText) ghostTexts.get(0);
    assertEquals("(int[] arr, int low, int high) {", ghostText.getText());
    assertEquals(0, ghostText.getModelOffset());
	}
}

package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;

class CompletionDataTests {

  @Test
  void testGetEmptyStringWhenNoItems() {
    CompletionData data = new CompletionData();
    assertEquals("", data.getText());
  }

  @Test
  void testGetFirstLineWithSingleLineItem() {
    CompletionData data = new CompletionData();
    data.setItems(List.of(new CompletionItem("uuid", "first", null, "first", null, 0)));
    assertEquals("first", data.getFirstLine());
  }

  @Test
  void testGetRemainingLineWithSingleLineItem() {
    CompletionData data = new CompletionData();
    data.setItems(List.of(new CompletionItem("uuid", "first", null, "first", null, 0)));
    assertEquals("", data.getRemainingLines());
  }

  @Test
  void testGetFirstLineWithMultilineItem() {
    CompletionData data = new CompletionData();
    data.setItems(List.of(new CompletionItem("uuid", "first\nsecond\nthird", null, "first\nsecond\nthird", null, 0)));
    assertEquals("first", data.getFirstLine());
  }

  @Test
  void testGetRemainingLinesWithMultilineItem() {
    CompletionData data = new CompletionData();
    data.setItems(List.of(new CompletionItem("uuid", "first\nsecond\nthird", null, "first\nsecond\nthird", null, 0)));
    assertEquals("second\nthird", data.getRemainingLines());
  }

  @Test
  void getNumberOfLinesWhenNoItems() {
    CompletionData completionData = new CompletionData();
    assertEquals(1, completionData.getNumberOfLines());
  }

  @Test
  void getNumberOfLinesWithMultilineItem() {
    CompletionData data = new CompletionData();
    data.setItems(List.of(new CompletionItem("uuid", "first\nsecond\nthird", null, "first\nsecond\nthird", null, 0)));
    assertEquals(3, data.getNumberOfLines());
  }

}

package com.microsoft.copilot.eclipse.core.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;

class CompletionCollectionTests {

  @Test
  void constructorThrowsExceptionWhenCompletionsIsNull() {
    assertThrows(IllegalArgumentException.class, () -> new CompletionCollection(null, "uri"));
  }

  @Test
  void constructorThrowsExceptionWhenCompletionsIsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> new CompletionCollection(Collections.emptyList(), "uri"));
  }

  @Test
  void getRemainingLinesReturnsEmptyStringWhenTextHasNoLineBreaks() {
    CompletionItem mockItem = mock(CompletionItem.class);
    List<CompletionItem> completions = List.of(mockItem);
    CompletionCollection collection = new CompletionCollection(completions, "uri");
    when(mockItem.getDisplayText()).thenReturn("single line text");
    assertEquals("", collection.getRemainingLines());
  }

  @Test
  void getFirstLineReturnsCorrectStringWhenTextHasLineBreaks() {
    CompletionItem mockItem = mock(CompletionItem.class);
    List<CompletionItem> completions = List.of(mockItem);
    CompletionCollection collection = new CompletionCollection(completions, "uri");
    when(mockItem.getDisplayText()).thenReturn("line1\nline2\nline3");
    assertEquals("line1", collection.getFirstLine());
  }

  @Test
  void getRemainingLineReturnsCorrectStringWhenTextHasLineBreaks() {
    CompletionItem mockItem = mock(CompletionItem.class);
    List<CompletionItem> completions = List.of(mockItem);
    CompletionCollection collection = new CompletionCollection(completions, "uri");
    when(mockItem.getDisplayText()).thenReturn("line1\nline2\nline3");
    assertEquals("line2\nline3", collection.getRemainingLines());
  }

  @Test
  void getNumberOfLinesReturnsCorrectNumberOfLines() {
    CompletionItem mockItem = mock(CompletionItem.class);
    List<CompletionItem> completions = List.of(mockItem);
    CompletionCollection collection = new CompletionCollection(completions, "uri");
    when(mockItem.getDisplayText()).thenReturn("line1\nline2\nline3");
    assertEquals(3, collection.getNumberOfLines());
  }

}

package com.microsoft.copilot.eclipse.core.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;

@ExtendWith(MockitoExtension.class)
class SuggestionUpdateManagerTests {

  @Test
  void getRemainingLinesReturnsEmptyStringWhenTextHasNoLineBreaks() {
    CompletionItem mockItem = mock(CompletionItem.class);
    List<CompletionItem> completions = List.of(mockItem);
    when(mockItem.getDisplayText()).thenReturn("single line text");
    SuggestionUpdateManager manager = new SuggestionUpdateManager(null);
    manager.setCompletionItems(completions);
    assertEquals("", manager.getRemainingLines());
  }

  @Test
  void getFirstLineReturnsCorrectStringWhenTextHasLineBreaks() {
    CompletionItem mockItem = mock(CompletionItem.class);
    List<CompletionItem> completions = List.of(mockItem);
    when(mockItem.getDisplayText()).thenReturn("line1\nline2\nline3");
    SuggestionUpdateManager manager = new SuggestionUpdateManager(null);
    manager.setCompletionItems(completions);
    assertEquals("line1", manager.getFirstLine());
  }

  @Test
  void getRemainingLineReturnsCorrectStringWhenTextHasLineBreaks() {
    CompletionItem mockItem = mock(CompletionItem.class);
    List<CompletionItem> completions = List.of(mockItem);
    when(mockItem.getDisplayText()).thenReturn("line1\nline2\nline3");
    SuggestionUpdateManager manager = new SuggestionUpdateManager(null);
    manager.setCompletionItems(completions);
    assertEquals("line2\nline3", manager.getRemainingLines());
  }

  @Test
  void testGetUuids() {
    List<CompletionItem> completions = List.of(new CompletionItem("uuid1", "test", null, "displayText1", null, 0),
        new CompletionItem("uuid2", "test", null, "displayText1", null, 0));
    SuggestionUpdateManager collection = new SuggestionUpdateManager(null);
    collection.setCompletionItems(completions);
    assertEquals(List.of("uuid1", "uuid2"), collection.getUuids());
  }

  @Test
  void testRejectionWhenInsertDifferentTest() {
    List<CompletionItem> completions = List
        .of(new CompletionItem("uuid1", "test displayText", null, "displayText", null, 0));
    SuggestionUpdateManager manager = new SuggestionUpdateManager(null);
    manager.setCompletionItems(completions);
    assertFalse(manager.insert("foo"));
  }

  @Test
  void testRejectionWhenDeleteTooMuchChars() {
    List<CompletionItem> completions = List
        .of(new CompletionItem("uuid1", "test displayText", null, "displayText", null, 0));
    SuggestionUpdateManager manager = new SuggestionUpdateManager(null);
    manager.setCompletionItems(completions);
    assertFalse(manager.delete(10));
  }
}

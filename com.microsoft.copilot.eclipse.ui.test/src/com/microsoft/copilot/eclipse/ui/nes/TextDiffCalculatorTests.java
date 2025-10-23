package com.microsoft.copilot.eclipse.ui.nes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.ui.nes.TextDiffCalculator.DiffSegment;
import com.microsoft.copilot.eclipse.ui.nes.TextDiffCalculator.DualDiffResult;

class TextDiffCalculatorTests {

  @Test
  void testBasicOperations() {
    // Empty strings - no diff
    DualDiffResult result = TextDiffCalculator.calculateDiff("", "");
    assertNotNull(result);
    assertEquals("", result.originalText);
    assertEquals("", result.replacementText);
    assertTrue(result.spans.isEmpty());

    // Pure insert
    result = TextDiffCalculator.calculateDiff("", "hello");
    assertNotNull(result);
    assertEquals("", result.originalText);
    assertEquals("hello", result.replacementText);
    assertTrue(result.spans.size() >= 1);
    assertEquals(DiffSegment.Type.INSERT, result.spans.get(0).type);
    assertEquals("hello", result.spans.get(0).newText);

    // Pure delete
    result = TextDiffCalculator.calculateDiff("hello", "");
    assertNotNull(result);
    assertEquals("hello", result.originalText);
    assertEquals("", result.replacementText);
    assertTrue(result.spans.size() >= 1);
    assertEquals(DiffSegment.Type.DELETE, result.spans.get(0).type);
    assertEquals("hello", result.spans.get(0).origText);

    // Complete replacement
    result = TextDiffCalculator.calculateDiff("hello", "world");
    assertNotNull(result);
    assertEquals("hello", result.originalText);
    assertEquals("world", result.replacementText);
    assertTrue(result.spans.size() >= 1);

    // Identical strings - no diff
    result = TextDiffCalculator.calculateDiff("same", "same");
    assertNotNull(result);
    assertEquals("same", result.originalText);
    assertEquals("same", result.replacementText);
    assertTrue(result.spans.isEmpty());
  }

  @Test
  void testPartialChanges() {
    // Replace in middle
    DualDiffResult result = TextDiffCalculator.calculateDiff("hello world", "hello universe");
    assertNotNull(result);
    assertEquals("hello world", result.originalText);
    assertEquals("hello universe", result.replacementText);
    assertTrue(result.spans.size() >= 1);

    // Insert in middle
    result = TextDiffCalculator.calculateDiff("hello", "hello world");
    assertNotNull(result);
    assertEquals("hello", result.originalText);
    assertEquals("hello world", result.replacementText);
    assertTrue(result.spans.size() >= 1);
    assertEquals(DiffSegment.Type.INSERT, result.spans.get(0).type);

    // Delete from end
    result = TextDiffCalculator.calculateDiff("hello world", "hello");
    assertNotNull(result);
    assertEquals("hello world", result.originalText);
    assertEquals("hello", result.replacementText);
    assertTrue(result.spans.size() >= 1);
    assertEquals(DiffSegment.Type.DELETE, result.spans.get(0).type);
  }

  @Test
  void testNewlineNormalization() {
    // CRLF normalized to LF - no diff
    DualDiffResult result = TextDiffCalculator.calculateDiff("hello\r\nworld", "hello\nworld");
    assertNotNull(result);
    assertEquals("hello\r\nworld", result.originalText);
    assertEquals("hello\nworld", result.replacementText);
    assertTrue(result.spans.isEmpty());

    // CR normalized to LF - no diff
    result = TextDiffCalculator.calculateDiff("hello\rworld", "hello\nworld");
    assertNotNull(result);
    assertEquals("hello\rworld", result.originalText);
    assertEquals("hello\nworld", result.replacementText);
    assertTrue(result.spans.isEmpty());

    // Actual content change with different line endings
    result = TextDiffCalculator.calculateDiff("hello\r\nworld", "hello\nmodified");
    assertNotNull(result);
    assertEquals("hello\r\nworld", result.originalText);
    assertEquals("hello\nmodified", result.replacementText);
    assertTrue(result.spans.size() >= 1);
  }

  @Test
  void testMultilineChanges() {
    String original = "line1\nline2\nline3";
    String replacement = "line1\nnewline\nline3";
    DualDiffResult result = TextDiffCalculator.calculateDiff(original, replacement);

    assertNotNull(result);
    assertEquals(original, result.originalText);
    assertEquals(replacement, result.replacementText);
    assertTrue(result.spans.size() >= 1);
  }

  @Test
  void testNullAndEdgeCases() {
    // Null inputs treated as empty
    DualDiffResult result = TextDiffCalculator.calculateDiff(null, null);
    assertNotNull(result);
    assertEquals("", result.originalText);
    assertEquals("", result.replacementText);
    assertTrue(result.spans.isEmpty());

    // Null original
    result = TextDiffCalculator.calculateDiff(null, "hello");
    assertNotNull(result);
    assertEquals("", result.originalText);
    assertEquals("hello", result.replacementText);
    assertTrue(result.spans.size() >= 1);
    assertEquals(DiffSegment.Type.INSERT, result.spans.get(0).type);

    // Null replacement
    result = TextDiffCalculator.calculateDiff("hello", null);
    assertNotNull(result);
    assertEquals("hello", result.originalText);
    assertEquals("", result.replacementText);
    assertTrue(result.spans.size() >= 1);
    assertEquals(DiffSegment.Type.DELETE, result.spans.get(0).type);

    // Single character change
    result = TextDiffCalculator.calculateDiff("a", "b");
    assertNotNull(result);
    assertEquals("a", result.originalText);
    assertEquals("b", result.replacementText);
    assertTrue(result.spans.size() >= 1);

    // Whitespace change
    result = TextDiffCalculator.calculateDiff("hello  world", "hello world");
    assertNotNull(result);
    assertEquals("hello  world", result.originalText);
    assertEquals("hello world", result.replacementText);
    assertTrue(result.spans.size() >= 1);
    assertEquals(DiffSegment.Type.DELETE, result.spans.get(0).type);
  }

  @Test
  void testComplexMultilineDiff() {
    String original = "public class Point2D {\n  private double x;\n  private double y;\n}";
    String replacement = "public class Point3D {\n  private double x;\n  private double y;\n  private double z;\n}";

    DualDiffResult result = TextDiffCalculator.calculateDiff(original, replacement);
    assertNotNull(result);
    assertEquals(original, result.originalText);
    assertEquals(replacement, result.replacementText);
    assertTrue(result.spans.size() >= 1);
  }
}

package com.microsoft.copilot.eclipse.ui.utils;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Utility class for completions.
 */
public class CompletionUtils {
  private CompletionUtils() {
  }

  /**
   * Normalize line endings in a string to a specific line delimiter.
   *
   * @param text the input text.
   * @param lineDelimiter the line delimiter to use.
   * @return the normalized text.
   */
  public static String normalizeLineEndings(String text, String lineDelimiter) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    String delimiter = lineDelimiter != null ? lineDelimiter : System.lineSeparator();
    String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
    if ("\n".equals(delimiter)) {
      return normalized;
    }
    return normalized.replace("\n", delimiter);
  }

  /**
   * Resolve the line delimiter for a document near the given offset.
   *
   * @param document the document to inspect.
   * @param offset the offset used to determine the line.
   * @return the resolved line delimiter.
   */
  public static String getDocumentLineDelimiter(IDocument document, int offset) {
    if (document == null) {
      return System.lineSeparator();
    }
    try {
      int safeOffset = Math.max(0, Math.min(offset, document.getLength()));
      int line = document.getLineOfOffset(safeOffset);
      String delimiter = document.getLineDelimiter(line);
      if (delimiter != null) {
        return delimiter;
      }
    } catch (BadLocationException e) {
      // fall through to fallback delimiter
    }
    String[] legalDelimiters = document.getLegalLineDelimiters();
    if (legalDelimiters != null && legalDelimiters.length > 0) {
      return legalDelimiters[0];
    }
    return System.lineSeparator();
  }

  /**
   * Replace all leading tabs for each line with spaces.
   *
   * @param input the input string.
   * @param tabSize the tab size.
   * @return the string with tabs replaced by spaces.
   */
  public static String replaceTabsWithSpaces(String input, int tabSize) {
    String[] lines = input.split("\n");
    StringBuilder result = new StringBuilder();

    for (String line : lines) {
      result.append(replaceLeadingTabs(line, tabSize)).append("\n");
    }

    // Remove the last newline character
    if (result.length() > 0) {
      result.setLength(result.length() - 1);
    }

    return result.toString();
  }

  private static String replaceLeadingTabs(String line, int tabSize) {
    int tabCount = countLeadingTabs(line);
    String spaces = " ".repeat(tabSize).repeat(tabCount);
    return spaces + line.substring(tabCount);
  }

  private static int countLeadingTabs(String line) {
    int count = 0;
    while (count < line.length() && line.charAt(count) == '\t') {
      count++;
    }
    return count;
  }
}

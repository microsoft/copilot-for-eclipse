package com.microsoft.copilot.eclipse.ui.utils;

/**
 * Utility class for completions.
 */
public class CompletionUtils {
  private CompletionUtils() {
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

package com.microsoft.copilot.eclipse.ui.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.ui.completion.EolGhostText;
import com.microsoft.copilot.eclipse.ui.completion.GhostText;
import com.microsoft.copilot.eclipse.ui.completion.InlineGhostText;

/**
 * Utility class for completions.
 */
public class CompletionUtils {
  private CompletionUtils() {
  }

  /**
   * Get the ghost texts for the completion.
   *
   * @param documentLine the line in the document where the completion is triggered.
   * @param completionLine the first line of the inline suggestion.
   * @param triggerOffset the offset where the completion is triggered in the document.
   */
  public static List<GhostText> getGhostTexts(String documentLine, String completionLine, int triggerOffset) {
    List<GhostText> ghostTexts = new ArrayList<>();
    if (documentLine.isEmpty()) {
      ghostTexts.add(new EolGhostText(completionLine, triggerOffset));
      return ghostTexts;
    }

    // strip trailing whitespaces, tabs, etc., across all platforms. These characters are not visually considered the
    // end of document line, so we should ignore them when calculating the starting point offset for ghost text
    // rendering.
    int i = StringUtils.stripEnd(documentLine, null).length() - 1;
    int j = completionLine.length() - 1;
    StringBuilder sb = new StringBuilder();

    while (i >= 0 && j >= 0) {
      if (documentLine.charAt(i) == completionLine.charAt(j)) {
        if (sb.length() > 0) {
          // passing i + 1 here because the current char indexed with i are the same, the ghost
          // text should display the content which is different from the document.
          ghostTexts.add(0, createGhostText(triggerOffset, i + 1, sb.toString(), i == documentLine.length() - 1));
          sb.setLength(0);
          continue;
        }
        i--;
        j--;
      } else {
        sb.insert(0, completionLine.charAt(j--));
      }
    }

    String remaining = sb.toString();
    if (j >= 0) {
      remaining = completionLine.substring(0, j + 1) + remaining;
    }
    if (StringUtils.isNotEmpty(remaining)) {
      ghostTexts.add(0, createGhostText(triggerOffset, i, remaining, i == documentLine.length() - 1));
    }

    return ghostTexts;
  }

  private static GhostText createGhostText(int base, int offset, String text, boolean isEol) {
    // use Math.max to avoid negative offset (i may == -1 after the while loop))
    int position = isEol ? base + offset : Math.max(base + offset, base);
    return isEol ? new EolGhostText(text, position) : new InlineGhostText(text, position);
  }
}

package com.microsoft.copilot.eclipse.core.completion;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;

/**
 * A class to hold data for the inline completion.
 */
public class CompletionCollection {

  private List<CompletionItem> completions;
  private int index;
  private String uriString;

  /**
   * Creates a new CompletionData.
   */
  public CompletionCollection(@NonNull List<CompletionItem> completions, String uriString) {
    if (completions == null || completions.isEmpty()) {
      var ex = new IllegalArgumentException("completions cannot be null or empty");
      CopilotCore.LOGGER.log(LogLevel.ERROR, ex);
      throw ex;
    }
    this.completions = completions;
    this.uriString = uriString;
    this.index = 0;
  }

  /**
   * Get text for the current active completion item.
   */
  public String getText() {
    CompletionItem item = getCurrentItem();
    if (item == null) {
      return "";
    }
    return item.getDisplayText();
  }

  /**
   * Get the first line of the current active completion item.
   */
  public String getFirstLine() {
    String text = getText();
    if (text == null) {
      return "";
    }
    return text.split("\n")[0];
  }

  /**
   * Get the remaining lines of the current active completion item.
   */
  public String getRemainingLines() {
    String text = getText();
    if (text == null) {
      return "";
    }
    int lineBreakIdx = text.indexOf("\n");
    if (lineBreakIdx < 0) {
      return "";
    }
    return text.substring(lineBreakIdx + 1);
  }

  /**
   * Get the number of items in the completion list.
   */
  public int getSize() {
    return this.completions.size();
  }

  /**
   * Get the document version when the completion was triggered.
   */
  public int getDocumentVersion() {
    if (this.completions.isEmpty()) {
      throw new IllegalStateException("completions cannot be empty");
    }
    return this.completions.get(0).getDocVersion();
  }

  public String getUriString() {
    return this.uriString;
  }

  /**
   * Get the number of lines for the active completion text.
   */
  public int getNumberOfLines() {
    return this.getText().split("\n").length;
  }

  /**
   * Get the position where the completion was triggered.
   */
  public Position getTriggerPosition() {
    if (this.completions.isEmpty()) {
      throw new IllegalStateException("completions cannot be empty");
    }
    return this.completions.get(index).getPosition();
  }

  /**
   * Get the range for the completion.
   */
  public Range getRange() {
    if (this.completions.isEmpty()) {
      throw new IllegalStateException("completions cannot be empty");
    }
    return this.completions.get(index).getRange();
  }

  public List<String> getUuids() {
    return this.completions.stream().map(CompletionItem::getUuid).toList();
  }

  /**
   * Get the current active completion item.
   */
  public CompletionItem getCurrentItem() {
    return this.completions.get(index);
  }

}

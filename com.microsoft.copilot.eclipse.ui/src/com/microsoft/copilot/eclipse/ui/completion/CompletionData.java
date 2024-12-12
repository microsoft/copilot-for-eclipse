package com.microsoft.copilot.eclipse.ui.completion;

import java.util.List;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;

/**
 * A class to hold data for the inline completion.
 */
public class CompletionData {

  /**
   * A constant for an empty completion item. This is used to indicate that the completion is not available. Thus when
   * clear the ghost text, we can set it to the rendering.
   */
  public static final CompletionItem EMPTY_ITEM = new CompletionItem("", "", null, "", null, -1);

  private List<CompletionItem> items;
  private int index;
  private int triggerOffset;

  /**
   * Creates a new CompletionData.
   */
  public CompletionData() {
    this.index = 0;
    this.triggerOffset = -1;
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
    if (items == null) {
      return 0;
    }
    return items.size();
  }

  /**
   * Get the document version when the completion was triggered.
   */
  public int getDocumentVersion() {
    CompletionItem item = getCurrentItem();
    if (item == null) {
      return -1;
    }
    return item.getDocVersion();
  }

  /**
   * Set the completion items.
   */
  public void setItems(List<CompletionItem> items) {
    this.items = items;
    this.index = 0;
  }

  public int getTriggerOffset() {
    return triggerOffset;
  }

  public void setTriggerOffset(int offset) {
    this.triggerOffset = offset;
  }

  /**
   * Get the number of lines for the active completion text.
   */
  public int getNumberOfLines() {
    return this.getText().split("\n").length;
  }

  /**
   * Get the current active completion item.
   */
  CompletionItem getCurrentItem() {
    if (items == null || items.isEmpty() || index >= items.size()) {
      return null;
    }
    return items.get(index);
  }

}

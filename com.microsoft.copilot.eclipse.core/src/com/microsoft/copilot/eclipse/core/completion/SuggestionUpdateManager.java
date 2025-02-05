package com.microsoft.copilot.eclipse.core.completion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.Position;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;

/**
 * Manage the suggestion updates. For example, when the user types a character, the suggestion list should be updated.
 */
public class SuggestionUpdateManager {

  /**
   * The document where the manager is working on.
   */
  private IDocument document;

  /**
   * The original completion items that are provided by the language server.
   */
  private List<CompletionItem> originalItems;

  /**
   * Updated items based on the original items and the user input.
   */
  private List<CompletionItem> updatedItems;
  /**
   * Current index that is being displayed in the completion list.
   */
  private int index;

  /**
   * The offset of the current completion item since it gets displayed.
   */
  private int offset;

  /**
   * Creates a new SuggestionUpdateManager.
   */
  public SuggestionUpdateManager(IDocument document) {
    this.document = document;
    this.originalItems = new ArrayList<>();
    this.updatedItems = new ArrayList<>();
    this.index = 0;
    this.offset = 0;
  }

  /**
   * When user type new input, update the suggestion list based on the user input.
   *
   * @return <code>true</code> if the update is accepted, <code>false</code> otherwise.
   */
  public boolean insert(String text) {
    if (this.updatedItems == null || this.updatedItems.isEmpty()) {
      throw new IllegalStateException("Cannot insert text when there are no items");
    }
    List<CompletionItem> newItems = new ArrayList<>();
    for (CompletionItem item : this.updatedItems) {
      if (item.getDisplayText().startsWith(text)) {
        String newDisplayText = item.getDisplayText().substring(text.length());
        if (newDisplayText.isEmpty()) {
          continue;
        }
        try {
          // the following update might be wrong especially for the replacement range. Another request should be
          // triggered to get the correct items.
          Position newTriggerPosition = LSPEclipseUtils
              .toPosition(LSPEclipseUtils.toOffset(item.getPosition(), this.document) + text.length(), this.document);
          item.getRange().setEnd(newTriggerPosition);
          newItems.add(new CompletionItem(item.getUuid(), item.getText(), item.getRange(), newDisplayText,
              newTriggerPosition, item.getDocVersion()));
        } catch (BadLocationException e) {
          CopilotCore.LOGGER.error("Could not update the trigger position", e);
        }
      }
    }
    boolean accepted = !newItems.isEmpty();
    if (accepted) {
      this.updatedItems = newItems;
      this.offset += text.length();
    } else {
      this.reset();
    }
    return accepted;

  }

  /**
   * When user delete characters, update the suggestion list based on the user input.
   *
   * @return true if the update is accepted, false otherwise
   */
  public boolean delete(int deletedCount) {
    if (this.updatedItems == null || this.updatedItems.isEmpty()) {
      throw new IllegalStateException("Cannot delete text when there are no items");
    }
    if (this.offset == 0 || this.offset < deletedCount) {
      this.reset();
      return false;
    }
    List<CompletionItem> newItems = new ArrayList<>();
    for (CompletionItem item : this.originalItems) {
      String newDisplayText = item.getDisplayText().substring(this.offset - deletedCount);
      try {
        // the following update might be wrong especially for the replacement range. Another request should be
        // triggered to get the correct items.
        Position newTriggerPosition = LSPEclipseUtils
            .toPosition(LSPEclipseUtils.toOffset(item.getPosition(), this.document) - deletedCount, this.document);
        item.getRange().setEnd(newTriggerPosition);
        newItems.add(new CompletionItem(item.getUuid(), item.getText(), item.getRange(), newDisplayText,
            newTriggerPosition, item.getDocVersion()));
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error("Could not update the trigger position", e);
      }
    }

    boolean accepted = !newItems.isEmpty();
    if (accepted) {
      this.updatedItems = newItems;
      this.offset -= deletedCount;
    } else {
      this.reset();
    }
    return accepted;
  }

  /**
   * Get the next word for the current active completion item.
   */
  public String getNextWord() {
    CompletionItem item = getCurrentItem();
    if (item == null) {
      throw new IllegalStateException("Cannot get text when there are no items");
    }
    String fullCompletion = item.getDisplayText();
    int whitespaceBlock = findContinuousBlock(fullCompletion, Character::isWhitespace);
    if (whitespaceBlock != -1) {
      return fullCompletion.substring(0, whitespaceBlock);
    }
    int symbolBlock = findContinuousBlock(fullCompletion, this::isBoundaryCharacter);
    if (symbolBlock != -1) {
      return fullCompletion.substring(0, symbolBlock);
    }
    int otherBlock = findContinuousBlock(fullCompletion, c -> !(isBoundaryCharacter(c) || Character.isWhitespace(c)));
    if (otherBlock != -1) {
      return fullCompletion.substring(0, otherBlock);
    }
    return fullCompletion;
  }

  private int findContinuousBlock(String fullCompletion, BoundaryFinder isBoundaryCharacter) {
    int endIndex = 0;
    if (!isBoundaryCharacter.isBoundaryCharacter(fullCompletion.charAt(endIndex))) {
      return -1;
    }
    while (endIndex < fullCompletion.length()
        && isBoundaryCharacter.isBoundaryCharacter(fullCompletion.charAt(endIndex))) {
      endIndex++;
    }
    return endIndex;
  }

  interface BoundaryFinder {

    boolean isBoundaryCharacter(char c);

  }

  private boolean isBoundaryCharacter(char c) {
    return "~!@#$%^&*()-=+[{]}\\|;:'\",.<>/?".indexOf(c) != -1;
  }

  /**
   * Initialize the completion items when the suggestion is resolved. It will do a entire flush when the original items
   * are empty. Otherwise, it will only update the updated items as a correction.
   */
  public void setCompletionItems(List<CompletionItem> items) {
    if (originalItems == null || originalItems.isEmpty()) {
      this.originalItems = items;
      this.updatedItems = items;
      this.offset = 0;
      this.index = 0;
    } else {
      this.updatedItems = items;
    }
  }

  /**
   * Clear the holding completion items.
   */
  public void reset() {
    this.originalItems = new ArrayList<>();
    this.updatedItems = new ArrayList<>();
    this.index = 0;
    this.offset = 0;
  }

  /**
   * Get the current active completion item. return null if there is no active item.
   */
  public CompletionItem getCurrentItem() {
    if (this.updatedItems.isEmpty()) {
      return null;
    }
    if (index < 0 || index >= updatedItems.size()) {
      throw new IllegalStateException("index out of range to get updated completion item.");
    }
    return this.updatedItems.get(index);
  }

  /**
   * Get the text of the current active completion item.
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
    return text.split("\r?\n")[0];
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

  public List<String> getUuids() {
    return this.updatedItems.stream().map(CompletionItem::getUuid).toList();
  }

  public int getSize() {
    return this.updatedItems.size();
  }
}

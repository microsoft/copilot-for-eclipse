// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A legacy completion manager implementation for Eclipse versions that don't support code mining. This implementation
 * uses the basic ghost text rendering approach.
 */
public class CompletionManagerLegacy extends BaseCompletionManager {
  private RenderingManager renderingManager;

  /**
   * Creates a new legacy completion manager for environments without code mining support.
   */
  public CompletionManagerLegacy(CopilotLanguageServerConnection lsConnection, CompletionProvider provider,
      ITextEditor editor, LanguageServerSettingManager settingsManager) {
    super(lsConnection, provider, editor, settingsManager);
    this.renderingManager = new RenderingManager(this.textViewer);
  }

  /**
   * Update the ghost texts for rendering using the basic rendering approach.
   */
  @Override
  protected void updateGhostTexts(Position position) {
    List<GhostText> ghostTexts = resolveGhostTexts(position);
    this.renderingManager.setGhostTexts(ghostTexts);
    this.renderingManager.redraw();
  }

  /**
   * Resolve the ghost texts based on the current suggestion update manager and the document content.
   *
   * @return a list of ghost texts to be rendered.
   */
  private List<GhostText> resolveGhostTexts(Position position) {
    if (this.suggestionUpdateManager.getSize() == 0) {
      return Collections.emptyList();
    }

    List<GhostText> ghostTexts = new ArrayList<>();

    String firstLine = this.suggestionUpdateManager.getFirstLine();
    if (StringUtils.isNotEmpty(firstLine)) {
      String documentContent = this.document.get();
      int triggerOffset = position.getOffset();
      String documentLine = "";
      try {
        int lineOffset = document.getLineOfOffset(triggerOffset);
        if (lineOffset == document.getNumberOfLines() - 1) {
          // this is the last line
          documentLine = documentContent.substring(triggerOffset);
        } else {
          for (int i = triggerOffset; i < this.document.getLength(); i++) {
            if (isNewLineCharacter(documentContent, i)) {
              documentLine = documentContent.substring(triggerOffset, i);
              break;
            }
          }
        }
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error(e);
      }
      ghostTexts.addAll(getGhostTexts(documentLine, firstLine, triggerOffset));
    }

    String remainingLines = this.suggestionUpdateManager.getRemainingLines();
    if (StringUtils.isNotEmpty(remainingLines)) {
      ghostTexts.add(new BlockGhostText(remainingLines, position.offset, this.document));
    }

    return ghostTexts;
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
    if (documentLine.isBlank()) {
      ghostTexts.add(new InlineGhostText(completionLine, triggerOffset));
      return ghostTexts;
    }

    // strip trailing whitespaces, tabs, etc., across all platforms. These characters are not visually considered the
    // end of document line, so we should ignore them when calculating the starting point offset for ghost text
    // rendering.
    int i = Math.max(0, StringUtils.stripEnd(documentLine, null).length() - 1);
    int j = completionLine.length() - 1;
    StringBuilder sb = new StringBuilder();

    while (i >= 0 && j >= 0) {
      if (documentLine.charAt(i) == completionLine.charAt(j)) {
        if (sb.length() > 0) {
          // passing i + 1 here because the current char indexed with i are the same, the ghost
          // text should display the content which is different from the document.
          // while calculating 'i' here, we use the original document line without stripping trailing whitespaces since
          // if there are trailing whitespaces, the ghost text should always be the inline ghost text instead of the eol
          // ghost text.
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

  /**
   * Clear the completion ghost text.
   */
  @Override
  public void clearGhostTexts() {
    disableContext();
    if (this.renderingManager != null) {
      this.suggestionUpdateManager.reset();
    }
    if (this.renderingManager != null) {
      this.renderingManager.clearGhostText();

      // Clear vertical indentation for the updated trigger position when the completion is accepted, the trigger
      // position here stands for the caret position.
      this.renderingManager.resetLineVerticalIndentationAtWidgetOffset(
          UiUtils.modelOffset2WidgetOffset(textViewer, triggerPosition.getOffset()));
    }
  }
}
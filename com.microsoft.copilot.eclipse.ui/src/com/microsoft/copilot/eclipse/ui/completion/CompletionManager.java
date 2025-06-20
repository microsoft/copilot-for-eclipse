package com.microsoft.copilot.eclipse.ui.completion;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.AbstractCodeMining;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.completion.codemining.BlockGhostText;
import com.microsoft.copilot.eclipse.ui.completion.codemining.LineContentGhostText;
import com.microsoft.copilot.eclipse.ui.completion.codemining.LineEndGhostText;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;

/**
 * A class to listen events which are completion related and notify the completion manager to render the ghost text or
 * apply the suggestion to document.
 */
public class CompletionManager extends BaseCompletionManager {

  /**
   * Creates a new completion manager. The manager is responsible for trigger the completion, apply suggestions to the
   * document. And schedule the rendering of ghost text.
   */
  public CompletionManager(CopilotLanguageServerConnection lsConnection, CompletionProvider provider,
      ITextEditor editor, LanguageServerSettingManager settingsManager) {
    super(lsConnection, provider, editor, settingsManager);
  }

  /**
   * Update the ghost texts for rendering.
   */
  @Override
  protected void updateGhostTexts() {
    resolveCodeMiningGhostTexts();
    this.updateCodeMinings();
  }

  /**
   * Clear the completion ghost text.
   */
  @Override
  public void clearGhostTexts() {
    disableContext();
    this.suggestionUpdateManager.reset();
    this.codeMinings.clear();
    this.updateCodeMinings();

    // Clear legacy vertical indentation for the block ghost text line when the line is out of the visible range.
    // Fix issue: https://github.com/microsoft/copilot-eclipse/issues/105
    redrawBlockLineAtModelOffset(this.cachedModelOffset);
  }

  private void updateCodeMinings() {
    if (textViewer instanceof ISourceViewerExtension5 sve) {
      sve.updateCodeMinings();
    }
  }

  private void resolveCodeMiningGhostTexts() {
    if (this.suggestionUpdateManager.getSize() == 0) {
      this.codeMinings.clear();
      return;
    }
    List<ICodeMining> cm = new ArrayList<>();
    String firstLine = this.suggestionUpdateManager.getFirstLine();

    if (StringUtils.isNotEmpty(firstLine)) {
      try {
        cm.addAll(getCodeMiningGhostTexts(triggerPosition, this.document, getCurrentLine(), firstLine));
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error(e);
      }
    }

    String remainingLines = this.suggestionUpdateManager.getRemainingLines();
    if (StringUtils.isNotEmpty(remainingLines)) {
      try {
        int lineOffset = document.getLineOfOffset(triggerPosition.offset) + 1;
        if (lineOffset >= document.getNumberOfLines()) {
          return;
        }
        cm.add(new BlockGhostText(lineOffset, document, null, remainingLines));
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error(e);
      }
    }
    this.codeMinings = cm;
  }

  private List<AbstractCodeMining> getCodeMiningGhostTexts(Position position, IDocument document, String documentLine,
      String completionLine) throws BadLocationException {

    // LineContentCodeMining for eclipse 2024-12 requires position length > 0, this is a work around.
    Position lineContentPostion = new Position(position.getOffset(), position.getLength());
    if (lineContentPostion.getLength() == 0 && position.getOffset() + 1 <= document.getLength()) {
      lineContentPostion.setLength(1);
    }

    List<AbstractCodeMining> ghostTexts = new ArrayList<>();
    if (documentLine.isEmpty()) {
      ghostTexts
          .add(new LineEndGhostText(document, document.getLineOfOffset(position.getOffset()), null, completionLine));
      return ghostTexts;
    }
    if (documentLine.isBlank()) {
      ghostTexts.add(new LineContentGhostText(position, true, null, completionLine));
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
          ghostTexts.add(0, createCodeMiningGhostText(document, position.getOffset(), i + 1, sb.toString(),
              i == documentLine.length() - 1));
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
      ghostTexts.add(0,
          createCodeMiningGhostText(document, position.getOffset(), i, remaining, i == documentLine.length() - 1));
    }

    return ghostTexts;
  }

  private AbstractCodeMining createCodeMiningGhostText(IDocument document, int base, int offset, String text,
      boolean isEol) throws BadLocationException {
    // use Math.max to avoid negative offset (i may == -1 after the while loop))
    int position = isEol ? base + offset : Math.max(base + offset, base);
    return isEol ? new LineEndGhostText(document, document.getLineOfOffset(position), null, text)
        : new LineContentGhostText(new Position(position, 1), true, null, text);
  }
}

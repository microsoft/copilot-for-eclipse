package com.microsoft.copilot.eclipse.ui.completion;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.format.FormatOptionProvider;
import com.microsoft.copilot.eclipse.ui.utils.CompletionUtils;

/**
 * A ghost text placed below the last line of the document. For normal block ghost text, we use code mining API to
 * display the ghost text.
 */
public class BlockBottomGhostText extends GhostText {

  /**
   * Creates a new EolGhostText.
   */
  public BlockBottomGhostText(String text, int modelOffset, IDocument document) {
    super(text, modelOffset, GhostTextType.BELOW_LAST_LINE);
    IFile file = LSPEclipseUtils.getFile(document);
    if (file == null) {
      return;
    }
    FormatOptionProvider formatOptionProvider = CopilotCore.getPlugin().getFormatOptionProvider();
    if (formatOptionProvider == null) {
      return;
    }
    boolean useSpace = formatOptionProvider.useSpace(file);
    if (useSpace) {
      return;
    }
    int tabSize = formatOptionProvider.getTabSize(file);
    String replacedText = CompletionUtils.replaceTabsWithSpaces(this.text, tabSize);
    this.text = replacedText;
  }

  @Override
  public void draw(StyledText styledText, int widgetOffset, GC gc) {
    if (StringUtils.isNotBlank(this.text)) {
      Rectangle bounds = styledText.getTextBounds(widgetOffset, widgetOffset);
      int y = bounds.y + styledText.getLineHeight();
      gc.drawText(this.text, styledText.getLeftMargin(), y, true);
    }
  }
}

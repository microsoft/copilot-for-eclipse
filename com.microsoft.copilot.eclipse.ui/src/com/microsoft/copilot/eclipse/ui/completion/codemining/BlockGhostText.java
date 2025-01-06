package com.microsoft.copilot.eclipse.ui.completion.codemining;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.jface.text.source.inlined.Positions;

/**
 * A block of ghost text with multiple lines placed in new lines. We use code mining API to display the ghost text.
 */
public class BlockGhostText extends LineHeaderCodeMining {

  /**
   * Creates a new BlockGhostText.
   */
  public BlockGhostText(int beforeLineNumber, IDocument document, ICodeMiningProvider provider, String text)
      throws BadLocationException {
    super(Positions.of(beforeLineNumber, document, false), provider, null);
    this.setLabel(text);
  }

}

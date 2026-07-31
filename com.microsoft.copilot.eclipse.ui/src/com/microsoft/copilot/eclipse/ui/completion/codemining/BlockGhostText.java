// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion.codemining;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.jface.text.source.inlined.Positions;

/**
 * A block of ghost text with multiple lines placed in new lines. We use code mining API to display the ghost text.
 */
public class BlockGhostText extends LineHeaderCodeMining {

  /**
   * Creates a new BlockGhostText.
   *
   * @param beforeLineNumber the line number before which the block ghost text is displayed.
   * @param document the document containing the block ghost text.
   * @param provider the code mining provider creating this ghost text.
   * @param text the ghost text to display.
   */
  public BlockGhostText(int beforeLineNumber, IDocument document, ICodeMiningProvider provider, String text)
      throws BadLocationException {
    super(Positions.of(beforeLineNumber, document, false), provider, null);
    this.setLabel(text);
  }

  /**
   * Creates a new BlockGhostText. (for testing purpose)
   *
   * @param position the position where the block ghost text is displayed.
   * @param provider the code mining provider creating this ghost text.
   * @param text the ghost text to display.
   */
  public BlockGhostText(Position position, ICodeMiningProvider provider, String text) throws BadLocationException {
    super(position, provider, null);
    this.setLabel(text);
  }

}

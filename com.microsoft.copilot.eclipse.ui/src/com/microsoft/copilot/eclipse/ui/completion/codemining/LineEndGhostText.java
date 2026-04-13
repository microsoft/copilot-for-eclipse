// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion.codemining;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineEndCodeMining;

/**
 * A ghost text that is displayed inline with the code, using the code mining API. This class is used to display a
 * single line of ghost text at a specific line number.
 */
public class LineEndGhostText extends LineEndCodeMining {

  /**
   * Creates a new LineEndGhostText.
   */
  public LineEndGhostText(IDocument document, int line, ICodeMiningProvider provider, String text)
      throws BadLocationException {
    super(document, line, provider);
    this.setLabel(text);
  }

  @Override
  public boolean isAfterPosition() {
    return true;
  }
}

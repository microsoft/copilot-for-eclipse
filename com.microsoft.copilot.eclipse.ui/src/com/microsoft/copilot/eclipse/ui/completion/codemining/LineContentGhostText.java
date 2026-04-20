// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion.codemining;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineContentCodeMining;

/**
 * A ghost text that is displayed inline with the code, using the code mining API. This class is used to display a
 * single line of ghost text at a specific position.
 */
public class LineContentGhostText extends LineContentCodeMining {

  /**
   * Creates a new LineContentCodeMining.
   */
  public LineContentGhostText(Position position, boolean afterPosition, ICodeMiningProvider provider, String text)
      throws BadLocationException {
    super(position, null);
    this.setLabel(text);
  }

  @Override
  public boolean isAfterPosition() {
    return true;
  }
}

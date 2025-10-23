package com.microsoft.copilot.eclipse.core.nes;

import org.eclipse.core.resources.IFile;

import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsResult.CopilotInlineEdit;

/**
 * Listener for Next Edit Suggestions (NES).
 */
public interface NextEditSuggestionListener {

  /**
   * Called when a NES request completes.
   *
   * @param file Eclipse file associated with the edit
   * @param edit inline edit suggestion
   */
  void onNextEditSuggestion(IFile file, CopilotInlineEdit edit);
}

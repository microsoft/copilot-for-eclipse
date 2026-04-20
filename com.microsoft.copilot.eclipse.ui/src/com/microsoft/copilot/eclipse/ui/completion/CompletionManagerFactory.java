// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion;

import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.IdeCapabilities;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;

/**
 * Factory class to create a completion manager based on IDE capabilities.
 */
public class CompletionManagerFactory {

  /**
   * Creates a completion manager based on IDE capabilities.
   *
   * @param lsConnection The Copilot language server connection
   * @param provider The completion provider
   * @param editor The text editor
   * @param settingsManager The language server settings manager
   * @return A completion manager instance - either CompletionManager or LegacyCompletionManager
   */
  public static BaseCompletionManager createCompletionManager(CopilotLanguageServerConnection lsConnection,
      CompletionProvider provider, ITextEditor editor, LanguageServerSettingManager settingsManager) {

    if (IdeCapabilities.canUseCodeMining()) {
      return new CompletionManager(lsConnection, provider, editor, settingsManager);
    } else {
      return new CompletionManagerLegacy(lsConnection, provider, editor, settingsManager);
    }
  }
}
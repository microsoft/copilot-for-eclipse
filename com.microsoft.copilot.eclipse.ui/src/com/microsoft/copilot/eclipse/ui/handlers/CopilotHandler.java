package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jdt.annotation.Nullable;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.CompletionHandler;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;

/**
 * Base class for Copilot handlers.
 */
public abstract class CopilotHandler extends AbstractHandler {
  /**
   * Gets the active {@link CompletionHandler} for the current editor.
   */
  @Nullable
  public CompletionHandler getActiveCompletionHandler() {
    CopilotUi copilotUi = CopilotUi.getPlugin();
    if (copilotUi == null) {
      return null;
    }
    EditorsManager manager = copilotUi.getEditorsManager();
    if (manager == null) {
      return null;
    }
    return manager.getActiveCompletionHandler();
  }

  public CopilotLanguageServerConnection getLanguageServerConnection() {
    return CopilotCore.getPlugin().getCopilotLanguageServer();
  }
}

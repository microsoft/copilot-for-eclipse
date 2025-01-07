package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jdt.annotation.Nullable;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.CopilotStatusManager;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;

/**
 * Base class for Copilot handlers.
 */
public abstract class CopilotHandler extends AbstractHandler {
  /**
   * Gets the active {@link CompletionManager} for the current editor.
   */
  @Nullable
  public CompletionManager getActiveCompletionManager() {
    CopilotUi copilotUi = CopilotUi.getPlugin();
    if (copilotUi == null) {
      return null;
    }
    EditorsManager manager = copilotUi.getEditorsManager();
    if (manager == null) {
      return null;
    }
    return manager.getActiveCompletionManager();
  }

  public CopilotLanguageServerConnection getLanguageServerConnection() {
    return CopilotCore.getPlugin().getCopilotLanguageServer();
  }
  
  public CopilotStatusManager getCopilotStatusManager() {
    return CopilotUi.getPlugin().getCopilotStatusManager();
  }
}

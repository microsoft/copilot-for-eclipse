package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jdt.annotation.Nullable;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.nes.RenderManager;

/**
 * Base class for Copilot handlers.
 */
public abstract class CopilotHandler extends AbstractHandler {
  /**
   * Gets the active {@link BaseCompletionManager} for the current editor.
   */
  @Nullable
  public BaseCompletionManager getActiveCompletionManager() {
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

  /**
   * Gets the active {@link RenderManager} for the current editor.
   */
  @Nullable
  public RenderManager getActiveNesRenderManager() {
    CopilotUi copilotUi = CopilotUi.getPlugin();
    if (copilotUi == null) {
      return null;
    }
    EditorsManager manager = copilotUi.getEditorsManager();
    if (manager == null) {
      return null;
    }
    return manager.getActiveNesRenderManager();
  }

}

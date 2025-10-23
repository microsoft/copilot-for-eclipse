package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.copilot.eclipse.core.completion.AcceptSuggestionType;
import com.microsoft.copilot.eclipse.core.completion.SuggestionUpdateManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyAcceptedParams;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.nes.RenderManager;

/**
 * Handler for accepting the full suggestion.
 */
public class AcceptFullSuggestionHandler extends CopilotHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    // Priority: completion suggestion first; fallback to NES only if no completion item.
    BaseCompletionManager handler = getActiveCompletionManager();
    if (handler != null) {
      SuggestionUpdateManager updateManager = handler.getSuggestionUpdateManager();
      if (updateManager != null) {
        CompletionItem item = updateManager.getCurrentItem();
        if (item != null) {
          notifyAccepted(updateManager);
          handler.acceptSuggestion(AcceptSuggestionType.FULL);
          return null;
        }
      }
    }
    // No completion item -> try NES via EditorsManager
    RenderManager nesManager = getActiveNesRenderManager();
    if (nesManager != null) {
      nesManager.handleTabAcceptOrReveal();
    }
    return null;
  }

  @Override
  public boolean isEnabled() {
    // Enabled if: there is a completion item OR (no completion but NES present)
    BaseCompletionManager manager = getActiveCompletionManager();
    if (manager != null) {
      SuggestionUpdateManager suggestionUpdateManager = manager.getSuggestionUpdateManager();
      if (suggestionUpdateManager != null) {
        CompletionItem item = suggestionUpdateManager.getCurrentItem();
        if (item != null) {
          return true;
        }
      }
    }
    EditorsManager mgr = CopilotUi.getPlugin().getEditorsManager();
    RenderManager nesManager = mgr != null ? mgr.getActiveNesRenderManager() : null;
    return nesManager != null && nesManager.hasActiveSuggestion();
  }

  private void notifyAccepted(SuggestionUpdateManager manager) {
    if (manager == null) {
      return;
    }

    CompletionItem item = manager.getCurrentItem();
    if (item == null) {
      return;
    }
    String uuid = item.getUuid();
    NotifyAcceptedParams params = new NotifyAcceptedParams(uuid);
    getLanguageServerConnection().notifyAccepted(params);
  }
}

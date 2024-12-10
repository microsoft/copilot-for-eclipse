package com.microsoft.copilot.eclipse.ui.completion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;

/**
 * Manages the completion handlers for all available ITextEditors.
 */
public class EditorsManager {

  private CopilotLanguageServerConnection languageServer;
  private Map<ITextEditor, CompletionHandler> editorMap;

  /**
   * Creates a new EditorManager.
   */
  public EditorsManager(CopilotLanguageServerConnection languageServer) {
    this.languageServer = languageServer;
    this.editorMap = new ConcurrentHashMap<>();
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionHandler CompletionHandler} for the given
   * ITextEditor. If it does not exist, a new one will be created. Returns <code>null</code> if the editor is
   * <code>null</code>.
   */
  public CompletionHandler getOrCreateCompletionHandlerFor(ITextEditor editor) {
    if (editor == null) {
      return null;
    }

    return editorMap.computeIfAbsent(editor, edt -> new CompletionHandler(this.languageServer, edt));
  }

  /**
   * Disposes the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionHandler CompletionHandler} for the given
   * ITextEditor.
   */
  public void disposeCompletionHandlerFor(ITextEditor editor) {
    CompletionHandler handler = editorMap.remove(editor);
    if (handler != null) {
      handler.dispose();
    }
  }

  /**
   * Dispose all the handlers.
   */
  public void dispose() {
    for (CompletionHandler handler : this.editorMap.values()) {
      handler.dispose();
    }
    this.editorMap.clear();
  }

}

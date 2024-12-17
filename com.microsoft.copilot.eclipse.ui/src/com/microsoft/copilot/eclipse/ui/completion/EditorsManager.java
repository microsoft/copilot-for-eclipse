package com.microsoft.copilot.eclipse.ui.completion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;

/**
 * Manages the completion handlers for all available ITextEditors.
 */
public class EditorsManager {

  private CopilotLanguageServerConnection languageServer;
  private CompletionProvider completionProvider;
  private Map<ITextEditor, CompletionHandler> editorMap;
  private AtomicReference<ITextEditor> activeEditor;

  /**
   * Creates a new EditorManager.
   */
  public EditorsManager(CopilotLanguageServerConnection languageServer, CompletionProvider completionProvider) {
    this.languageServer = languageServer;
    this.completionProvider = completionProvider;
    this.editorMap = new ConcurrentHashMap<>();
    this.activeEditor = new AtomicReference<>();
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

    return editorMap.computeIfAbsent(editor,
        edt -> new CompletionHandler(this.languageServer, this.completionProvider, edt));
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionHandler CompletionHandler} for the active
   * ITextEditor.
   */
  @Nullable
  public CompletionHandler getActiveCompletionHandler() {
    if (this.activeEditor.get() == null) {
      return null;
    }
    return this.editorMap.get(activeEditor.get());
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
   * Sets the active editor.
   */
  public void setActiveEditor(ITextEditor editor) {
    this.activeEditor.set(editor);
  }

  @Nullable
  public ITextEditor getActiveEditor() {
    return this.activeEditor.get();
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

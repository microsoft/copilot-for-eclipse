package com.microsoft.copilot.eclipse.ui.completion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Manages the completion managers for all available ITextEditors.
 */
public class EditorsManager {

  private CopilotLanguageServerConnection languageServer;
  private CompletionProvider completionProvider;
  private Map<ITextEditor, CompletionManager> editorMap;
  private AtomicReference<ITextEditor> activeEditor;
  private IPreferenceStore preferenceStore;

  /**
   * Creates a new EditorManager.
   */
  public EditorsManager(CopilotLanguageServerConnection languageServer, CompletionProvider completionProvider,
      IPreferenceStore preferenceStore) {
    this.languageServer = languageServer;
    this.completionProvider = completionProvider;
    this.editorMap = new ConcurrentHashMap<>();
    this.activeEditor = new AtomicReference<>();
    this.preferenceStore = preferenceStore;
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionManager CompletionManager} for the given
   * ITextEditor. If it does not exist, a new one will be created. Returns <code>null</code> if the editor is
   * <code>null</code>.
   */
  public CompletionManager getOrCreateCompletionManagerFor(ITextEditor editor) {
    if (editor == null) {
      return null;
    }

    ITextViewer textViewer = (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
    if (!SwtUtils.isEditable(textViewer)) {
      return null;
    }

    return editorMap.computeIfAbsent(editor,
        edt -> new CompletionManager(this.languageServer, this.completionProvider, edt, this.preferenceStore));
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionManager CompletionManager} for the given
   * ITextEditor. Returns <code>null</code> if there is no manager for the editor.
   */
  public CompletionManager getCompletionManagerFor(ITextEditor editor) {
    if (editor == null) {
      return null;
    }

    return editorMap.get(editor);
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionManager CompletionManager} for the active
   * ITextEditor.
   */
  @Nullable
  public CompletionManager getActiveCompletionManager() {
    if (this.activeEditor.get() == null) {
      return null;
    }
    return this.editorMap.get(activeEditor.get());
  }

  /**
   * Disposes the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionManager CompletionHandler} for the given
   * ITextEditor.
   */
  public void disposeCompletionManagerFor(ITextEditor editor) {
    CompletionManager handler = editorMap.remove(editor);
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
   * Dispose all the managers.
   */
  public void dispose() {
    for (CompletionManager handler : this.editorMap.values()) {
      handler.dispose();
    }
    this.editorMap.clear();
  }

}

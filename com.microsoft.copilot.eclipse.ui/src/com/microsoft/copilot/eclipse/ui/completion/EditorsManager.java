package com.microsoft.copilot.eclipse.ui.completion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Manages the completion managers for all available ITextEditors.
 */
public class EditorsManager {

  private CopilotLanguageServerConnection languageServer;
  private CompletionProvider completionProvider;
  private Map<ITextEditor, CompletionManager> editorMap;
  private AtomicReference<ITextEditor> activeEditor;
  private LanguageServerSettingManager settingsManager;

  /**
   * Creates a new EditorManager.
   */
  public EditorsManager(CopilotLanguageServerConnection languageServer, CompletionProvider completionProvider,
      LanguageServerSettingManager settingsManager) {
    this.languageServer = languageServer;
    this.completionProvider = completionProvider;
    this.editorMap = new ConcurrentHashMap<>();
    this.activeEditor = new AtomicReference<>();
    this.settingsManager = settingsManager;
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionManager CompletionManager} for the given
   * ITextEditor. If it does not exist, a new one will be created. Returns <code>null</code> if the editor is
   * <code>null</code>.
   */
  @Nullable
  public CompletionManager getOrCreateCompletionManagerFor(ITextEditor textEditor) {
    if (textEditor == null) {
      return null;
    }

    CompletionManager manager = editorMap.get(textEditor);
    if (manager != null) {
      return manager;
    }

    ITextViewer textViewer = textEditor.getAdapter(ITextViewer.class);
    if (!SwtUtils.isEditable(textViewer)) {
      return null;
    }

    manager = new CompletionManager(this.languageServer, this.completionProvider, textEditor, this.settingsManager);
    editorMap.put(textEditor, manager);

    return manager;
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionManager CompletionManager} for the given
   * ITextEditor. Returns <code>null</code> if there is no manager for the editor.
   */
  @Nullable
  public CompletionManager getCompletionManagerFor(IEditorPart editor) {
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
  public void disposeCompletionManagerFor(ITextEditor textEditor) {
    if (textEditor == null) {
      return;
    }
    CompletionManager handler = editorMap.remove(textEditor);
    if (handler != null) {
      handler.dispose();
    }
  }

  /**
   * Sets the active editor.
   */
  public void setActiveEditor(ITextEditor textEditor) {
    this.activeEditor.set(textEditor);

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

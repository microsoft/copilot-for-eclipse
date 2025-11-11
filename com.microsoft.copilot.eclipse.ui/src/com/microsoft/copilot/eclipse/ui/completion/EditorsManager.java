package com.microsoft.copilot.eclipse.ui.completion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.nes.NextEditSuggestionProvider;
import com.microsoft.copilot.eclipse.ui.nes.RenderManager;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Manages the completion managers and NES render managers for all available ITextEditors.
 */
public class EditorsManager {

  private CopilotLanguageServerConnection languageServer;
  private CompletionProvider completionProvider;
  private NextEditSuggestionProvider nesProvider;
  private Map<ITextEditor, BaseCompletionManager> editorMap;
  private Map<ITextEditor, RenderManager> nesRenderManagers;
  private AtomicReference<ITextEditor> activeEditor;
  private LanguageServerSettingManager settingsManager;

  /**
   * Creates a new EditorManager.
   */
  public EditorsManager(CopilotLanguageServerConnection languageServer, CompletionProvider completionProvider,
      NextEditSuggestionProvider nesProvider, LanguageServerSettingManager settingsManager) {
    this.languageServer = languageServer;
    this.completionProvider = completionProvider;
    this.nesProvider = nesProvider;
    this.editorMap = new ConcurrentHashMap<>();
    this.nesRenderManagers = new ConcurrentHashMap<>();
    this.activeEditor = new AtomicReference<>();
    this.settingsManager = settingsManager;
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager BaseCompletionManager} for the
   * given ITextEditor. If it does not exist, a new one will be created. Returns <code>null</code> if the editor is
   * <code>null</code>.
   */
  @Nullable
  public BaseCompletionManager getOrCreateCompletionManagerFor(ITextEditor textEditor) {
    if (textEditor == null) {
      return null;
    }

    BaseCompletionManager manager = editorMap.get(textEditor);
    if (manager != null) {
      return manager;
    }

    ITextViewer textViewer = textEditor.getAdapter(ITextViewer.class);
    if (!SwtUtils.isEditable(textViewer)) {
      return null;
    }

    manager = CompletionManagerFactory.createCompletionManager(this.languageServer, this.completionProvider, textEditor,
        this.settingsManager);
    editorMap.put(textEditor, manager);

    return manager;
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager BaseCompletionManager} for the
   * given ITextEditor. Returns <code>null</code> if there is no manager for the editor.
   */
  @Nullable
  public BaseCompletionManager getCompletionManagerFor(IEditorPart editor) {
    if (editor == null) {
      return null;
    }

    return editorMap.get(editor);
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager BaseCompletionManager} for the
   * active ITextEditor.
   */
  @Nullable
  public BaseCompletionManager getActiveCompletionManager() {
    if (this.activeEditor.get() == null) {
      return null;
    }
    return this.editorMap.get(activeEditor.get());
  }

  /**
   * Disposes the {@link com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager BaseCompletionManager} for
   * the given ITextEditor.
   */
  public void disposeCompletionManagerFor(ITextEditor textEditor) {
    if (textEditor == null) {
      return;
    }
    BaseCompletionManager handler = editorMap.remove(textEditor);
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
    for (BaseCompletionManager handler : this.editorMap.values()) {
      handler.dispose();
    }
    this.editorMap.clear();

    // Dispose all NES RenderManagers
    for (RenderManager renderMgr : this.nesRenderManagers.values()) {
      renderMgr.dispose();
    }
    this.nesRenderManagers.clear();
  }

  // ============ NES RenderManager Management ============

  /**
   * Gets or creates the RenderManager for the given editor.
   *
   * @param editor The text editor
   * @return The RenderManager instance, or null if creation fails or preview features are disabled
   */
  @Nullable
  public RenderManager getOrCreateNesRenderManager(ITextEditor editor) {
    if (editor == null) {
      return null;
    }
    
    // Only create NES RenderManager if client preview features are enabled
    if (!CopilotCore.getPlugin().getFeatureFlags().isClientPreviewFeatureEnabled()) {
      return null;
    }
    
    return nesRenderManagers.computeIfAbsent(editor,
        ed -> new RenderManager(this.languageServer, this.nesProvider, ed));
  }

  /**
   * Gets the RenderManager for the given editor.
   *
   * @param editor The text editor
   * @return The RenderManager instance, or null if not found
   */
  @Nullable
  public RenderManager getNesRenderManager(ITextEditor editor) {
    if (editor == null) {
      return null;
    }
    return nesRenderManagers.get(editor);
  }

  /**
   * Gets the RenderManager for the currently active editor.
   *
   * @return The RenderManager instance, or null if no active editor or no manager found
   */
  @Nullable
  public RenderManager getActiveNesRenderManager() {
    ITextEditor ed = this.activeEditor.get();
    if (ed == null) {
      return null;
    }
    return nesRenderManagers.get(ed);
  }

  /**
   * Disposes the RenderManager for the given editor.
   *
   * @param editor The text editor
   */
  public void disposeNesRenderManager(ITextEditor editor) {
    if (editor == null) {
      return;
    }
    RenderManager mgr = nesRenderManagers.remove(editor);
    if (mgr != null) {
      mgr.dispose();
    }
  }
}

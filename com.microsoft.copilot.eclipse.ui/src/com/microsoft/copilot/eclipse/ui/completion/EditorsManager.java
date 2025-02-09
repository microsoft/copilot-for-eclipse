package com.microsoft.copilot.eclipse.ui.completion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
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
  private Map<IDocument, Integer> documentActiveCount;
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
    this.documentActiveCount = new ConcurrentHashMap<>();
    this.activeEditor = new AtomicReference<>();
    this.settingsManager = settingsManager;
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

    CompletionManager manager = editorMap.get(editor);
    if (manager != null) {
      return manager;
    }

    manager = new CompletionManager(this.languageServer, this.completionProvider, editor, this.settingsManager);
    editorMap.put(editor, manager);

    // connect the document if it is the first time this document is opened.
    IDocument document = LSPEclipseUtils.getDocument(editor);
    if (document != null) {
      int count = documentActiveCount.getOrDefault(document, 0);
      if (count == 0) {
        try {
          this.languageServer.connectDocument(document);
        } catch (Exception e) {
          CopilotCore.LOGGER.error(e);
        }
      }
      documentActiveCount.put(document, count + 1);
    }
    return manager;
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
    IDocument document = LSPEclipseUtils.getDocument(editor);
    if (document != null) {
      int count = documentActiveCount.getOrDefault(document, 0);
      if (count == 1) {
        this.languageServer.disconnectDocument(LSPEclipseUtils.toUri(document));
        documentActiveCount.remove(document);
        return;
      }
      documentActiveCount.put(document, count - 1);
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

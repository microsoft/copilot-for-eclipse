package com.microsoft.copilot.eclipse.ui.completion;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Manages the completion managers for all available ITextEditors.
 */
public class EditorsManager implements ITextInputListener {

  private CopilotLanguageServerConnection languageServer;
  private CompletionProvider completionProvider;
  private Map<ITextEditor, CompletionManager> editorMap;
  private Map<IFile, Integer> fileActiveCount;
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
    this.fileActiveCount = new ConcurrentHashMap<>();
    this.activeEditor = new AtomicReference<>();
    this.settingsManager = settingsManager;
  }

  /**
   * Gets the {@link com.microsoft.copilot.eclipse.ui.completion.CompletionManager CompletionManager} for the given
   * ITextEditor. If it does not exist, a new one will be created. Returns <code>null</code> if the editor is
   * <code>null</code>.
   */
  @Nullable
  public CompletionManager getOrCreateCompletionManagerFor(IEditorPart editor) {
    if (editor == null) {
      return null;
    }

    ITextEditor textEditor = editor.getAdapter(ITextEditor.class);
    if (textEditor != null) {
      CompletionManager manager = editorMap.get(textEditor);
      if (manager != null) {
        return manager;
      }
    }

    IFile file = UiUtils.getFileFromEditorPart(editor);
    IDocument document = null;
    if (textEditor != null) {
      IDocument tmpDocument = LSPEclipseUtils.getDocument(textEditor);
      // the document synchronizer will not work if the uri of the document is null.
      if (LSPEclipseUtils.toUri(tmpDocument) != null) {
        document = tmpDocument;
      }
    }
    connectDocument(document, file);

    if (textEditor != null) {
      ITextViewer textViewer = textEditor.getAdapter(ITextViewer.class);
      if (!SwtUtils.isEditable(textViewer)) {
        return null;
      }

      CompletionManager manager = new CompletionManager(this.languageServer, this.completionProvider, textEditor,
          this.settingsManager);
      editorMap.put(textEditor, manager);

      SwtUtils.invokeOnDisplayThread(() -> {
        textViewer.addTextInputListener(this);
      }, textViewer.getTextWidget());
      return manager;
    }

    return null;
  }

  private void connectDocument(IDocument document, IFile file) {
    if (file != null) {
      int count = fileActiveCount.getOrDefault(file, 0);
      CompletableFuture<LanguageServerWrapper> wrapper = this.languageServer.connectDocument(document, file);
      // if the document is not connected, the wrapper will be null.
      if (wrapper != null) {
        fileActiveCount.put(file, count + 1);
      }
    }
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
  public void disposeCompletionManagerFor(IEditorPart editor) {
    disconnectDocument(UiUtils.getFileFromEditorPart(editor));

    ITextEditor textEditor = editor.getAdapter(ITextEditor.class);
    if (textEditor == null) {
      return;
    }
    CompletionManager handler = editorMap.remove(editor);
    if (handler != null) {
      handler.dispose();
      ITextViewer textViewer = textEditor.getAdapter(ITextViewer.class);
      if (textViewer != null) {
        SwtUtils.invokeOnDisplayThread(() -> {
          textViewer.removeTextInputListener(this);
        }, textViewer.getTextWidget());
      }
    }
  }

  private void disconnectDocument(IFile file) {
    if (file != null) {
      int count = fileActiveCount.getOrDefault(file, 0);
      if (count == 1) {
        try {
          this.languageServer.disconnectDocument(new URI(FileUtils.getResourceUri(file)));
        } catch (URISyntaxException e) {
          CopilotCore.LOGGER.error(e);
        }
        fileActiveCount.remove(file);
        return;
      }
      fileActiveCount.put(file, count - 1);
    }
  }

  @Override
  public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    // do nothing.
  }

  @Override
  public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
    IFile newFile = LSPEclipseUtils.getFile(newInput);
    String newUri = FileUtils.getResourceUri(newFile);
    IFile oldFile = LSPEclipseUtils.getFile(oldInput);
    String oldUri = FileUtils.getResourceUri(oldFile);
    if (Objects.equals(newUri, oldUri)) {
      return;
    }
    connectDocument(newInput, newFile);
    disconnectDocument(oldFile);
  }

  /**
   * Sets the active editor.
   */
  public void setActiveEditor(IEditorPart editorPart) {
    if (editorPart == null) {
      this.activeEditor.set(null);
      return;
    }
    ITextEditor textEditor = editorPart.getAdapter(ITextEditor.class);
    if (textEditor != null) {
      this.activeEditor.set(textEditor);
    }

  }

  @Nullable
  public IEditorPart getActiveEditor() {
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

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Listen to the lifecycle event of an editor parts.
 */
public class EditorLifecycleListener implements IPartListener2 {

  /**
   * The map of opened editors to its contained file uri. This is to avoid same editor being treated multiple times.
   * Because each time user double clicks the file, partActivated will be called. This is also used when
   * partInputChanged is called, the old uri need to be disconnected.
   */
  private Map<IEditorPart, URI> editorToUriMap;

  /**
   * A map to count how many editors are opened for each file. Because the same file can be opened in multiple editors,
   * we only need to disconnect the file when all editors are closed.
   */
  private Map<URI, Integer> uriActiveCount;

  private EditorsManager manager;

  private CopilotLanguageServerConnection languageServer;

  /**
   * Creates a new EditorLifecycleListener.
   */
  public EditorLifecycleListener(CopilotLanguageServerConnection languageServer, EditorsManager manager) {
    this.languageServer = languageServer;
    this.manager = manager;
    editorToUriMap = new ConcurrentHashMap<>();
    uriActiveCount = new ConcurrentHashMap<>();
  }

  @Override
  public void partActivated(IWorkbenchPartReference partRef) {
    IEditorPart editorPart = getEditorPart(partRef);
    if (editorPart == null) {
      return;
    }

    partActivated(editorPart);
  }

  /**
   * Used to prepare the active editor part when the IDE is opened.
   */
  public void partActivated(IEditorPart editorPart) {
    ITextEditor textEditor = editorPart.getAdapter(ITextEditor.class);
    if (connectDocumentIfNecessary(editorPart, textEditor)) {
      createCompletionHandlerFor(textEditor);
    }
    if (textEditor != null) {
      manager.setActiveEditor(textEditor);
      manager.getOrCreateNesRenderManager(textEditor);
    }
  }

  @Override
  public void partBroughtToTop(IWorkbenchPartReference partRef) {
    partActivated(partRef);
  }

  @Override
  public void partDeactivated(IWorkbenchPartReference partRef) {
    // Clear NES suggestion when switching away from editor
    IEditorPart editorPart = getEditorPart(partRef);
    if (editorPart == null) {
      return;
    }
    ITextEditor textEditor = editorPart.getAdapter(ITextEditor.class);
    if (textEditor != null) {
      var nesManager = manager.getNesRenderManager(textEditor);
      if (nesManager != null) {
        nesManager.clearSuggestion();
      }
    }
  }

  @Override
  public void partInputChanged(IWorkbenchPartReference partRef) {
    // try to re-create the completion handler for the part to fix the completion manager is not created for the ABAP
    // editor in the beginning
    IEditorPart editorPart = getEditorPart(partRef);
    if (editorPart == null) {
      return;
    }
    ITextEditor editor = editorPart.getAdapter(ITextEditor.class);
    if (editor != null) {
      manager.disposeNesRenderManager(editor);
    }
    disconnectDocumentIfNecessary(editorPart);

    this.partActivated(partRef);
  }

  @Override
  public void partClosed(IWorkbenchPartReference partRef) {
    IEditorPart editorPart = getEditorPart(partRef);
    if (editorPart == null) {
      return;
    }

    disconnectDocumentIfNecessary(editorPart);

    ITextEditor textEditor = editorPart.getAdapter(ITextEditor.class);
    disposeCompletionHandlerFor(textEditor);
    manager.disposeNesRenderManager(textEditor);
  }

  @Nullable
  private IEditorPart getEditorPart(IWorkbenchPartReference partRef) {
    IWorkbenchPart part = partRef.getPart(false);
    if (part == null) {
      return null;
    }
    return part.getAdapter(IEditorPart.class);
  }

  private boolean connectDocumentIfNecessary(IEditorPart editorPart, ITextEditor textEditor) {
    if (editorToUriMap.containsKey(editorPart)) {
      return false; // already connected
    }

    IFile file = UiUtils.getFileFromEditorPart(editorPart);
    if (file == null) {
      return false; // not a file editor, do not connect
    }
    URI uri = LSPEclipseUtils.toUri((IResource) file);
    if (uri == null) {
      return false; // cannot get valid URI, do not connect
    }
    IDocument document = LSPEclipseUtils.getDocument(textEditor);
    boolean connected = connectDocument(document, file, uri);
    if (connected) {
      editorToUriMap.put(editorPart, uri);
    }
    return connected;
  }

  /**
   * Return <code>true</code> if the document is connected successfully, otherwise <code>false</code>.
   */
  private boolean connectDocument(IDocument document, IFile file, URI uri) {
    if (file != null) {
      int count = uriActiveCount.getOrDefault(uri, 0);
      CompletableFuture<LanguageServerWrapper> wrapper = this.languageServer.connectDocument(document, file);
      // if the document is not connected, the wrapper will be null.
      if (wrapper != null) {
        uriActiveCount.put(uri, count + 1);
        return true;
      }
    }
    return false;
  }

  private void disconnectDocumentIfNecessary(IEditorPart editorPart) {
    URI uri = editorToUriMap.remove(editorPart);
    disconnectDocument(uri);
  }

  private void disconnectDocument(URI uri) {
    if (uri != null) {
      int count = uriActiveCount.getOrDefault(uri, 0);
      if (count == 1) {
        this.languageServer.disconnectDocument(uri);
        uriActiveCount.remove(uri);
      } else if (count > 1) {
        uriActiveCount.put(uri, count - 1);
      }
    }
  }

  /**
   * Creates the {@link BaseCompletionManager} for the ITextEditor of the IWorkbenchPart.
   */
  public void createCompletionHandlerFor(ITextEditor textEditor) {
    if (textEditor != null) {
      manager.getOrCreateCompletionManagerFor(textEditor);
      manager.setActiveEditor(textEditor);
    }
  }

  void disposeCompletionHandlerFor(ITextEditor textEditor) {
    if (textEditor != null) {
      manager.disposeCompletionManagerFor(textEditor);
      if (textEditor.equals(manager.getActiveEditor())) {
        manager.setActiveEditor(null);
      }
    }
  }

}
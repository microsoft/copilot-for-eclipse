package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.service.IReferencedFileService;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.ui.chat.ActionBar;
import com.microsoft.copilot.eclipse.ui.chat.CurrentReferencedFile;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Service to manage the referenced file in the chat view.
 */
public class ReferencedFileService extends ChatBaseService implements IReferencedFileService {

  private IObservableValue<IFile> currentFileObservable;
  private IObservableValue<Boolean> isCurrentFileVisibleObservable;
  private IObservableValue<Range> currentSelectionObservable;

  // The reason that we use map to dedup the context file is that the hashCode() method
  // of the IFile/IFolder checks the full path, which will fail to dedup when it comes to multi-module
  // project, so we use the URI instead.
  private IObservableValue<Map<String, IResource>> referencedFilesObservable;

  private ISideEffect currentReferencedFileSideEffect;
  private ISideEffect isCurrentFileVisibleSideEffect;
  private ISideEffect referencedFilesSideEffect;
  private ISideEffect currentSelectionSideEffect;

  private IPartListener2 listener;
  private ISelectionChangedListener selectionListener;
  private ITextEditor currentTrackedEditor;

  /**
   * Creates a new ReferencedFileService.
   */
  public ReferencedFileService() {
    super(null, null);
    ensureRealm(() -> {
      currentFileObservable = new WritableValue<>(null, IFile.class);
      isCurrentFileVisibleObservable = new WritableValue<>(true, Boolean.class);
      currentSelectionObservable = new WritableValue<>(null, Range.class);
      referencedFilesObservable = new WritableValue<>(new LinkedHashMap<>(), Map.class);
    });
    this.selectionListener = new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        // Use currentTrackedEditor instead of getActiveEditor() to ensure we use the correct
        // document for this selection event (the active editor may have already changed)
        if (currentTrackedEditor != null) {
          IDocument document = LSPEclipseUtils.getDocument(currentTrackedEditor);
          updateCurrentSelection(event.getSelection(), document);
        } else {
          ensureRealm(() -> currentSelectionObservable.setValue(null));
        }
      }
    };
    this.listener = new IPartListener2() {
      @Override
      public void partActivated(IWorkbenchPartReference partRef) {
        updateCurrentReferencedFile(partRef);
        trackSelectionInEditor(partRef);
      }

      @Override
      public void partBroughtToTop(IWorkbenchPartReference partRef) {
        updateCurrentReferencedFile(partRef);
        trackSelectionInEditor(partRef);
      }

      @Override
      public void partClosed(IWorkbenchPartReference partRef) {
        IWorkbenchPage page = UiUtils.getActivePage();
        if (page == null || page.getEditorReferences().length == 0) {
          ensureRealm(() -> {
            currentFileObservable.setValue(null);
            currentSelectionObservable.setValue(null);
          });
        }
        untrackSelectionInEditor(partRef);
      }
    };
    registerPartListener();
  }

  @Override
  public IFile getCurrentFile() {
    final AtomicReference<IFile> result = new AtomicReference<>();
    ensureRealm(() -> {
      result.set(
          Boolean.TRUE.equals(isCurrentFileVisibleObservable.getValue()) ? currentFileObservable.getValue() : null);
    });
    return result.get();
  }

  @Override
  public List<IResource> getReferencedFiles() {
    final AtomicReference<List<IResource>> result = new AtomicReference<>();
    ensureRealm(() -> {
      Map<String, IResource> files = referencedFilesObservable.getValue();
      result.set(List.copyOf(files.values()));
    });
    return result.get();
  }

  @Override
  @Nullable
  public Range getCurrentSelection() {
    // Use the tracked observable value if available to ensure UI consistency
    if (currentSelectionObservable != null) {
      AtomicReference<Range> result = new AtomicReference<>();
      ensureRealm(() -> result.set(currentSelectionObservable.getValue()));
      Range observableValue = result.get();
      if (observableValue != null) {
        return observableValue;
      }
    }

    // Fallback: query editor directly if observable is null or not tracking
    IEditorPart activeEditor = UiUtils.getActiveEditor();
    if (activeEditor == null) {
      return null;
    }

    ITextEditor textEditor = activeEditor.getAdapter(ITextEditor.class);
    if (textEditor == null) {
      return null;
    }

    try {
      ITextSelection selection = (ITextSelection) textEditor.getSelectionProvider().getSelection();
      if (selection == null || selection.isEmpty()) {
        return null;
      }

      IDocument document = LSPEclipseUtils.getDocument(textEditor);
      if (document == null) {
        return null;
      }

      return convertTextSelectionToRange(selection, document);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to get current selection", e);
      return null;
    }
  }

  /**
   * Converts Eclipse text selection to LSP Range.
   *
   * @param selection the text selection
   * @param document the document
   * @return the LSP Range
   * @throws BadLocationException if offset is invalid
   */
  private Range convertTextSelectionToRange(ITextSelection selection, IDocument document)
      throws BadLocationException {
    int startLine = selection.getStartLine();
    int endLine = selection.getEndLine();
    int startOffset = selection.getOffset();
    int endOffset = startOffset + selection.getLength();

    // Get character positions within the lines
    int startLineOffset = document.getLineOffset(startLine);
    int endLineOffset = document.getLineOffset(endLine);
    int startCharacter = startOffset - startLineOffset;
    int endCharacter = endOffset - endLineOffset;

    Position start = new Position(startLine, startCharacter);
    Position end = new Position(endLine, endCharacter);
    return new Range(start, end);
  }

  /**
   * Binds the current file widget to the current file observable.
   */
  public void bindCurrentFileWidget(CurrentReferencedFile widget) {
    unbindCurrentFileWidget();

    ensureRealm(() -> {
      currentReferencedFileSideEffect = ISideEffect.create(currentFileObservable::getValue, (IFile file) -> {
        if (widget.isDisposed()) {
          return;
        }
        widget.setFile(file);
        widget.updateCloseClickBtnIcon(isCurrentFileVisibleObservable.getValue());
      });
      isCurrentFileVisibleSideEffect = ISideEffect.create(isCurrentFileVisibleObservable::getValue,
          (Boolean isVisible) -> {
            if (widget.isDisposed()) {
              return;
            }
            widget.updateCloseClickBtnIcon(isVisible);
          });
      currentSelectionSideEffect = ISideEffect.create(currentSelectionObservable::getValue, (Range selection) -> {
        if (widget.isDisposed()) {
          return;
        }
        widget.setSelection(selection);
      });

      widget.addDisposeListener(e -> unbindCurrentFileWidget());
    });

    updateCurrentReferencedFile(UiUtils.getActiveEditor());
    trackSelectionInCurrentEditor();
  }

  /**
   * Toggle the visibility of the current file.
   */
  public void toggleIsVisible() {
    ensureRealm(() -> {
      boolean isVisible = isCurrentFileVisibleObservable.getValue();
      isCurrentFileVisibleObservable.setValue(!isVisible);
    });
  }

  /**
   * Binds the action bar with the referenced files observable.
   */
  public void bindReferencedFilesWidget(ActionBar actionBar) {
    ensureRealm(() -> {
      if (referencedFilesSideEffect != null) {
        referencedFilesSideEffect.dispose();
        referencedFilesSideEffect = null;
      }

      referencedFilesSideEffect = ISideEffect.create(referencedFilesObservable::getValue,
          (Map<String, IResource> files) -> {
            if (actionBar.isDisposed()) {
              return;
            }
            actionBar.updateReferencedWidgetsWithFiles(List.copyOf(files.values()));
          });
    });
  }

  /**
   * Clear the current selection.
   */
  public void clearCurrentSelection() {
    ensureRealm(() -> currentSelectionObservable.setValue(null));
  }

  private void trackSelectionInCurrentEditor() {
    IEditorPart activeEditor = UiUtils.getActiveEditor();
    if (activeEditor != null) {
      trackSelectionInEditor(activeEditor);
    }
  }

  private void trackSelectionInEditor(IWorkbenchPartReference partRef) {
    IWorkbenchPart part = partRef.getPart(false);
    if (part instanceof IEditorPart editorPart) {
      trackSelectionInEditor(editorPart);
    }
  }

  private void trackSelectionInEditor(IEditorPart editorPart) {
    if (editorPart == null) {
      return;
    }

    ITextEditor textEditor = editorPart.getAdapter(ITextEditor.class);
    if (textEditor == null) {
      return;
    }

    // Untrack old editor if different
    if (currentTrackedEditor != null && currentTrackedEditor != textEditor) {
      untrackSelectionListener(currentTrackedEditor);
    }

    if (currentTrackedEditor == textEditor) {
      // Already tracking this editor, just update selection
      updateSelectionFromEditor(textEditor);
      return;
    }

    currentTrackedEditor = textEditor;
    ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
    if (selectionProvider instanceof IPostSelectionProvider postSelectionProvider) {
      postSelectionProvider.addPostSelectionChangedListener(selectionListener);
    } else if (selectionProvider != null) {
      selectionProvider.addSelectionChangedListener(selectionListener);
    }

    // Initialize with current selection
    updateSelectionFromEditor(textEditor);
  }

  private void untrackSelectionInEditor(IWorkbenchPartReference partRef) {
    IWorkbenchPart part = partRef.getPart(false);
    if (part instanceof IEditorPart editorPart) {
      ITextEditor textEditor = editorPart.getAdapter(ITextEditor.class);
      if (textEditor != null && textEditor == currentTrackedEditor) {
        untrackSelectionListener(textEditor);
        currentTrackedEditor = null;
        ensureRealm(() -> currentSelectionObservable.setValue(null));
      }
    }
  }

  private void untrackSelectionListener(ITextEditor textEditor) {
    if (textEditor == null) {
      return;
    }
    ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
    if (selectionProvider instanceof IPostSelectionProvider postSelectionProvider) {
      postSelectionProvider.removePostSelectionChangedListener(selectionListener);
    } else if (selectionProvider != null) {
      selectionProvider.removeSelectionChangedListener(selectionListener);
    }
  }

  private void updateSelectionFromEditor(ITextEditor textEditor) {
    if (textEditor == null || textEditor.getSelectionProvider() == null) {
      ensureRealm(() -> currentSelectionObservable.setValue(null));
      return;
    }
    IDocument document = LSPEclipseUtils.getDocument(textEditor);
    updateCurrentSelection(textEditor.getSelectionProvider().getSelection(), document);
  }

  private void updateCurrentSelection(ISelection selection, IDocument document) {
    if (!(selection instanceof ITextSelection textSelection) || textSelection.isEmpty()) {
      ensureRealm(() -> currentSelectionObservable.setValue(null));
      return;
    }

    if (document == null) {
      ensureRealm(() -> currentSelectionObservable.setValue(null));
      return;
    }

    try {
      Range range = convertTextSelectionToRange(textSelection, document);
      ensureRealm(() -> currentSelectionObservable.setValue(range));
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to update current selection", e);
      ensureRealm(() -> currentSelectionObservable.setValue(null));
    }
  }

  /**
   * Update the referenced files observable with a new set of files.
   */
  public void updateReferencedFiles(List<IResource> files) {
    ensureRealm(() -> {
      Map<String, IResource> fileMap = new LinkedHashMap<>();
      addFilesToMap(files, fileMap);
      referencedFilesObservable.setValue(fileMap);
    });
  }

  /**
   * Add files to the existing referenced files observable.
   */
  public void addReferencedFiles(List<IResource> files) {
    ensureRealm(() -> {
      Map<String, IResource> fileMap = new LinkedHashMap<>(referencedFilesObservable.getValue());
      addFilesToMap(files, fileMap);
      referencedFilesObservable.setValue(fileMap);
    });
  }

  /**
   * Helper method to add valid files to the map.
   */
  private void addFilesToMap(List<IResource> files, Map<String, IResource> fileMap) {
    for (IResource file : files) {
      if (file instanceof IFile) {
        if (file != null && !FileUtils.isExcludedFromReferencedFiles((IFile) file)) {
          String uri = FileUtils.getResourceUri(file);
          if (uri != null) {
            fileMap.put(uri, file);
          }
        }
      } else if (file instanceof IFolder) {
        String uri = FileUtils.getResourceUri(file);
        if (uri != null) {
          fileMap.put(uri, file);
        }
      }
    }
  }

  /**
   * Remove a specific resource from the referenced files observable.
   */
  public void removeReferencedFile(IResource targetResource) {
    ensureRealm(() -> {
      if (targetResource == null) {
        return;
      }

      Map<String, IResource> fileMap = new LinkedHashMap<>(referencedFilesObservable.getValue());
      String uri = FileUtils.getResourceUri(targetResource);
      boolean hasChanges = fileMap.remove(uri) != null
          || fileMap.values().removeIf(resource -> resource == targetResource);
      if (hasChanges) {
        referencedFilesObservable.setValue(fileMap);
      }
    });
  }

  /**
   * Clean up all non-existent resources from the referenced files list.
   */
  public void cleanupNonExistentFiles() {
    ensureRealm(() -> {
      Map<String, IResource> fileMap = new LinkedHashMap<>(referencedFilesObservable.getValue());
      boolean hasChanges = fileMap.values().removeIf(resource -> resource == null || !resource.exists());
      if (hasChanges) {
        referencedFilesObservable.setValue(fileMap);
      }
    });
  }

  private void unbindCurrentFileWidget() {
    ensureRealm(() -> {
      if (currentReferencedFileSideEffect != null) {
        currentReferencedFileSideEffect.dispose();
        currentReferencedFileSideEffect = null;
      }

      if (isCurrentFileVisibleSideEffect != null) {
        isCurrentFileVisibleSideEffect.dispose();
        isCurrentFileVisibleSideEffect = null;
      }

      if (currentSelectionSideEffect != null) {
        currentSelectionSideEffect.dispose();
        currentSelectionSideEffect = null;
      }
    });
  }

  private void updateCurrentReferencedFile(IWorkbenchPartReference partRef) {
    IWorkbenchPart part = partRef.getPart(false);
    if (part instanceof IEditorPart editorPart) {
      updateCurrentReferencedFile(editorPart);
    }
  }

  private void updateCurrentReferencedFile(IEditorPart editorPart) {
    if (editorPart == null) {
      updateObservable(currentFileObservable, null);
      return;
    }

    ITextEditor textEditor = editorPart.getAdapter(ITextEditor.class);
    if (textEditor == null) {
      updateObservable(currentFileObservable, null);
      return;
    }

    // We do following checks to ensure that un-connected document will not
    // be added to the current file. See: https://github.com/microsoft/copilot-eclipse/issues/884
    // TODO: Support other types of editors.
    IDocument document = LSPEclipseUtils.getDocument(textEditor);
    if (document == null || LSPEclipseUtils.toUri(document) == null) {
      updateObservable(currentFileObservable, null);
      return;
    }

    IFile currentFile = UiUtils.getCurrentFile();
    if (FileUtils.isExcludedFromCurrentFile(currentFile)) {
      currentFile = null;
    }

    updateObservable(currentFileObservable, currentFile);
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    unregisterPartListener();
  }

  private void registerPartListener() {
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      window.getPartService().addPartListener(this.listener);
    }
  }

  private void unregisterPartListener() {
    IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow window : windows) {
      window.getPartService().removePartListener(this.listener);
    }
  }

}
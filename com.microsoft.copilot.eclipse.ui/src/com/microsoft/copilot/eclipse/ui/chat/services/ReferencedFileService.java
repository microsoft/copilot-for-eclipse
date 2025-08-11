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
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.Constants;
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

  // The reason that we use map to dedup the context file is that the hashCode() method
  // of the IFile/IFolder checks the full path, which will fail to dedup when it comes to multi-module
  // project, so we use the URI instead.
  private IObservableValue<Map<String, IResource>> referencedFilesObservable;

  private ISideEffect currentReferencedFileSideEffect;
  private ISideEffect isCurrentFileVisibleSideEffect;
  private ISideEffect referencedFilesSideEffect;

  private IPartListener2 listener;

  /**
   * Creates a new ReferencedFileService.
   */
  public ReferencedFileService() {
    super(null, null);
    ensureRealm(() -> {
      currentFileObservable = new WritableValue<>(null, IFile.class);
      isCurrentFileVisibleObservable = new WritableValue<>(true, Boolean.class);
      referencedFilesObservable = new WritableValue<>(new LinkedHashMap<>(), Map.class);
    });
    this.listener = new IPartListener2() {
      @Override
      public void partActivated(IWorkbenchPartReference partRef) {
        updateCurrentReferencedFile(partRef);
      }

      @Override
      public void partBroughtToTop(IWorkbenchPartReference partRef) {
        updateCurrentReferencedFile(partRef);
      }

      @Override
      public void partClosed(IWorkbenchPartReference partRef) {
        IWorkbenchPage page = UiUtils.getActivePage();
        if (page == null || page.getEditorReferences().length == 0) {
          ensureRealm(() -> currentFileObservable.setValue(null));
        }
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

      widget.addDisposeListener(e -> unbindCurrentFileWidget());
    });

    updateCurrentReferencedFile(UiUtils.getActiveEditor());
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
   * Remove the uri from the referenced files observable.
   */
  public void removeReferencedFile(String uri) {
    ensureRealm(() -> {
      Map<String, IResource> fileMap = referencedFilesObservable.getValue();
      IResource removedFile = fileMap.remove(uri);
      if (removedFile == null) {
        return;
      }
      referencedFilesObservable.setValue(new LinkedHashMap<>(fileMap));
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
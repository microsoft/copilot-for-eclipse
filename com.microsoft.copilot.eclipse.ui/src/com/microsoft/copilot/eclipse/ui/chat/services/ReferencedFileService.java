package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

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
  // of the IFile checks the full path, which will fail to dedup when it comes to multi-module
  // project, so we use the URI instead.
  private IObservableValue<Map<String, IFile>> referencedFilesObservable;

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
  public List<IFile> getReferencedFiles() {
    final AtomicReference<List<IFile>> result = new AtomicReference<>();
    ensureRealm(() -> {
      Map<String, IFile> files = referencedFilesObservable.getValue();
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

    updateCurrentReferencedFile(UiUtils.getCurrentFile());
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
          (Map<String, IFile> files) -> {
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
  public void updateReferencedFiles(List<IFile> files) {
    ensureRealm(() -> {
      Map<String, IFile> fileMap = new LinkedHashMap<>();
      addFilesToMap(files, fileMap);
      referencedFilesObservable.setValue(fileMap);
    });
  }

  /**
   * Add files to the existing referenced files observable.
   */
  public void addReferencedFiles(List<IFile> files) {
    ensureRealm(() -> {
      Map<String, IFile> fileMap = new LinkedHashMap<>(referencedFilesObservable.getValue());
      addFilesToMap(files, fileMap);
      referencedFilesObservable.setValue(fileMap);
    });
  }

  /**
   * Helper method to add valid files to the map.
   */
  private void addFilesToMap(List<IFile> files, Map<String, IFile> fileMap) {
    for (IFile file : files) {
      if (file != null && !isExcludedFromReferencedFiles(file)) {
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
      Map<String, IFile> fileMap = referencedFilesObservable.getValue();
      IFile removedFile = fileMap.remove(uri);
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
    if (!(part instanceof IEditorPart)) {
      return;
    }
    updateCurrentReferencedFile(UiUtils.getCurrentFile());
  }

  private void updateCurrentReferencedFile(IFile currentFile) {
    if (isExcludedFromCurrentFile(currentFile)) {
      currentFile = null;
    }
    final IFile finalCurrentFile = currentFile;
    ensureRealm(() -> currentFileObservable.setValue(finalCurrentFile));
  }

  /**
   * Returns true if the file needs to be excluded from 'Current file' reference in chat.
   */
  private boolean isExcludedFromCurrentFile(@Nullable IFile file) {
    if (file == null) {
      return true;
    }

    if (file.getFileExtension() == null) {
      return false; // If the file has no extension, we do not exclude it.
    }
    return Constants.EXCLUDED_CURRENT_FILE_TYPE.contains(file.getFileExtension());
  }

  /**
   * Returns true if the file needs to be excluded from the referenced files.
   */
  private boolean isExcludedFromReferencedFiles(@Nullable IFile file) {
    if (file == null) {
      return true;
    }

    if (file.getFileExtension() == null) {
      return false; // If the file has no extension, we do not exclude it.
    }
    return Constants.EXCLUDED_REFERENCE_FILE_TYPE.contains(file.getFileExtension());
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

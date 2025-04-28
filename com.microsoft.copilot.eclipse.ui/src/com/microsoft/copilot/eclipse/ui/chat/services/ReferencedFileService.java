package com.microsoft.copilot.eclipse.ui.chat.services;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.chat.service.IReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.CurrentReferencedFile;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Service to manage the referenced file in the chat view.
 */
public class ReferencedFileService extends ChatBaseService implements IReferencedFileService {

  private IObservableValue<IFile> currentFileObservable;
  private IObservableValue<Boolean> isCurrentFileVisibleObservable;

  private ISideEffect currentReferencedFileSideEffect;
  private ISideEffect isCurrentFileVisibleSideEffect;

  private IPartListener2 listener;

  /**
   * Creates a new ReferencedFileService.
   */
  public ReferencedFileService() {
    super(null, null);
    ensureRealm(() -> {
      currentFileObservable = new WritableValue<>(null, IFile.class);
      isCurrentFileVisibleObservable = new WritableValue<>(true, Boolean.class);
    });
    this.listener = new IPartListener2() {
      @Override
      public void partActivated(IWorkbenchPartReference partRef) {
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
    final IFile[] result = new IFile[1];
    ensureRealm(() -> {
      if (Boolean.TRUE.equals(isCurrentFileVisibleObservable.getValue())) {
        result[0] = currentFileObservable.getValue();
      } else {
        result[0] = null;
      }
    });
    return result[0];
  }

  /**
   * Binds the current file widget to the current file observable.
   */
  public void bindCurrentFileWidget(CurrentReferencedFile widget) {
    unbindCurrentFileWidget();

    widget.setCloseClickAction(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        toggleIsVisible();
      }
    });

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
    if (shouldExcluded(currentFile)) {
      currentFile = null;
    }
    final IFile finalCurrentFile = currentFile;
    ensureRealm(() -> currentFileObservable.setValue(finalCurrentFile));
  }

  /**
   * Returns true if the file needs to be excluded from 'Current file' reference in chat.
   */
  private boolean shouldExcluded(@Nullable IFile file) {
    if (file == null || file.getFileExtension() == null) {
      return true;
    }
    return Constants.EXCLUDED_FILE_TYPE.contains(file.getFileExtension());
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

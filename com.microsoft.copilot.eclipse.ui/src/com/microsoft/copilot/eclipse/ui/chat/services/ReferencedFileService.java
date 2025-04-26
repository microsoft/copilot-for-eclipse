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
        updateCurrentReferencedFile(partRef);
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

    updateCurrentReferencedFile(null);
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
    if (currentReferencedFileSideEffect != null) {
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
  }

  private void updateCurrentReferencedFile(IWorkbenchPartReference partRef) {
    if (partRef != null && !(partRef.getPart(false) instanceof IEditorPart)) {
      return;
    }
    IFile currentFile = UiUtils.getCurrentFile();
    if (needExcluded(currentFile)) {
      return;
    }
    ensureRealm(() -> currentFileObservable.setValue(currentFile));
  }

  /**
   * Returns true if the file needs to be excluded from 'Current file' reference in chat.
   */
  private boolean needExcluded(@Nullable IFile file) {
    if (file == null) {
      return true;
    }
    String fileExtension = file.getFileExtension();
    if (fileExtension == null) {
      return true;
    }
    return Constants.EXCLUDED_FILE_TYPE.contains(fileExtension);
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

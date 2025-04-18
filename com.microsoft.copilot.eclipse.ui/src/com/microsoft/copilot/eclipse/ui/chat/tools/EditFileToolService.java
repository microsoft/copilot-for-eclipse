package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.resources.IFile;

import com.microsoft.copilot.eclipse.ui.chat.FileChangeSummaryBar;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatBaseService;

/**
 * Service for the Edit File tool. This service manages the state of the Edit File tool, including the files to be
 * edited and the enable state of the button.
 */
public class EditFileToolService extends ChatBaseService {
  private IObservableValue<Map<IFile, Boolean>> filesObservable;
  private IObservableValue<Boolean> buttonEnableObservable;
  private ISideEffect titleBarSideEffect;
  private ISideEffect buttonStatusSideEffect;

  /**
   * Constructor for EditFileToolService.
   */
  public EditFileToolService() {
    super(null, null);
    ensureRealm(() -> {
      filesObservable = new WritableValue<>(new LinkedHashMap<>(), Map.class);
      buttonEnableObservable = new WritableValue<>(false, Boolean.class);
    });
  }

  /**
   * Bind the FileChangeSummaryBar to the changed files.
   */
  public void bindFileChangeSummaryBar(FileChangeSummaryBar filesChangeSummaryBar) {
    if (filesChangeSummaryBar == null) {
      return;
    }

    ensureRealm(() -> {
      titleBarSideEffect = ISideEffect.create(() -> filesObservable.getValue(),
          (Map<IFile, Boolean> filesMap) -> filesChangeSummaryBar.buildSummaryBarFor(filesMap,
              buttonEnableObservable.getValue()));
      buttonStatusSideEffect = ISideEffect.create(() -> buttonEnableObservable.getValue(),
          (Boolean status) -> filesChangeSummaryBar.setButtonStatus(status));
    });
  }

  /**
   * Enable or disable the buttons for the file change summary bar.
   */
  public void setFileChangeSummaryBarButtonStatus(boolean status) {
    ensureRealm(() -> {
      buttonEnableObservable.setValue(status);
    });
  }

  /**
   * Set the changed files for the file change summary bar.
   */
  public void setChangedFiles(Map<IFile, Boolean> files) {
    ensureRealm(() -> {
      filesObservable.setValue(files);
    });
  }

  /**
   * Get the changed files for the file change summary bar.
   */
  public Map<IFile, Boolean> getChangedFiles() {
    return filesObservable.getValue();
  }

  /**
   * Add a changed file to the file change summary bar.
   */
  public void addChangedFile(IFile file) {
    ensureRealm(() -> {
      Map<IFile, Boolean> filesMap = new LinkedHashMap<>(filesObservable.getValue());
      filesMap.put(file, false);
      filesObservable.setValue(filesMap);
      buttonEnableObservable.setValue(false);
    });
  }

  /**
   * Complete a changed file from the file change summary bar.
   */
  public void completeFile(IFile file) {
    ensureRealm(() -> {
      Map<IFile, Boolean> filesMap = new LinkedHashMap<>(filesObservable.getValue());
      filesMap.put(file, true);
      filesObservable.setValue(filesMap);
    });
  }

  /**
   * Remove a changed file from the file change summary bar.
   */
  public void removeFile(IFile file) {
    ensureRealm(() -> {
      Map<IFile, Boolean> filesMap = new LinkedHashMap<>(filesObservable.getValue());
      filesMap.remove(file);
      filesObservable.setValue(filesMap);
    });
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    if (titleBarSideEffect != null) {
      titleBarSideEffect.dispose();
      titleBarSideEffect = null;
    }
    if (buttonStatusSideEffect != null) {
      buttonStatusSideEffect.dispose();
      buttonStatusSideEffect = null;
    }
  }
}

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.ConversationUtils;
import com.microsoft.copilot.eclipse.ui.chat.FileChangeSummaryBar;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatBaseService;

/**
 * Service for the Edit File tool. This service manages the state of the Create File Tool and Edit File tool, including
 * the files to be created or edited and the enable state of the button.
 */
public class FileToolService extends ChatBaseService {
  private IObservableValue<Map<IFile, FileChangeProperty>> filesObservable;
  private IObservableValue<Boolean> buttonEnableObservable;

  private FileChangeSummaryBar fileChangeSummaryBar;
  private CreateFileTool createFileTool;
  private EditFileTool editFileTool;

  private ISideEffect filesSideEffect;
  private ISideEffect buttonEnableSideEffect;

  /**
   * Constructor for FileToolService.
   */
  public FileToolService(CopilotLanguageServerConnection lsConnection) {
    super(lsConnection, null);

    ensureRealm(() -> {
      filesObservable = new WritableValue<>(new LinkedHashMap<>(), Map.class);
      buttonEnableObservable = new WritableValue<>(false, Boolean.class);
    });

    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_NEW_CONVERSATION, event -> {
      onResolveAllChanges();
    });
  }

  /**
   * Bind the FileChangeSummaryBar to the changed files.
   */
  public void bindFileChangeSummaryBar(ChatView chatView) {
    if (this.createFileTool == null) {
      this.createFileTool = (CreateFileTool) CopilotUi.getPlugin().getChatServiceManager().getAgentToolService()
          .getTool(CreateFileTool.TOOL_NAME);
    }
    if (this.editFileTool == null) {
      this.editFileTool = (EditFileTool) CopilotUi.getPlugin().getChatServiceManager().getAgentToolService()
          .getTool(EditFileTool.TOOL_NAME);
    }

    ensureRealm(() -> {
      unbindFileChangeSummaryBar();
      filesSideEffect = ISideEffect.create(() -> filesObservable.getValue(),
          (Map<IFile, FileChangeProperty> filesMap) -> {
            if (filesMap.isEmpty()) {
              disposeFileChangeSummaryBar();
            } else {
              if (this.fileChangeSummaryBar == null || this.fileChangeSummaryBar.isDisposed()) {
                this.fileChangeSummaryBar = new FileChangeSummaryBar(chatView.getActionBar(), SWT.NONE);
                if (chatView.getActionBar().getChildren().length > 0) {
                  this.fileChangeSummaryBar.moveAbove(chatView.getActionBar().getChildren()[0]);
                }
              }
              this.fileChangeSummaryBar.buildSummaryBarFor(filesMap);
              this.fileChangeSummaryBar.moveAbove(chatView.getActionBar());
            }
          });
      buttonEnableSideEffect = ISideEffect.create(() -> buttonEnableObservable.getValue(), (Boolean status) -> {
        if (this.fileChangeSummaryBar == null || this.fileChangeSummaryBar.isDisposed()) {
          return;
        }
        this.fileChangeSummaryBar.setButtonStatus(status);
      });
    });
  }

  /**
   * Unbind the FileChangeSummaryBar and dispose side effects.
   */
  public void unbindFileChangeSummaryBar() {
    ensureRealm(() -> {
      if (filesSideEffect != null) {
        filesSideEffect.dispose();
        filesSideEffect = null;
      }
      if (buttonEnableSideEffect != null) {
        buttonEnableSideEffect.dispose();
        buttonEnableSideEffect = null;
      }

      ConversationUtils.confirmEndChat();
      disposeFileChangeSummaryBar();

      // Clear observables to prevent stale data when view is reopened
      filesObservable.setValue(new LinkedHashMap<>());
      buttonEnableObservable.setValue(false);
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
  public void setChangedFiles(Map<IFile, FileChangeProperty> files) {
    ensureRealm(() -> {
      filesObservable.setValue(files);
    });
  }

  /**
   * Get the changed files for the file change summary bar.
   */
  public Map<IFile, FileChangeProperty> getChangedFiles() {
    return filesObservable.getValue();
  }

  /**
   * Get the FileChangeSummaryBar instance.
   */
  public FileChangeSummaryBar getFileChangeSummaryBar() {
    return fileChangeSummaryBar;
  }

  /**
   * Add a newly created file to the file change summary bar.
   */
  public void addChangedFile(IFile file, FileChangeType fileChangeType) {
    ensureRealm(() -> {
      Map<IFile, FileChangeProperty> filesMap = new LinkedHashMap<>(filesObservable.getValue());
      if (filesMap.containsKey(file)) {
        return;
      }
      filesMap.put(file, new FileChangeProperty(fileChangeType));
      filesObservable.setValue(filesMap);
      buttonEnableObservable.setValue(false);
    });
  }

  /**
   * Complete a changed file action and remove it from the file change summary bar.
   *
   * @param file the file to complete
   */
  public void completeFile(IFile file) {
    ensureRealm(() -> {
      Map<IFile, FileChangeProperty> filesMap = new LinkedHashMap<>(filesObservable.getValue());
      filesMap.remove(file);
      filesObservable.setValue(filesMap);

      if (filesMap.isEmpty()) {
        onResolveAllChanges();
      }
    });
  }

  /**
   * Get the file change type of a file.
   *
   * @param file the file to get the change type for
   * @return the file change type, or null if the file is not in the list
   */
  public FileChangeType getFileChangeTypeOf(IFile file) {
    FileChangeProperty property = filesObservable.getValue().get(file);
    if (property != null) {
      return property.getChangeType();
    } else {
      return null;
    }
  }

  /**
   * Handles the action of keeping changes to a file.
   *
   * @param file the file to keep changes for
   */
  public void onKeepChange(IFile file) {
    if (getFileChangeTypeOf(file) == FileChangeType.Created) {
      this.createFileTool.onKeepChange(file);
    } else if (getFileChangeTypeOf(file) == FileChangeType.Changed) {
      this.editFileTool.onKeepChange(file);
    }
    this.completeFile(file);
  }

  /**
   * Handles the action of keeping all changes to files.
   */
  public void onKeepAllChanges() {
    this.createFileTool.onKeepAllChanges(getCreatedFiles());
    this.editFileTool.onKeepAllChanges(getEditedFiles());
    onResolveAllChanges();
  }

  /**
   * Handles the action of undoing changes to a file.
   *
   * @param file the file to undo changes for
   */
  public void onUndoChange(IFile file) {
    try {
      if (getFileChangeTypeOf(file) == FileChangeType.Created) {
        this.createFileTool.onUndoChange(file);
      } else if (getFileChangeTypeOf(file) == FileChangeType.Changed) {
        this.editFileTool.onUndoChange(file);
      }
    } catch (CoreException e) {
      CopilotCore.LOGGER.error("Error undoing changes for the new file", e);
    }
    this.completeFile(file);
  }

  /**
   * Handles the action of undoing all changes to files.
   */
  public void onUndoAllChanges() {
    try {
      this.createFileTool.onUndoAllChanges(getCreatedFiles());
      this.editFileTool.onUndoAllChanges(getEditedFiles());
    } catch (CoreException e) {
      CopilotCore.LOGGER.error("Error undoing all changes for the files", e);
    }
    onResolveAllChanges();
  }

  /**
   * Handles the action of viewing the diff of a file.
   *
   * @param file the file to view the diff for
   */
  public void onViewDiff(IFile file) {
    if (getFileChangeTypeOf(file) == FileChangeType.Created) {
      this.createFileTool.onViewDiff(file);
    } else if (getFileChangeTypeOf(file) == FileChangeType.Changed) {
      this.editFileTool.onViewDiff(file);
    }
  }

  /**
   * Handles the action of clicking done button to resolve all changes.
   */
  public void onResolveAllChanges() {
    this.createFileTool.onResolveAllChanges();
    this.editFileTool.onResolveAllChanges();
    ensureRealm(() -> {
      this.filesObservable.setValue(new LinkedHashMap<>());
      this.buttonEnableObservable.setValue(false);
      this.disposeFileChangeSummaryBar();
    });
  }

  /**
   * Dispose the FileChangeSummaryBar.
   */
  public void disposeFileChangeSummaryBar() {
    if (fileChangeSummaryBar != null && !fileChangeSummaryBar.isDisposed()) {
      Composite control = fileChangeSummaryBar.getParent();
      fileChangeSummaryBar.dispose();
      fileChangeSummaryBar = null;
      control.requestLayout();
    }
  }

  private List<IFile> getCreatedFiles() {
    List<IFile> createdFiles = new ArrayList<>();
    for (Map.Entry<IFile, FileChangeProperty> entry : this.filesObservable.getValue().entrySet()) {
      if (entry.getValue().getChangeType() == FileChangeType.Created) {
        createdFiles.add(entry.getKey());
      }
    }
    return createdFiles;
  }

  private List<IFile> getEditedFiles() {
    List<IFile> editedFiles = new ArrayList<>();
    for (Map.Entry<IFile, FileChangeProperty> entry : this.filesObservable.getValue().entrySet()) {
      if (entry.getValue().getChangeType() == FileChangeType.Changed) {
        editedFiles.add(entry.getKey());
      }
    }
    return editedFiles;
  }

  /**
   * Class for file change properties. changeType - The type of file change (new or edited). isCompleted - Whether the
   * file change is completed or not.
   */
  public static class FileChangeProperty {
    private FileChangeType changeType;

    /**
     * Constructor for FileChangeProperty.
     *
     * @param changeType The type of file change (new or edited).
     */
    public FileChangeProperty(FileChangeType changeType) {
      this.changeType = changeType;
    }

    public FileChangeType getChangeType() {
      return changeType;
    }
  }
}

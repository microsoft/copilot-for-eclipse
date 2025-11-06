package com.microsoft.copilot.eclipse.ui.preferences;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;
import com.microsoft.copilot.eclipse.core.chat.CustomChatModeManager;
import com.microsoft.copilot.eclipse.core.chat.service.ICustomModeService;
import com.microsoft.copilot.eclipse.core.utils.WorkspaceUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Preference page for managing custom chat modes.
 */
public class CustomModesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.CustomModesPreferencePage";
  private static final String SEPARATOR_PREFIX = "---";

  private Table modesTable;
  private Button addButton;
  private Button editButton;
  private Button deleteButton;
  private List<CustomChatMode> customModes;
  private boolean modesChanged = false;

  @Override
  public void init(IWorkbench workbench) {
    setDescription(Messages.customModes_page_description);
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(2, false);
    container.setLayout(layout);

    // Table to show existing modes
    modesTable = new Table(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
    modesTable.setHeaderVisible(true);
    modesTable.setLinesVisible(true);
    GridData gdTable = new GridData(SWT.FILL, SWT.FILL, true, true);
    gdTable.heightHint = 300;
    modesTable.setLayoutData(gdTable);

    TableColumn nameColumn = new TableColumn(modesTable, SWT.NONE);
    nameColumn.setText(Messages.customModes_table_column_modeName);
    nameColumn.setWidth(120);

    TableColumn workspaceColumn = new TableColumn(modesTable, SWT.NONE);
    workspaceColumn.setText(Messages.customModes_table_column_workspace);
    workspaceColumn.setWidth(100);

    TableColumn descColumn = new TableColumn(modesTable, SWT.NONE);
    descColumn.setText(Messages.customModes_table_column_description);
    descColumn.setWidth(280);

    // Button panel
    Composite buttonPanel = new Composite(container, SWT.NONE);
    buttonPanel.setLayout(new GridLayout(1, false));
    GridData gdButtons = new GridData(SWT.FILL, SWT.TOP, false, false);
    buttonPanel.setLayoutData(gdButtons);

    addButton = new Button(buttonPanel, SWT.PUSH);
    addButton.setText(Messages.customModes_button_add);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    addButton.addListener(SWT.Selection, e -> handleAddMode());

    editButton = new Button(buttonPanel, SWT.PUSH);
    editButton.setText(Messages.customModes_button_edit);
    editButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    editButton.addListener(SWT.Selection, e -> handleEditMode());
    editButton.setEnabled(false);

    deleteButton = new Button(buttonPanel, SWT.PUSH);
    deleteButton.setText(Messages.customModes_button_delete);
    deleteButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    deleteButton.addListener(SWT.Selection, e -> handleDeleteMode());
    deleteButton.setEnabled(false);

    // Info label at the bottom
    Label infoLabel = new Label(container, SWT.WRAP);
    infoLabel.setText(Messages.customModes_info_label);
    GridData gdInfo = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gdInfo.horizontalSpan = 2;
    gdInfo.widthHint = 600;
    infoLabel.setLayoutData(gdInfo);

    // Table selection listener
    modesTable.addListener(SWT.Selection, e -> {
      boolean hasSelection = modesTable.getSelectionIndex() >= 0;
      editButton.setEnabled(hasSelection);
      deleteButton.setEnabled(hasSelection);
    });

    // Double-click to edit
    modesTable.addListener(SWT.MouseDoubleClick, e -> handleEditMode());

    // Load existing modes
    loadModes();

    return container;
  }

  /**
   * Load custom modes into the table.
   */
  private void loadModes() {
    if (modesTable.isDisposed()) {
      return;
    }

    modesTable.removeAll();
    customModes = CustomChatModeManager.INSTANCE.getCustomModes();

    for (CustomChatMode mode : customModes) {
      TableItem item = new TableItem(modesTable, SWT.NONE);
      item.setText(0, mode.getDisplayName());
      item.setText(1, getWorkspaceNameForMode(mode));
      item.setText(2, mode.getDescription() != null ? mode.getDescription() : "");
      item.setData(mode);
    }

    modesTable.update();
  }

  /**
   * Get the workspace name for a custom mode based on its file path.
   */
  private String getWorkspaceNameForMode(CustomChatMode mode) {
    try {
      String modeId = mode.getId();
      Path modePath = Paths.get(java.net.URI.create(modeId));

      List<WorkspaceFolder> workspaceFolders = WorkspaceUtils.listWorkspaceFolders();
      if (workspaceFolders != null) {
        for (WorkspaceFolder folder : workspaceFolders) {
          Path folderPath = Paths.get(java.net.URI.create(folder.getUri()));
          if (modePath.startsWith(folderPath)) {
            return folder.getName();
          }
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to get workspace name for mode", e);
    }
    return "";
  }

  /**
   * Handle adding a new mode.
   */
  private void handleAddMode() {
    ICustomModeService service = CustomChatModeManager.INSTANCE.getCustomModeService();

    // Check if there are workspace folders
    List<WorkspaceFolder> workspaceFolders = WorkspaceUtils.listWorkspaceFolders();

    if (workspaceFolders == null || workspaceFolders.isEmpty()) {
      MessageDialog.openError(getShell(), Messages.customModes_error_noWorkspaceFolder_title,
          Messages.customModes_error_noWorkspaceFolder_message);
      return;
    }

    // Show dialog with name input and workspace folder selection
    CreateModeDialog dialog = new CreateModeDialog(getShell(), workspaceFolders);

    if (dialog.open() == Window.OK) {
      String modeName = dialog.getModeName();
      WorkspaceFolder targetFolder = dialog.getSelectedFolder();

      if (StringUtils.isNotBlank(modeName) && targetFolder != null) {
        try {
          // Create a new mode file with default template
          service.createCustomModeInWorkspaceFolder(targetFolder, modeName).thenCompose(newMode -> {
            // Reload from file system
            CustomChatModeManager manager = CustomChatModeManager.INSTANCE;
            return manager.syncCustomModesFromService().thenApply(v -> newMode);
          }).thenAccept(newMode -> {
            // Run UI operations on the display thread
            SwtUtils.invokeOnDisplayThreadAsync(() -> {
              // Refresh workspace to show new file in Project Explorer
              refreshWorkspace();

              // Mark that modes were changed (will reload when dialog reopens)
              modesChanged = true;

              // Open the file for editing
              openModeFileInEditor(newMode.getId());

              // Close the preference dialog to let the user focus on editing
              closePreferenceDialog();
            });
          }).exceptionally(ex -> {
            CopilotCore.LOGGER.error("Failed to add mode", ex);
            SwtUtils.invokeOnDisplayThreadAsync(() -> {
              MessageDialog.openError(getShell(), Messages.customModes_error_createFailed_title,
                  String.format(Messages.customModes_error_createFailed_message, ex.getMessage()));
            });
            return null;
          });

        } catch (Exception ex) {
          CopilotCore.LOGGER.error("Failed to add mode", ex);
          MessageDialog.openError(getShell(), Messages.customModes_error_createFailed_title,
              String.format(Messages.customModes_error_createFailed_message, ex.getMessage()));
        }
      }
    }
  }

  /**
   * Dialog for creating a new custom mode with name and workspace folder selection.
   */
  private static class CreateModeDialog extends org.eclipse.jface.dialogs.Dialog {
    private List<WorkspaceFolder> folders;
    private Text nameText;
    private Combo folderCombo;
    private String modeName;
    private WorkspaceFolder selectedFolder;

    public CreateModeDialog(Shell parentShell, List<WorkspaceFolder> folders) {
      super(parentShell);
      this.folders = folders;
    }

    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText(Messages.customModes_dialog_createMode_title);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      Composite container = (Composite) super.createDialogArea(parent);
      GridLayout layout = new GridLayout(2, false);
      layout.marginHeight = 10;
      layout.marginWidth = 10;
      container.setLayout(layout);

      // Mode name label and text field
      Label nameLabel = new Label(container, SWT.NONE);
      nameLabel.setText(Messages.customModes_dialog_modeName_label);

      nameText = new Text(container, SWT.BORDER);
      GridData nameData = new GridData(SWT.FILL, SWT.CENTER, true, false);
      nameData.widthHint = 300;
      nameText.setLayoutData(nameData);

      // Only show folder selection if there are multiple folders
      if (folders.size() > 1) {
        // Folder label and combo
        Label folderLabel = new Label(container, SWT.NONE);
        folderLabel.setText(Messages.customModes_dialog_workspaceFolder_label);

        folderCombo = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
        GridData comboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        folderCombo.setLayoutData(comboData);

        // Populate combo with folder names
        for (WorkspaceFolder folder : folders) {
          folderCombo.add(folder.getName());
        }

        // Select first folder by default
        folderCombo.select(0);
      }

      return container;
    }

    @Override
    protected void okPressed() {
      modeName = nameText.getText().trim();

      // Validate mode name
      if (modeName.isEmpty()) {
        MessageDialog.openError(getShell(), Messages.customModes_dialog_error_emptyName_title,
            Messages.customModes_dialog_error_emptyName_message);
        return;
      }

      if (modeName.startsWith(SEPARATOR_PREFIX)) {
        MessageDialog.openError(getShell(), Messages.customModes_dialog_error_invalidName_title,
            Messages.customModes_dialog_error_invalidName_message);
        return;
      }

      // Get selected workspace folder
      if (folders.size() == 1) {
        // Only one folder, use it automatically
        selectedFolder = folders.get(0);
      } else {
        // Multiple folders, get from combo
        int selectedIndex = folderCombo.getSelectionIndex();
        if (selectedIndex >= 0 && selectedIndex < folders.size()) {
          selectedFolder = folders.get(selectedIndex);
        }

        if (selectedFolder == null) {
          MessageDialog.openError(getShell(), Messages.customModes_dialog_error_noFolder_title,
              Messages.customModes_dialog_error_noFolder_message);
          return;
        }
      }

      super.okPressed();
    }

    public String getModeName() {
      return modeName;
    }

    public WorkspaceFolder getSelectedFolder() {
      return selectedFolder;
    }
  }

  /**
   * Handle editing an existing mode.
   */
  private void handleEditMode() {
    int index = modesTable.getSelectionIndex();
    if (index < 0) {
      return;
    }

    TableItem item = modesTable.getItem(index);
    CustomChatMode mode = (CustomChatMode) item.getData();

    if (mode != null) {
      openModeFileInEditor(mode.getId());
      modesChanged = true;

      // Close the preference dialog to let the user focus on editing
      closePreferenceDialog();
    }
  }

  /**
   * Handle deleting a mode.
   */
  private void handleDeleteMode() {
    int index = modesTable.getSelectionIndex();
    if (index < 0) {
      return;
    }

    TableItem item = modesTable.getItem(index);
    CustomChatMode mode = (CustomChatMode) item.getData();

    if (mode != null) {
      boolean confirmed = MessageDialog.openConfirm(getShell(), Messages.customModes_delete_confirm_title,
          String.format(Messages.customModes_delete_confirm_message, mode.getDisplayName()));

      if (confirmed) {
        try {
          String modeId = mode.getId();
          CustomChatModeManager manager = CustomChatModeManager.INSTANCE;

          // Close any open editors for this file before deleting
          closeEditorForMode(modeId);

          // Delete and sync asynchronously
          manager.deleteCustomMode(modeId).thenCompose(v -> manager.syncCustomModesFromService()).thenAccept(v -> {
            // Run UI operations on the display thread
            SwtUtils.invokeOnDisplayThreadAsync(() -> {
              // Refresh workspace to remove deleted file from Project Explorer
              refreshWorkspace();

              // Mark that modes were changed (will reload when dialog reopens)
              modesChanged = true;

              // Reload the table to reflect the deletion
              loadModes();
            });
          }).exceptionally(ex -> {
            CopilotCore.LOGGER.error("Failed to delete mode", ex);
            SwtUtils.invokeOnDisplayThreadAsync(() -> {
              MessageDialog.openError(getShell(), Messages.customModes_error_deleteFailed_title,
                  String.format(Messages.customModes_error_deleteFailed_message, ex.getMessage()));
            });
            return null;
          });

        } catch (Exception ex) {
          CopilotCore.LOGGER.error("Failed to delete mode", ex);
          MessageDialog.openError(getShell(), Messages.customModes_error_deleteFailed_title,
              String.format(Messages.customModes_error_deleteFailed_message, ex.getMessage()));
        }
      }
    }
  }

  /**
   * Close the preference dialog if this page is displayed within one.
   */
  private void closePreferenceDialog() {
    if (getContainer() != null && getContainer() instanceof PreferenceDialog) {
      ((PreferenceDialog) getContainer()).close();
    }
  }

  /**
   * Close any open editors for the custom mode file.
   */
  private void closeEditorForMode(String modeId) {
    try {
      Path filePath = Paths.get(java.net.URI.create(modeId));
      File file = filePath.toFile();

      if (!file.exists()) {
        return;
      }

      IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      if (page == null) {
        return;
      }

      // Try to find the file in the workspace first
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      IFile workspaceFile = findFileInWorkspace(workspaceRoot, filePath);

      IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());

      // Iterate through all editor references to find and close matching editors
      IEditorReference[] editorRefs = page.getEditorReferences();
      for (IEditorReference editorRef : editorRefs) {
        try {
          IEditorInput editorInput = editorRef.getEditorInput();

          // Check if it's a workspace file editor
          if (workspaceFile != null && editorInput instanceof FileEditorInput) {
            FileEditorInput fileInput = (FileEditorInput) editorInput;
            if (workspaceFile.equals(fileInput.getFile())) {
              page.closeEditor(editorRef.getEditor(true), false);
              CopilotCore.LOGGER.info("Closed workspace editor for mode file: " + filePath);
            }
          } else if (editorInput instanceof org.eclipse.ui.ide.FileStoreEditorInput) {
            // Check if it's an external file editor
            org.eclipse.ui.ide.FileStoreEditorInput storeInput = (org.eclipse.ui.ide.FileStoreEditorInput) editorInput;
            if (fileStore.toURI().equals(storeInput.getURI())) {
              page.closeEditor(editorRef.getEditor(true), false);
              CopilotCore.LOGGER.info("Closed external editor for mode file: " + filePath);
            }
          }
        } catch (PartInitException e) {
          CopilotCore.LOGGER.error("Failed to get editor input for closing", e);
          // Continue checking other editors
        }
      }

    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to close editors for mode file", e);
      // Continue with deletion even if closing editors fails
    }
  }

  /**
   * Open the .agent.md file in Eclipse's Agent File Editor.
   */
  private void openModeFileInEditor(String modeId) {
    try {
      Path filePath = Paths.get(java.net.URI.create(modeId));
      File file = filePath.toFile();

      if (!file.exists()) {
        MessageDialog.openError(getShell(), Messages.customModes_error_fileNotFound_title,
            String.format(Messages.customModes_error_fileNotFound_message, file.getAbsolutePath()));
        return;
      }

      // Try to find the file in the workspace first
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      IFile workspaceFile = findFileInWorkspace(workspaceRoot, filePath);

      IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      String agentEditorId = "com.microsoft.copilot.eclipse.ui.editors.AgentFileEditor";

      if (workspaceFile != null && workspaceFile.exists()) {
        // File is in workspace - open it with Agent File Editor
        try {
          IDE.openEditor(page, workspaceFile, agentEditorId);
        } catch (PartInitException e) {
          CopilotCore.LOGGER.error("Failed to open workspace file in Agent File Editor", e);
          // Fall back to external file opening
          openExternalFileWithEditor(page, file, agentEditorId);
        }
      } else {
        // File is outside workspace - open as external file with Agent File Editor
        openExternalFileWithEditor(page, file, agentEditorId);
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to open mode file", e);
      MessageDialog.openError(getShell(), Messages.customModes_error_openFailed_title,
          String.format(Messages.customModes_error_openFailed_message, e.getMessage()));
    }
  }

  /**
   * Find a file in the workspace that matches the given path.
   */
  private IFile findFileInWorkspace(IWorkspaceRoot workspaceRoot, Path targetPath) {
    IProject[] projects = workspaceRoot.getProjects();
    for (IProject project : projects) {
      if (project.isOpen()) {
        IPath projectLocation = project.getLocation();
        if (projectLocation != null) {
          java.nio.file.Path projectPath = projectLocation.toFile().toPath();
          if (targetPath.startsWith(projectPath)) {
            // File is under this project
            java.nio.file.Path relativePath = projectPath.relativize(targetPath);
            IFile file = project.getFile(relativePath.toString());
            if (file.exists()) {
              return file;
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Open a file that's outside the workspace using Eclipse's external file support with specific editor.
   */
  private void openExternalFileWithEditor(IWorkbenchPage page, File file, String editorId) throws PartInitException {
    IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
    FileStoreEditorInput editorInput = new FileStoreEditorInput(fileStore);
    page.openEditor(editorInput, editorId);
  }

  /**
   * Refresh the workspace to show file changes in Project Explorer.
   */
  private void refreshWorkspace() {
    try {
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      IProject[] projects = workspaceRoot.getProjects();

      for (IProject project : projects) {
        if (project.isOpen()) {
          // Refresh the .github/agents folder if it exists
          IFolder agentsFolder = project.getFolder(".github/agents");
          if (agentsFolder.exists()) {
            agentsFolder.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
          }
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to refresh workspace", e);
    }
  }

  @Override
  public boolean performOk() {
    // If modes were changed, sync them
    if (modesChanged) {
      CustomChatModeManager.INSTANCE.syncCustomModesFromService();
    }
    return super.performOk();
  }
}

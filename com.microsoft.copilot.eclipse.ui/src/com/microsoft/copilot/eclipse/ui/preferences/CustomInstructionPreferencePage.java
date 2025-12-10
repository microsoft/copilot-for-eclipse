package com.microsoft.copilot.eclipse.ui.preferences;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Preference page for GitHub Copilot Custom Instructions settings.
 */
public class CustomInstructionPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.CustomInstructionPreferencePage";

  private BooleanFieldEditor enableWorkspaceInstrField;
  private StringFieldEditor workspaceInstrField;

  // Variables to track initial preference values for change detection
  private boolean initialWorkspaceEnabled;
  private String initialWorkspaceInstructions;

  private static final String GITHUB = ".github";
  private static final String COPILOT_INSTRUCTIONS = "copilot-instructions.md";

  // Constants for tooltip configuration
  private static final int TOOLTIP_OFFSET_Y = 20;
  private static final int TOOLTIP_AUTO_HIDE_DELAY_MS = 5000;

  /**
   * Constructor for CustomInstructionPreferencePage.
   */
  public CustomInstructionPreferencePage() {
    super(GRID);
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

  @Override
  protected void createFieldEditors() {
    Composite parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));
    GridLayout gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    createWorkspaceInstructionsField(parent, gl);
    createProjectInstructionsField(parent, gl);

    // Initialize tracking of preference values after fields are created
    initializePreferenceValues();
  }

  /**
   * Initialize the tracking variables with current preference values.
   */
  private void initializePreferenceValues() {
    initialWorkspaceEnabled = getPreferenceStore().getBoolean(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED);
    initialWorkspaceInstructions = getPreferenceStore().getString(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE);
  }

  /**
   * Check if any preference values have changed from their initial values.
   *
   * @return true if any preferences have changed, false otherwise
   */
  private boolean hasPreferencesChanged() {
    boolean currentWorkspaceEnabled = enableWorkspaceInstrField.getBooleanValue();
    String currentWorkspaceInstructions = workspaceInstrField.getStringValue();

    return currentWorkspaceEnabled != initialWorkspaceEnabled
        || !StringUtils.equals(currentWorkspaceInstructions, initialWorkspaceInstructions);
  }

  @Override
  public boolean performOk() {
    // Save the current preference values
    initialWorkspaceEnabled = enableWorkspaceInstrField.getBooleanValue();
    initialWorkspaceInstructions = workspaceInstrField.getStringValue();

    // Call super to save preferences
    return super.performOk();
  }

  private void createWorkspaceInstructionsField(Composite parent, GridLayout gl) {
    // workspace instructions group
    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);
    Group workspaceInstrGroup = new Group(parent, SWT.NONE);
    workspaceInstrGroup.setLayout(gl);
    gdf.applyTo(workspaceInstrGroup);
    workspaceInstrGroup.setText(Messages.preferences_page_custom_instructions_workspace);

    // Add workspace instructions field
    Composite workspaceInstrFieldContainer = new Composite(workspaceInstrGroup, SWT.NONE);
    workspaceInstrFieldContainer.setLayout(gl);
    workspaceInstrFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    // Add enable workspace instructions checkbox - use same parent as workspaceInstrField
    enableWorkspaceInstrField = new BooleanFieldEditor(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED,
        Messages.preferences_page_custom_instructions_workspace_enable, workspaceInstrFieldContainer);
    addField(enableWorkspaceInstrField);

    PreferencePageUtils.createExternalLink(workspaceInstrFieldContainer,
        Messages.preferences_page_custom_instructions_copilot_instructions_desc, null);

    workspaceInstrField = new StringFieldEditor(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE, "",
        StringFieldEditor.UNLIMITED, 4, StringFieldEditor.VALIDATE_ON_KEY_STROKE, workspaceInstrFieldContainer);
    // disable the label of the input field, so that the input box can be positioned at the beginning
    // of the container.
    workspaceInstrField.getLabelControl(workspaceInstrFieldContainer).dispose();
    addField(workspaceInstrField);

    // Add note using WrappableNoteLabel
    new WrappableNoteLabel(workspaceInstrGroup, Messages.preferences_page_note_prefix + " ",
        Messages.preferences_page_custom_instructions_copilot_instructions_note);
  }

  private void createProjectInstructionsField(Composite parent, GridLayout gl) {
    // project instructions group
    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);
    Group projectInstrGroup = new Group(parent, SWT.NONE);
    projectInstrGroup.setLayout(gl);
    gdf.applyTo(projectInstrGroup);
    projectInstrGroup.setText(Messages.preferences_page_custom_instructions_project);

    // Add project instructions field
    Composite projectInstrFieldContainer = new Composite(projectInstrGroup, SWT.NONE);
    GridLayout projectInstrFieldLayout = new GridLayout(1, true);
    projectInstrFieldLayout.marginWidth = 0;
    projectInstrFieldLayout.marginHeight = 0;
    projectInstrFieldContainer.setLayout(projectInstrFieldLayout);
    projectInstrFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    PreferencePageUtils.createExternalLink(projectInstrFieldContainer,
        Messages.preferences_page_custom_instructions_project_intro, null);

    // Create a container for the table and buttons
    Composite tableContainer = new Composite(projectInstrFieldContainer, SWT.NONE);
    GridLayout tableLayout = new GridLayout(2, false);
    tableLayout.marginWidth = 0;
    tableLayout.marginHeight = 0;
    tableContainer.setLayout(tableLayout);
    tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    // Create the table
    Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    GridData tableGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    tableGridData.heightHint = 150; // Set a reasonable height
    table.setLayoutData(tableGridData);

    // Create table columns
    TableColumn projectNameColumn = new TableColumn(table, SWT.LEFT);
    projectNameColumn.setText(Messages.preferences_page_custom_instructions_project_table_projectName);
    projectNameColumn.setWidth(150);

    TableColumn fileLocationColumn = new TableColumn(table, SWT.LEFT);
    fileLocationColumn.setText(Messages.preferences_page_custom_instructions_project_table_fileLocation);
    // Set the file location column to take remaining width (table width - project name column width)
    fileLocationColumn.setWidth(tableGridData.widthHint > 0 ? tableGridData.widthHint - 150 : 400);

    // Add resize listener to make the file location column take remaining width
    table.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(org.eclipse.swt.events.ControlEvent e) {
        int tableWidth = table.getClientArea().width;
        int projectNameWidth = projectNameColumn.getWidth();
        int remainingWidth = tableWidth - projectNameWidth;
        if (remainingWidth > 100) { // Minimum width for file location column
          fileLocationColumn.setWidth(remainingWidth);
        }
      }
    });

    // Populate table with actual workspace projects
    populateProjectTable(table);

    // Create edit button
    createButton(tableContainer, table);

    // Add note using WrappableNoteLabel
    new WrappableNoteLabel(projectInstrGroup, Messages.preferences_page_note_prefix + " ",
        Messages.preferences_page_custom_instructions_project_table_note);
  }

  private void createButton(Composite tableContainer, Table table) {
    // Create button container
    Composite buttonContainer = new Composite(tableContainer, SWT.NONE);
    GridLayout buttonLayout = new GridLayout(1, false);
    buttonContainer.setLayout(buttonLayout);
    buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

    // Edit button
    Button editButton = new Button(buttonContainer, SWT.PUSH);
    editButton.setText(Messages.preferences_page_custom_instructions_project_table_editButton);
    GridData editButtonGridData = new GridData(SWT.FILL, SWT.TOP, true, false);
    editButtonGridData.widthHint = Math.max(convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH),
        editButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x) + 5;
    editButton.setLayoutData(editButtonGridData);
    editButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      handleEditButtonClick(table);
    }));

    // Enable/disable buttons based on selection
    table.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      boolean hasSelection = table.getSelectionIndex() >= 0;
      editButton.setEnabled(hasSelection);
    }));

    // Initially disable buttons if no selection
    editButton.setEnabled(false);
  }

  private void handleEditButtonClick(Table table) {
    int selectionIndex = table.getSelectionIndex();
    if (selectionIndex < 0) {
      return;
    }

    String projectName = table.getItem(selectionIndex).getText(0);
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

    if (project == null || !project.exists()) {
      return;
    }

    openInstructionFile(project, projectName);
  }

  private void openInstructionFile(IProject project, String projectName) {
    IPath projectLocation = project.getLocation();
    if (projectLocation == null) {
      return;
    }

    IFile instructionFile = project.getFolder(GITHUB).getFile(COPILOT_INSTRUCTIONS);
    try {
      instructionFile.refreshLocal(IFile.DEPTH_ZERO, new NullProgressMonitor());
      if (!instructionFile.exists()) {
        return;
      }

      IWorkbenchPage page = UiUtils.getActivePage();
      IDE.openEditor(page, instructionFile);

      promptToClosePreferencePage();
    } catch (IllegalStateException | CoreException ex) {
      CopilotCore.LOGGER.error("Failed to open copilot-instructions.md in editor", ex);
    }
  }

  /**
   * Shows a tooltip reminder to the user to save the file after editing.
   */
  private void showSaveReminderTooltip() {
    SwtUtils.getDisplay().asyncExec(() -> {
      Shell activeShell = SwtUtils.getDisplay().getActiveShell();
      if (activeShell == null) {
        return;
      }

      Point tooltipPosition = tryGetEditorPosition();
      if (tooltipPosition == null) {
        return;
      }

      ToolTip tooltip = new ToolTip(activeShell, SWT.BALLOON | SWT.ICON_INFORMATION);
      tooltip.setText(Messages.preferences_page_custom_instructions_project_file_save_reminder_title);
      tooltip.setMessage(Messages.preferences_page_custom_instructions_project_file_save_reminder_desc);
      tooltip.setLocation(tooltipPosition);
      tooltip.setVisible(true);
      tooltip.setAutoHide(true);

      scheduleTooltipAutoHide(tooltip);
    });
  }

  /**
   * Attempts to get the position relative to the active editor.
   */
  private Point tryGetEditorPosition() {
    try {
      IWorkbenchPage page = UiUtils.getActivePage();
      if (page == null || page.getActiveEditor() == null) {
        return null;
      }

      IEditorPart activeEditor = page.getActiveEditor();
      if (!(activeEditor instanceof EditorPart)) {
        return null;
      }

      ITextViewer textViewer = activeEditor.getAdapter(ITextViewer.class);
      if (textViewer == null) {
        return null;
      }

      // Get the text widget and its caret position
      StyledText textWidget = textViewer.getTextWidget();
      if (textWidget == null || textWidget.isDisposed()) {
        return null;
      }

      // Get caret location relative to the text widget
      Point caretLocation = textWidget.getLocationAtOffset(textWidget.getCaretOffset());

      // Convert to display coordinates
      Point displayLocation = textWidget.toDisplay(caretLocation.x, caretLocation.y);
      return new Point(displayLocation.x, displayLocation.y + TOOLTIP_OFFSET_Y);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to get editor position for tooltip", e);
      return null;
    }
  }

  /**
   * Schedules the tooltip to auto-hide after a delay.
   */
  private void scheduleTooltipAutoHide(ToolTip tooltip) {
    SwtUtils.getDisplay().timerExec(TOOLTIP_AUTO_HIDE_DELAY_MS, () -> {
      if (!tooltip.isDisposed()) {
        tooltip.dispose();
      }
    });
  }

  private void promptToClosePreferencePage() {
    // Check if any preferences have changed
    if (!hasPreferencesChanged()) {
      // No changes made, close the preference page directly
      getShell().close();
      showSaveReminderTooltip();
      return;
    }

    // Preferences have changed, show the dialog to ask user what to do
    int result = MessageDialog.open(MessageDialog.QUESTION, getShell(),
        Messages.preferences_page_custom_instructions_project_editDialog_title,
        Messages.preferences_page_custom_instructions_project_editDialog_message, SWT.NONE,
        Messages.preferences_page_custom_instructions_project_editDialog_button_stay,
        Messages.preferences_page_custom_instructions_project_editDialog_button_close);

    if (result == 1) {
      getShell().close();
      showSaveReminderTooltip();
    }
  }

  private void createTableItem(Table table, String projectName, String fileLocation) {
    TableItem item = new TableItem(table, SWT.NONE);
    item.setText(0, projectName);
    item.setText(1, fileLocation);
  }

  /**
   * Populates the table with actual workspace projects that have copilot instructions files.
   */
  private void populateProjectTable(Table table) {
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (IProject project : projects) {
      if (project.exists() && project.isOpen()) {
        String projectName = project.getName();
        IPath projectLocation = project.getLocation();
        if (projectLocation != null) {
          // Use proper path separator for cross-platform compatibility
          IPath instructionFilePath = projectLocation.append(GITHUB).append(COPILOT_INSTRUCTIONS);

          // Only add projects to the table if the copilot-instructions.md file actually exists
          if (instructionFilePath.toFile().exists()) {
            createTableItem(table, projectName, projectLocation.toOSString());
          }
        }
      }
    }
  }
}

package com.microsoft.copilot.eclipse.ui.dialogs.jobs;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.core.utils.WorkspaceUtils;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.dialogs.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;

/**
 * Dialog to select a project for GitHub Coding Agent. This dialog cannot be skipped and requires user to select a
 * project.
 */
public class ProjectSelectionDialog extends BaseCopilotDialog {

  private Combo projectCombo;
  private List<IProject> projects;
  private String selectedProjectPath;

  /**
   * Constructs a new ProjectSelectionDialog.
   *
   * @param parentShell the parent shell
   */
  public ProjectSelectionDialog(Shell parentShell) {
    super(parentShell);
    this.projects = WorkspaceUtils.listTopLevelProjectsWithGitRepository();
  }

  @Override
  protected String getDialogTitle() {
    return Messages.projectSelectionDialog_title;
  }

  @Override
  protected String getLearnMoreUrl() {
    return UiConstants.GITHUB_COPILOT_CODING_AGENT_LEARN_MORE_URL;
  }

  @Override
  protected String getLearnMoreLinkText() {
    return Messages.githubCodingAgent_link_learnMore;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = (Composite) super.createDialogArea(parent);

    // Create message composite using base class helper
    Composite messageComposite = createMessageComposite(container, Messages.projectSelectionDialog_info_description);

    // Create project selection section if there are multiple projects
    if (projects.size() > 1) {
      // Calculate the left margin based on the icon width and spacing
      int iconWidth = parent.getDisplay().getSystemImage(SWT.ICON_INFORMATION).getBounds().width;
      int horizontalSpacing = ((GridLayout) messageComposite.getLayout()).horizontalSpacing;
      int leftMargin = iconWidth + horizontalSpacing;

      // Create a composite for project selection
      Composite projectComposite = new Composite(container, SWT.NONE);
      projectComposite.setLayout(new GridLayout(2, false));
      GridData projectCompositeData = new GridData(SWT.FILL, SWT.CENTER, true, false);
      projectCompositeData.horizontalIndent = leftMargin; // Indent to align with message text
      projectComposite.setLayoutData(projectCompositeData);

      // Create project selection label
      Label projectLabel = new Label(projectComposite, SWT.NONE);
      projectLabel.setText(Messages.projectSelectionDialog_label_selectProject);
      projectLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

      // Create project dropdown
      projectCombo = new Combo(projectComposite, SWT.READ_ONLY | SWT.DROP_DOWN);
      GridData comboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
      comboData.widthHint = 300;
      projectCombo.setLayoutData(comboData);

      // Populate the combo with project names
      for (IProject project : projects) {
        projectCombo.add(project.getName());
      }

      // Select the first project by default and set selectedProjectPath
      if (projectCombo.getItemCount() > 0) {
        projectCombo.select(0);
        // Set the initial selected project path for the first project
        selectedProjectPath = FileUtils.getResourceUri(projects.get(0));
      }

      // Add selection listener to update selectedProjectPath when user changes selection
      projectCombo.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          int selectionIndex = projectCombo.getSelectionIndex();
          if (selectionIndex >= 0 && selectionIndex < projects.size()) {
            selectedProjectPath = FileUtils.getResourceUri(projects.get(selectionIndex));
          }
        }
      });
    } else if (projects.size() == 1) {
      // Only one project, set it immediately
      selectedProjectPath = FileUtils.getResourceUri(projects.get(0));
    }

    return container;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    // Create Continue button (default)
    Button continueButton = createButton(parent, OK, Messages.projectSelectionDialog_button_continue, true);
    continueButton.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");
    continueButton.setFocus();

    // Create Cancel button
    createButton(parent, CANCEL, Messages.projectSelectionDialog_button_cancel, false);
  }

  /**
   * Get the selected project path.
   *
   * @return the path of the selected project, or null if none selected
   */
  public String getSelectedProjectPath() {
    return selectedProjectPath;
  }

  /**
   * Opens the dialog and returns the selected project path, or null if cancelled.
   *
   * @param parentShell the parent shell
   * @return the selected project path, or null if cancelled
   */
  public static String open(Shell parentShell) {
    ProjectSelectionDialog dialog = new ProjectSelectionDialog(parentShell);
    int result = dialog.open();
    if (result == OK) {
      return dialog.getSelectedProjectPath();
    }
    return null;
  }
}

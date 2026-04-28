// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.utils.McpFileConfigService;
import com.microsoft.copilot.eclipse.core.utils.McpFileSource;
import com.microsoft.copilot.eclipse.core.utils.WorkspaceUtils;

/**
 * Preference sub-page showing discovered file-based MCP server configurations.
  */
public class McpFileConfigPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.McpFileConfigPreferencePage";

  @Override
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, true));
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    createDescriptionArea(container);
    createDiscoveredFilesArea(container);
    createSupportedLocationsArea(container);

    return container;
  }

  private void createDescriptionArea(Composite parent) {
    PreferencePageUtils.createExternalLink(parent,
        Messages.preferences_page_mcp_file_configs_description, null);
  }

  private void createDiscoveredFilesArea(Composite parent) {
    GridLayout gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    Group group = new Group(parent, SWT.NONE);
    group.setLayout(gl);
    GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(group);
    group.setText(Messages.preferences_page_mcp_file_configs_discovered);

    List<McpFileSource> sources = McpFileConfigService.discoverMcpJsonFiles(getProjectRootPaths());

    if (sources.isEmpty()) {
      Label noneLabel = new Label(group, SWT.WRAP);
      noneLabel.setText(Messages.preferences_page_mcp_file_configs_none);
      noneLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    } else {
      Table table = new Table(group, SWT.BORDER | SWT.FULL_SELECTION);
      table.setHeaderVisible(true);
      table.setLinesVisible(true);
      GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
      tableData.heightHint = 150;
      table.setLayoutData(tableData);

      TableColumn sourceCol = new TableColumn(table, SWT.LEFT);
      sourceCol.setText(Messages.preferences_page_mcp_file_configs_col_source);
      sourceCol.setWidth(150);

      TableColumn pathCol = new TableColumn(table, SWT.LEFT);
      pathCol.setText(Messages.preferences_page_mcp_file_configs_col_path);
      pathCol.setWidth(350);

      TableColumn countCol = new TableColumn(table, SWT.LEFT);
      countCol.setText(Messages.preferences_page_mcp_file_configs_col_servers);
      countCol.setWidth(70);

      for (McpFileSource source : sources) {
        TableItem item = new TableItem(table, SWT.NONE);
        item.setText(0, source.getLabel());
        item.setText(1, source.getFilePath().toString());
        item.setText(2, String.valueOf(source.getServerCount()));
      }
    }

    // Merge-order note
    new WrappableNoteLabel(group, Messages.preferences_page_note_prefix + " ",
        Messages.preferences_page_mcp_file_configs_merge_note);
  }

  private void createSupportedLocationsArea(Composite parent) {
    GridLayout gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    Group group = new Group(parent, SWT.NONE);
    group.setLayout(gl);
    GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(group);
    group.setText(Messages.preferences_page_mcp_file_configs_locations);

    // Global path
    Path globalPath = McpFileConfigService.resolveGlobalConfigPath();
    String globalPathStr = globalPath != null ? globalPath.toString()
        : Messages.preferences_page_mcp_file_configs_unknown;
    addLocationRow(group, Messages.preferences_page_mcp_file_configs_loc_global, globalPathStr);

    // Project paths
    addLocationRow(group, Messages.preferences_page_mcp_file_configs_loc_project_vscode,
        "<project>/.vscode/mcp.json");
    addLocationRow(group, Messages.preferences_page_mcp_file_configs_loc_project_copilot,
        "<project>/.github/copilot/mcp.json");
  }

  private void addLocationRow(Composite parent, String label, String path) {
    Composite row = new Composite(parent, SWT.NONE);
    GridLayout rowLayout = new GridLayout(2, false);
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 2;
    row.setLayout(rowLayout);
    row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label labelWidget = new Label(row, SWT.NONE);
    labelWidget.setText(label);
    GridData labelData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    labelData.widthHint = 160;
    labelWidget.setLayoutData(labelData);

    Label pathWidget = new Label(row, SWT.WRAP);
    pathWidget.setText(path);
    pathWidget.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  /**
   * Returns the filesystem paths of all top-level workspace projects.
   *
   * @return list of project root paths
   */
  private List<Path> getProjectRootPaths() {
    List<Path> roots = new ArrayList<>();
    for (IProject project : WorkspaceUtils.listTopLevelProjects()) {
      if (project.getLocationURI() != null) {
        try {
          roots.add(Paths.get(project.getLocationURI()));
        } catch (Exception e) {
          CopilotCore.LOGGER.error("Failed to resolve project path: " + project.getName(), e);
        }
      }
    }
    return roots;
  }
}

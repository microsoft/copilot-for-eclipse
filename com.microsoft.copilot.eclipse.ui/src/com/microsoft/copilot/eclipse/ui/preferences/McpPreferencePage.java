package com.microsoft.copilot.eclipse.ui.preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.McpServerStatus;
import com.microsoft.copilot.eclipse.core.lsp.protocol.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Preference page for GitHub Copilot MCP settings.
 */
public class McpPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

  private static final int NOTE_LABEL_MARGIN = 20;
  private static final Gson GSON = new Gson();

  private Group toolsGroup;

  private Map<String, Map<String, Boolean>> activeToolStatus = new HashMap<>();

  /**
   * Constructor.
   */
  public McpPreferencePage() {
    super(GRID);
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
    Job job = new Job("Binding to MCP service...") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          Job.getJobManager().join(CopilotUi.INIT_JOB_FAMILY, null);
        } catch (OperationCanceledException | InterruptedException e) {
          CopilotCore.LOGGER.error(e);
        }
        CopilotUi.getPlugin().getChatServiceManager().getMcpToolService()
            .bindWithMcpPreferencePage(McpPreferencePage.this);
        return Status.OK_STATUS;
      }
    };
    job.setUser(true);
    job.schedule();
  }

  @Override
  protected void createFieldEditors() {
    Composite parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));
    var gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, false);
    Group mcpGroup = new Group(parent, SWT.NONE);
    mcpGroup.setLayout(gl);
    gdf.applyTo(mcpGroup);
    mcpGroup.setText(Messages.preferences_page_mcp_settings);
    // add mcp field
    var mcpFieldContainer = new Composite(mcpGroup, SWT.NONE);
    mcpFieldContainer.setLayout(gl);
    mcpFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    var mcpField = new StringFieldEditor(Constants.MCP, Messages.preferences_page_mcp, StringFieldEditor.UNLIMITED, 20,
        StringFieldEditor.VALIDATE_ON_KEY_STROKE, mcpFieldContainer);
    mcpField.getLabelControl(mcpFieldContainer).setToolTipText(Messages.preferences_page_mcp_tooltip);
    // @formatter:off
    mcpField.getLabelControl(mcpFieldContainer).setLayoutData(new GridData(
        SWT.LEFT, 
        SWT.TOP, 
        false, 
        false, 
        2, // The label-control will take up 2 column cells itself, so the text-control will be underneath it.
        1));
    // @formatter:on
    addField(mcpField);
    // add note to mcp field
    var mcpNoteComposite = new Composite(mcpGroup, SWT.NONE);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginLeft = -3;
    gridLayout.marginBottom = 1;
    mcpNoteComposite.setLayout(gridLayout);
    mcpNoteComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    var mcpNoteLabel = new Label(mcpNoteComposite, SWT.NONE);
    mcpNoteLabel.setText(Messages.preferences_page_note_text);
    FontData[] fontData = mcpNoteLabel.getFont().getFontData();
    for (FontData fd : fontData) {
      fd.setStyle(SWT.BOLD);
    }
    Font boldFont = new Font(parent.getDisplay(), fontData);
    mcpNoteLabel.setFont(boldFont);
    Label mcpNoteContentLabel = new Label(mcpNoteComposite, SWT.WRAP);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.widthHint = 400;
    mcpNoteContentLabel.setLayoutData(gd);
    mcpNoteContentLabel.setText(Messages.preferences_page_mcp_note_content);

    this.toolsGroup = new Group(parent, SWT.WRAP);
    toolsGroup.setLayout(gl);
    gdf.applyTo(toolsGroup);
    toolsGroup.setText("MCP Tools");

    ControlListener controlListener = new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        // resize the note label
        int width = McpPreferencePage.this.getFieldEditorParent().getSize().x - NOTE_LABEL_MARGIN;
        GridData mcpNoteContentGrid = new GridData(SWT.FILL, SWT.FILL, true, true);
        mcpNoteContentGrid.widthHint = width;
        mcpNoteContentLabel.setLayoutData(mcpNoteContentGrid);
        McpPreferencePage.this.getFieldEditorParent().layout();
      }
    };
    parent.addControlListener(controlListener);
    parent.addDisposeListener(e -> {
      if (boldFont != null && !boldFont.isDisposed()) {
        boldFont.dispose();
      }
      parent.removeControlListener(controlListener);
    });

  }
  
  /**
   * Displays the server names and tool names in the tools group using a tree view.
   */
  public void displayServerToolsInfo(List<McpServerToolsCollection> servers) {
    if (toolsGroup == null || toolsGroup.isDisposed()) {
      return;
    }

    // Clear existing children
    for (var child : toolsGroup.getChildren()) {
      if (child != null && !child.isDisposed()) {
        child.dispose();
      }
    }

    // Create a new Tree widget with checkboxes
    Tree toolsTree = new Tree(toolsGroup, SWT.SINGLE | SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL);
    GridData treeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    treeGridData.heightHint = 300;
    toolsTree.setLayoutData(treeGridData);
    
    Map<String, Map<String, Boolean>> savedToolStatusMap = loadToolStatusFromPreferences();
    
    // Add servers and tools to the tree
    for (McpServerToolsCollection server : servers) {
      if (server == null || server.getStatus() != McpServerStatus.running) {
        continue;
      }

      TreeItem serverNode = new TreeItem(toolsTree, SWT.NONE);
      serverNode.setText(server.getName());

      for (LanguageModelToolInformation tool : server.getTools()) {
        if (tool == null) {
          continue;
        }

        TreeItem toolNode = new TreeItem(serverNode, SWT.NONE);
        toolNode.setText(tool.getName());

        boolean isEnabled = savedToolStatusMap.getOrDefault(server.getName(), Map.of()).getOrDefault(tool.getName(),
            true);
        toolNode.setChecked(isEnabled);
        updateParentCheckstatus(toolNode);

        // Track status map
        Map<String, Boolean> serverTools = activeToolStatus.computeIfAbsent(server.getName(), k -> new HashMap<>());
        serverTools.put(tool.getName(), isEnabled);
      }

      serverNode.setExpanded(true);
    }
    
    // Add selection listener to save status changes
    toolsTree.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (e.detail == SWT.CHECK) {
          TreeItem item = (TreeItem) e.item;
          TreeItem parent = item.getParentItem();
          
          if (parent == null) {
            // Handle parent server node check/uncheck
            boolean checked = item.getChecked();
            TreeItem[] children = item.getItems();
            
            // Apply the checked status to all children
            for (TreeItem child : children) {
              child.setChecked(checked);
              
              // Update the status map for each child
              String serverName = item.getText();
              String toolName = child.getText();
              
              Map<String, Boolean> serverTools = activeToolStatus.computeIfAbsent(serverName, k -> new HashMap<>());
              serverTools.put(toolName, checked);
            }
          } else {
            // Handle child tool item check/uncheck
            String serverName = parent.getText();
            String toolName = item.getText();
            updateParentCheckstatus(item);
            
            // Track status
            Map<String, Boolean> serverTools = activeToolStatus.computeIfAbsent(serverName, k -> new HashMap<>());
            serverTools.put(toolName, item.getChecked());
          }
        }
      }
    });

    toolsGroup.requestLayout();
  }
  
  private void updateParentCheckstatus(TreeItem item) {
    TreeItem parent = item.getParentItem();
    if (parent == null) {
      return;
    }

    TreeItem[] siblings = parent.getItems();
    boolean allChecked = true;
    boolean allUnchecked = true;

    for (TreeItem sibling : siblings) {
      if (sibling.getChecked()) {
        allUnchecked = false;
      } else {
        allChecked = false;
      }
    }

    // Set the parent status - this won't trigger another event
    if (allChecked) {
      parent.setChecked(true);
    } else if (allUnchecked) {
      parent.setChecked(false);
    } else {
      // Some are checked, some are not - use grayed status
      parent.setGrayed(true);
      parent.setChecked(true);
    }
  }

  private Map<String, Map<String, Boolean>> loadToolStatusFromPreferences() {
    Map<String, Map<String, Boolean>> result = new HashMap<>();
    
    IPreferenceStore preferenceStore = getPreferenceStore();
    String jsonStatus = preferenceStore.getString(Constants.MCP_TOOLS_STATUS);
    
    // jsonStatus != null && !jsonStatus.isBlank()
    if (StringUtils.isNotBlank(jsonStatus)) {
      try {
        result = GSON.fromJson(jsonStatus, 
            new com.google.gson.reflect.TypeToken<Map<String, Map<String, Boolean>>>(){}.getType());
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to parse MCP tools status JSON", e);
      }
    }
    
    return result;
  }
  
  @Override
  public boolean performOk() {
    String jsonStatus = GSON.toJson(activeToolStatus);
    IPreferenceStore preferenceStore = getPreferenceStore();
    preferenceStore.setValue(Constants.MCP_TOOLS_STATUS, jsonStatus);
    
    return super.performOk();
  }
}

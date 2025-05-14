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

  private Tree toolsTree;

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
        StringFieldEditor.VALIDATE_ON_KEY_STROKE, mcpFieldContainer) {
      @Override
      protected boolean doCheckState() {
        return validateMcpField(this);
      }
    };

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
    GridDataFactory toolsGdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true);
    toolsGdf.applyTo(toolsGroup);
    toolsGroup.setText(Messages.preferences_page_mcp_tools_settings);

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
  
  private boolean validateMcpField(StringFieldEditor mcpField) {
    String stringValue = mcpField.getStringValue();
    try {
      GSON.fromJson(stringValue, Object.class);
      return true;
    } catch (Exception e) {
      String errorMsg = e.getMessage();
      if (errorMsg != null) {
        int exceptionIndex = errorMsg.indexOf("Exception: ");
        if (exceptionIndex >= 0) {
          errorMsg = errorMsg.substring(exceptionIndex + "Exception: ".length());
        }

        int seeHttpsIndex = errorMsg.indexOf("See https:");
        if (seeHttpsIndex >= 0) {
          errorMsg = errorMsg.substring(0, seeHttpsIndex).trim();
        }
      }
      mcpField.setErrorMessage("SyntaxError: " + errorMsg);
      return false;
    }
  }

  private String getServerRunningStatusHint(McpServerToolsCollection server) {
    switch (server.getStatus()) {
      case running:
      case stopped:
        return StringUtils.EMPTY;
      case error:
        return " " + Messages.preferences_page_mcp_server_init_error;
      default:
        return StringUtils.EMPTY;
    }
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
    toolsTree = new Tree(toolsGroup, SWT.SINGLE | SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL);
    GridData treeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    toolsTree.setLayoutData(treeGridData);
    
    Map<String, Map<String, Boolean>> savedServerToolStatusMap = loadToolStatusFromPreferences();
    
    // Add servers and tools to the tree
    for (McpServerToolsCollection server : servers) {
      if (server == null) {
        continue;
      }

      TreeItem serverNode = new TreeItem(toolsTree, SWT.NONE);
      serverNode.setText(server.getName() + getServerRunningStatusHint(server));

      for (LanguageModelToolInformation tool : server.getTools()) {
        if (tool == null) {
          continue;
        }

        boolean isEnabled = savedServerToolStatusMap.getOrDefault(server.getName(), Map.of())
            .getOrDefault(tool.getName(),
            true);

        TreeItem toolNode = new TreeItem(serverNode, SWT.NONE);
        toolNode.setText(tool.getName());
        toolNode.setChecked(isEnabled);
      }

      serverNode.setExpanded(true);
      updateServerCheckStatus(serverNode);
    }
    
    // Add selection listener to update status changes
    toolsTree.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (e.detail == SWT.CHECK) {
          TreeItem item = (TreeItem) e.item;
          TreeItem parent = item.getParentItem();
          
          if (parent == null) {
            // Handle server node action
            updateToolsCheckStatus(item);
          } else {
            // Handle tool node action
            updateServerCheckStatus(parent);
          }
        }
      }
    });

    toolsGroup.requestLayout();
  }
  
  private void updateServerCheckStatus(TreeItem serverNode) {
    if (serverNode == null) {
      return;
    }

    TreeItem[] toolNodes = serverNode.getItems();
    boolean allChecked = true;
    boolean allUnchecked = true;

    for (TreeItem toolNode : toolNodes) {
      allChecked &= toolNode.getChecked();
      allUnchecked &= !toolNode.getChecked();
    }

    // corner case: server fails to init
    if (toolNodes == null || toolNodes.length == 0) {
      allChecked = false;
    }

    if (allChecked) {
      serverNode.setGrayed(false);
      serverNode.setChecked(true);
    } else if (allUnchecked) {
      serverNode.setGrayed(false);
      serverNode.setChecked(false);
    } else {
      serverNode.setGrayed(true);
      serverNode.setChecked(true);
    }
  }

  private void updateToolsCheckStatus(TreeItem serverNode) {
    if (serverNode == null) {
      return;
    }

    TreeItem[] toolNodes = serverNode.getItems();
    for (TreeItem toolNode : toolNodes) {
      toolNode.setChecked(serverNode.getChecked());
    }

    serverNode.setGrayed(false);
  }

  private Map<String, Map<String, Boolean>> loadToolStatusFromPreferences() {
    Map<String, Map<String, Boolean>> result = new HashMap<>();
    
    IPreferenceStore preferenceStore = getPreferenceStore();
    String jsonStatus = preferenceStore.getString(Constants.MCP_TOOLS_STATUS);
    
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

  private void saveToolStatusToPreferences() {
    if (toolsTree == null || toolsTree.isDisposed()) {
      return;
    }

    Map<String, Map<String, Boolean>> serverToolStatus = new HashMap<>();
    for (TreeItem serverNode : toolsTree.getItems()) {
      String serverName = serverNode.getText();
      Map<String, Boolean> toolStatus = new HashMap<>();
      for (TreeItem toolNode : serverNode.getItems()) {
        toolStatus.put(toolNode.getText(), toolNode.getChecked());
      }
      serverToolStatus.put(serverName, toolStatus);
    }

    String jsonResult = GSON.toJson(serverToolStatus);
    IPreferenceStore preferenceStore = getPreferenceStore();
    preferenceStore.setValue(Constants.MCP_TOOLS_STATUS, jsonResult);
  }

  @Override
  public boolean performOk() {
    saveToolStatusToPreferences();
    return super.performOk();
  }
}

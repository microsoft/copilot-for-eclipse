// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpToolInformation;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * MCP auto-approve preference section with a trust-annotations checkbox
 * and a tree viewer for per-server/tool approval management.
 */
public class McpAutoApproveSection extends Composite {

  private static final int TREE_HEIGHT_HINT = 200;
  private static final Type STRING_LIST_TYPE =
      new TypeToken<List<String>>() {}.getType();

  private Button trustAnnotationsCheckbox;
  private CheckboxTreeViewer treeViewer;
  private Group group;

  private List<McpServerToolsCollection> serverCollections =
      new ArrayList<>();

  // Current check state (lowercased for case-insensitive matching)
  private final Set<String> checkedServers = new HashSet<>();
  private final Set<String> checkedTools = new HashSet<>();

  /** Creates the MCP auto-approve section inside the given parent. */
  public McpAutoApproveSection(Composite parent, int style) {
    super(parent, style);
    setLayout(new GridLayout(1, false));
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    createContents();
  }

  private void createContents() {
    group = new Group(this, SWT.NONE);
    group.setText(Messages.preferences_page_mcp_auto_approve_title);
    group.setLayout(new GridLayout(1, false));
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    group.setBackgroundMode(SWT.INHERIT_FORCE);

    // Trust annotations checkbox
    trustAnnotationsCheckbox = new Button(group, SWT.CHECK);
    trustAnnotationsCheckbox.setText(
        Messages.preferences_page_mcp_auto_approve_trust_annotations);
    trustAnnotationsCheckbox.setLayoutData(
        new GridData(SWT.FILL, SWT.TOP, true, false));

    new WrappableNoteLabel(group,
        Messages.preferences_page_note_prefix + " ",
        Messages.preferences_page_mcp_auto_approve_trust_annotations_note);

    // Server/tool approval label
    Label serverToolsLabel = new Label(group, SWT.NONE);
    serverToolsLabel.setText(
        Messages.preferences_page_mcp_auto_approve_server_tools_label);
    GridData labelData = new GridData(SWT.FILL, SWT.TOP, true, false);
    serverToolsLabel.setLayoutData(labelData);

    // Tree viewer for server/tool approval
    treeViewer = new CheckboxTreeViewer(group,
        SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
    GridData treeData = new GridData(SWT.FILL, SWT.FILL, true, false);
    treeData.heightHint = TREE_HEIGHT_HINT;
    treeViewer.getTree().setLayoutData(treeData);
    SwtUtils.forwardVerticalMouseWheelToParentScrollerAtBoundary(treeViewer.getTree());

    treeViewer.setContentProvider(new McpTreeContentProvider());
    treeViewer.setLabelProvider(new McpTreeLabelProvider());
    treeViewer.addCheckStateListener(new McpCheckStateListener());
    treeViewer.setInput(serverCollections);
  }

  /** Loads MCP auto-approve settings from the preference store. */
  public void loadFromPreferences(IPreferenceStore store) {
    trustAnnotationsCheckbox.setSelection(
        store.getBoolean(Constants.AUTO_APPROVE_TRUST_TOOL_ANNOTATIONS));

    checkedServers.clear();
    checkedTools.clear();

    List<String> servers = loadJsonList(store,
        Constants.AUTO_APPROVE_MCP_SERVERS);
    for (String s : servers) {
      checkedServers.add(s.toLowerCase(Locale.ROOT));
    }

    List<String> tools = loadJsonList(store,
        Constants.AUTO_APPROVE_MCP_TOOLS);
    for (String t : tools) {
      checkedTools.add(t.toLowerCase(Locale.ROOT));
    }

    refreshTreeCheckState();
  }

  /** Saves MCP auto-approve settings to the preference store. */
  public void saveToPreferences(IPreferenceStore store) {
    store.setValue(Constants.AUTO_APPROVE_TRUST_TOOL_ANNOTATIONS,
        trustAnnotationsCheckbox.getSelection());

    store.setValue(Constants.AUTO_APPROVE_MCP_SERVERS,
        new Gson().toJson(new ArrayList<>(checkedServers)));
    store.setValue(Constants.AUTO_APPROVE_MCP_TOOLS,
        new Gson().toJson(new ArrayList<>(checkedTools)));
  }

  /**
   * Updates the server/tool collections displayed in the tree viewer.
   * Called from the MCP config service when server data changes.
   */
  public void updateServerCollections(
      List<McpServerToolsCollection> collections) {
    if (isDisposed()) {
      return;
    }
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (isDisposed()) {
        return;
      }
      this.serverCollections = collections != null
          ? collections : Collections.emptyList();
      treeViewer.setInput(serverCollections);
      refreshTreeCheckState();
      requestLayout();
    }, this);
  }

  private void refreshTreeCheckState() {
    if (treeViewer.getTree().isDisposed()) {
      return;
    }
    for (McpServerToolsCollection server : serverCollections) {
      // Expand to ensure child TreeItems exist before setChecked
      treeViewer.expandToLevel(server, 1);

      String serverLower = server.getName() != null
          ? server.getName().toLowerCase(Locale.ROOT) : "";
      boolean serverChecked = checkedServers.contains(serverLower);

      List<McpToolInformation> tools = server.getTools();
      if (tools == null) {
        tools = Collections.emptyList();
      }

      if (serverChecked) {
        // Server is approved: check server and all its tools
        treeViewer.setChecked(server, true);
        treeViewer.setGrayed(server, false);
        for (McpToolInformation tool : tools) {
          treeViewer.setChecked(tool, true);
        }
      } else {
        // Check individual tools
        int checkedCount = 0;
        for (McpToolInformation tool : tools) {
          String toolKey = serverLower + "::"
              + (tool.getName() != null
                  ? tool.getName().toLowerCase(Locale.ROOT) : "");
          boolean toolChecked = checkedTools.contains(toolKey);
          treeViewer.setChecked(tool, toolChecked);
          if (toolChecked) {
            checkedCount++;
          }
        }
        // Update parent check/gray state
        if (checkedCount == 0) {
          treeViewer.setChecked(server, false);
          treeViewer.setGrayed(server, false);
        } else if (checkedCount == tools.size()) {
          treeViewer.setChecked(server, true);
          treeViewer.setGrayed(server, false);
        } else {
          treeViewer.setChecked(server, true);
          treeViewer.setGrayed(server, true);
        }
      }
    }
  }

  private static List<String> loadJsonList(IPreferenceStore store,
      String key) {
    String json = store.getString(key);
    if (StringUtils.isBlank(json) || "[]".equals(json.trim())) {
      return Collections.emptyList();
    }
    try {
      List<String> list = new Gson().fromJson(json, STRING_LIST_TYPE);
      return list != null ? list : Collections.emptyList();
    } catch (Exception e) {
      CopilotCore.LOGGER.error(
          "Failed to parse MCP auto-approve list: " + key, e);
      return Collections.emptyList();
    }
  }

  /** Content provider for the server/tool tree. */
  private static class McpTreeContentProvider
      implements ITreeContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
      if (inputElement instanceof List<?> list) {
        return list.toArray();
      }
      return new Object[0];
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof McpServerToolsCollection server) {
        List<McpToolInformation> tools = server.getTools();
        return tools != null ? tools.toArray() : new Object[0];
      }
      return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      if (element instanceof McpServerToolsCollection server) {
        List<McpToolInformation> tools = server.getTools();
        return tools != null && !tools.isEmpty();
      }
      return false;
    }
  }

  /** Label provider for the server/tool tree. */
  private static class McpTreeLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
      if (element instanceof McpServerToolsCollection server) {
        return server.getName() != null ? server.getName() : "";
      }
      if (element instanceof McpToolInformation tool) {
        return tool.getName() != null ? tool.getName() : "";
      }
      return "";
    }
  }

  /** Handles check state changes in the server/tool tree. */
  private class McpCheckStateListener implements ICheckStateListener {
    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {
      Object element = event.getElement();
      boolean checked = event.getChecked();

      if (element instanceof McpServerToolsCollection server) {
        handleServerCheckChanged(server, checked);
      } else if (element instanceof McpToolInformation tool) {
        handleToolCheckChanged(tool, checked);
      }
    }

    private void handleServerCheckChanged(
        McpServerToolsCollection server, boolean checked) {
      String serverLower = server.getName() != null
          ? server.getName().toLowerCase(Locale.ROOT) : "";
      treeViewer.setGrayed(server, false);

      if (checked) {
        checkedServers.add(serverLower);
      } else {
        checkedServers.remove(serverLower);
      }

      // Check/uncheck all children
      List<McpToolInformation> tools = server.getTools();
      if (tools != null) {
        for (McpToolInformation tool : tools) {
          treeViewer.setChecked(tool, checked);
          String toolKey = serverLower + "::"
              + (tool.getName() != null
                  ? tool.getName().toLowerCase(Locale.ROOT) : "");
          if (checked) {
            checkedTools.add(toolKey);
          } else {
            checkedTools.remove(toolKey);
          }
        }
      }
    }

    private void handleToolCheckChanged(
        McpToolInformation tool, boolean checked) {
      // Find parent server
      McpServerToolsCollection parentServer = findParentServer(tool);
      if (parentServer == null) {
        return;
      }
      String serverLower = parentServer.getName() != null
          ? parentServer.getName().toLowerCase(Locale.ROOT) : "";
      String toolKey = serverLower + "::"
          + (tool.getName() != null
              ? tool.getName().toLowerCase(Locale.ROOT) : "");

      if (checked) {
        checkedTools.add(toolKey);
      } else {
        checkedTools.remove(toolKey);
      }

      // Update parent check/gray state
      updateParentState(parentServer);
    }

    private void updateParentState(McpServerToolsCollection server) {
      String serverLower = server.getName() != null
          ? server.getName().toLowerCase(Locale.ROOT) : "";
      List<McpToolInformation> tools = server.getTools();
      if (tools == null || tools.isEmpty()) {
        return;
      }
      int checkedCount = 0;
      for (McpToolInformation tool : tools) {
        if (treeViewer.getChecked(tool)) {
          checkedCount++;
        }
      }
      if (checkedCount == 0) {
        treeViewer.setChecked(server, false);
        treeViewer.setGrayed(server, false);
        checkedServers.remove(serverLower);
      } else if (checkedCount == tools.size()) {
        treeViewer.setChecked(server, true);
        treeViewer.setGrayed(server, false);
        checkedServers.add(serverLower);
      } else {
        treeViewer.setChecked(server, true);
        treeViewer.setGrayed(server, true);
        checkedServers.remove(serverLower);
      }
    }

    private McpServerToolsCollection findParentServer(
        McpToolInformation tool) {
      for (McpServerToolsCollection server : serverCollections) {
        List<McpToolInformation> tools = server.getTools();
        if (tools != null && tools.contains(tool)) {
          return server;
        }
      }
      return null;
    }
  }
}

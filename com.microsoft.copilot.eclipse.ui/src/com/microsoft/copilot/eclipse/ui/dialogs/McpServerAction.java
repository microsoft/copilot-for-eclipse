package com.microsoft.copilot.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.mcp.Package;
import com.microsoft.copilot.eclipse.core.lsp.mcp.Remote;
import com.microsoft.copilot.eclipse.core.lsp.mcp.ServerDetail;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.dialogs.McpServerInstallManager.ButtonState;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handles the creation and management of action items in the MCP Registry dialog table. This class is responsible for
 * building toolbar buttons and managing their interactions.
 */
public class McpServerAction implements EventHandler {
  private final List<TableEditor> editors = new ArrayList<>();
  private final Map<String, ToolItem> serverInstallButtons = new HashMap<>();
  private final Map<String, ServerDetail> serverDetailsCache = new HashMap<>();

  // Dependencies
  private final Shell parentShell;
  private final McpServerInstallManager installManager;
  private final IEventBroker eventBroker;
  private IStylingEngine stylingEngine;
  private String mcpRegistryUrl;

  /**
   * Constructor for ActionItems.
   *
   * @param parentShell The parent shell for dialog interactions.
   */
  public McpServerAction(Shell parentShell, String mcpRegistryUrl) {
    this.parentShell = parentShell;
    this.installManager = new McpServerInstallManager();
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    this.mcpRegistryUrl = mcpRegistryUrl;

    if (this.eventBroker != null) {
      this.eventBroker.subscribe(CopilotEventConstants.TOPIC_MCP_SERVER_STATE_CHANGE, this);
    }
    this.stylingEngine = PlatformUI.getWorkbench().getService(IStylingEngine.class);
  }

  @Override
  public void handleEvent(Event event) {
    String serverName = (String) event.getProperty(McpServerInstallManager.EVENT_DATA_SERVER_NAME);
    McpServerInstallManager.ActionType actionType = (McpServerInstallManager.ActionType) event
        .getProperty(McpServerInstallManager.EVENT_DATA_ACTION_TYPE);
    McpServerInstallManager.ActionResult actionResult = (McpServerInstallManager.ActionResult) event
        .getProperty(McpServerInstallManager.EVENT_DATA_ACTION_RESULT);

    if (CopilotEventConstants.TOPIC_MCP_SERVER_STATE_CHANGE.equals(event.getTopic())) {
      handleServerStateChange(serverName, actionType, actionResult);
    }
  }

  private void handleServerStateChange(String serverName, McpServerInstallManager.ActionType actionType,
      McpServerInstallManager.ActionResult actionResult) {
    parentShell.getDisplay().asyncExec(() -> {
      // Determine button state based on action type + action result
      ButtonState buttonState = determineButtonState(actionType, actionResult);
      updateButtonState(serverName, buttonState);
    });
  }

  /**
   * Determines the button state based on action type and result. This decouples the event logic from direct button
   * state control.
   */
  private ButtonState determineButtonState(McpServerInstallManager.ActionType actionType,
      McpServerInstallManager.ActionResult actionResult) {
    if (McpServerInstallManager.ActionType.INSTALL == actionType) {
      switch (actionResult) {
        case IN_PROGRESS:
          return ButtonState.INSTALLING;
        case SUCCESS:
          return ButtonState.UNINSTALL;
        case FAILURE:
        default:
          return ButtonState.INSTALL;
      }
    } else if (McpServerInstallManager.ActionType.UNINSTALL == actionType) {
      switch (actionResult) {
        case IN_PROGRESS:
          return ButtonState.UNINSTALLING;
        case SUCCESS:
          return ButtonState.INSTALL;
        case FAILURE:
        default:
          return ButtonState.UNINSTALL;
      }
    }

    return ButtonState.INSTALL;
  }

  /**
   * Builds action editors for each server item in the provided table.
   *
   * @param table The table to populate with action editors.
   * @param items The list of server items to create actions for.
   */
  public void buildActionEditors(Table table, List<McpRegistryDialog.ServerItem> items) {
    for (int i = 0; i < table.getItemCount(); i++) {
      TableItem item = table.getItem(i);
      Composite actionCell = createActionEditor(table, item);
      // only add measurement listener once
      if (i == 0 && actionCell != null) {
        TableMeasurementListener listener = new TableMeasurementListener(actionCell);
        table.addListener(SWT.MeasureItem, listener);
        actionCell.addDisposeListener(e -> table.removeListener(SWT.MeasureItem, listener));
      }
    }
  }

  /**
   * Disposes all editors and clears associated resources.
   */
  public void disposeAllEditors() {
    for (TableEditor editor : editors) {
      if (editor != null) {
        Control ctrl = editor.getEditor();
        if (ctrl != null && !ctrl.isDisposed()) {
          ctrl.dispose();
        }
        editor.dispose();
      }
    }
    editors.clear();

    // Clear the button cache when disposing editors to prevent stale references
    serverInstallButtons.clear();
    serverDetailsCache.clear();
  }

  private void updateButtonState(String serverName, ButtonState state) {
    ToolItem button = serverInstallButtons.get(serverName);
    if (button != null && !button.isDisposed()) {
      switch (state) {
        case INSTALL:
        case UNINSTALL:
          Composite toolBar = button.getParent();
          button.dispose();
          createInstallUninstallButton((ToolBar) toolBar, serverDetailsCache.get(serverName));
          toolBar.requestLayout();
          break;
        case INSTALLING:
        case UNINSTALLING:
          button.setText(state.getText());
          button.setEnabled(false);
          break;
        default:
          break;
      }
    }
  }

  private boolean shouldSkipActionButtons(McpRegistryDialog.ServerItem serverItem) {
    return serverItem == null || isStatusRow(serverItem)
        || McpRegistryDialog.LOADING_SPINNER_ROW_NAME.equals(serverItem.name);
  }

  private Composite createActionEditor(Table table, TableItem item) {
    McpRegistryDialog.ServerItem serverItem = (McpRegistryDialog.ServerItem) item.getData();
    if (shouldSkipActionButtons(serverItem)) {
      return null;
    }
    ServerDetail serverDetail = serverItem.details;
    if (serverDetail == null) {
      return null;
    }

    Composite cell = createActionCell(table);
    createActionToolBar(cell, serverDetail);

    TableEditor editor = new TableEditor(table);
    editor.grabHorizontal = true;
    editor.horizontalAlignment = SWT.CENTER;
    editor.verticalAlignment = SWT.CENTER;
    editor.setEditor(cell, item, 2);
    editors.add(editor);
    return cell;
  }

  private Composite createActionCell(Table table) {
    Composite cell = new Composite(table, SWT.NONE);
    GridLayout gl = new GridLayout(1, false);
    cell.setLayout(gl);
    cell.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    if (PlatformUtils.isWindows()) {
      UiUtils.applyCssClass(cell, "mcp-registry-dialog-actions", stylingEngine);
    }
    return cell;
  }

  private void createActionToolBar(Composite parent, ServerDetail serverDetail) {
    ToolBar toolBar = new ToolBar(parent, SWT.FLAT);
    toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    createDetailsButton(toolBar, serverDetail);
    ToolItem separator = new ToolItem(toolBar, SWT.SEPARATOR);
    separator.setWidth(1);
    createInstallUninstallButton(toolBar, serverDetail);
  }

  private void createDetailsButton(ToolBar toolBar, ServerDetail serverDetail) {
    ToolItem detailsButton = new ToolItem(toolBar, SWT.PUSH);
    detailsButton.setText(Messages.mcpRegistryDialog_details);
    detailsButton.addListener(SWT.Selection, e -> onDetails(serverDetail));
    UiUtils.applyCssClass(detailsButton, "mcp-registry-dialog-actions", stylingEngine);
  }

  private void createInstallUninstallButton(ToolBar toolBar, ServerDetail serverDetail) {
    ToolItem installButton;

    // Determine initial button state based on whether server is installed
    String serverId = McpServerConfigurationBuilder.getServerId(serverDetail);
    ButtonState initialState = this.installManager.getInitialState(serverId, this.mcpRegistryUrl);
    boolean isInstalled = initialState == ButtonState.UNINSTALL;

    if (isInstalled) {
      // If installed, show uninstall button
      installButton = new ToolItem(toolBar, SWT.FLAT);
      installButton.addListener(SWT.Selection, e -> onUninstall(serverDetail));
    } else {
      // If not installed, show install dropdown
      installButton = new ToolItem(toolBar, SWT.DROP_DOWN);
      Menu installMenu = createInstallMenu(parentShell, installButton, serverDetail, this.mcpRegistryUrl);
      setupInstallButtonHandler(installButton, installMenu, serverDetail);
    }

    String serverName = serverDetail.getName();
    installButton.setText(initialState.getText());
    serverInstallButtons.put(serverName, installButton);
    serverDetailsCache.put(serverName, serverDetail);
    UiUtils.applyCssClass(installButton, "mcp-registry-dialog-actions", stylingEngine);
  }

  private Menu createInstallMenu(Shell shell, ToolItem installButton, ServerDetail serverDetail,
      String mcpProviderUrl) {
    Menu installMenu = new Menu(shell);

    // Cache lists locally and compute presence flags once
    List<Remote> remotes = serverDetail.getRemotes();
    List<Package> packages = serverDetail.getPackages();
    boolean hasRemotes = remotes != null && !remotes.isEmpty();
    boolean hasPackages = packages != null && !packages.isEmpty();

    // Only the very first option across all sections should be default (prefer remotes)
    boolean defaultAssigned = false;

    // Add remote server options first (typically preferred)
    if (hasRemotes) {
      for (int index = 0; index < remotes.size(); index++) {
        Remote remote = remotes.get(index);
        JsonObject config = McpServerConfigurationBuilder.createRemoteServerConfiguration(remote, serverDetail,
            mcpProviderUrl);
        boolean isDefault = !defaultAssigned && index == 0;
        if (isDefault) {
          installButton.addListener(SWT.Selection, e -> {
            if (e.detail != SWT.ARROW) {
              onInstall(serverDetail, config);
            }
          });
          defaultAssigned = true;
        }
        String typeSuffix = (remote.getTransportType() != null
            && StringUtils.isNotBlank(remote.getTransportType().toString())
                ? " (" + remote.getTransportType().toString() + ")"
                : "");
        createMenuItem(installMenu, "remote: " + remote.getUrl() + typeSuffix,
            "Connect to remote server at " + remote.getUrl() + typeSuffix, () -> onInstall(serverDetail, config));
      }
    }

    // Separator only when both sections exist
    if (hasRemotes && hasPackages) {
      new MenuItem(installMenu, SWT.SEPARATOR);
    }

    // Add package options
    if (hasPackages) {
      for (int index = 0; index < packages.size(); index++) {
        Package pkg = packages.get(index);
        JsonObject config = McpServerConfigurationBuilder.createPackageServerConfiguration(pkg, serverDetail,
            this.mcpRegistryUrl);
        boolean isDefault = !defaultAssigned && index == 0;
        if (isDefault) {
          installButton.addListener(SWT.Selection, e -> {
            if (e.detail != SWT.ARROW) {
              onInstall(serverDetail, config);
            }
          });
          defaultAssigned = true;
        }
        createMenuItem(installMenu, pkg.getRegistryType() + ": " + pkg.getIdentifier(),
            "Install " + pkg.getIdentifier() + " from " + pkg.getRegistryType(), () -> onInstall(serverDetail, config));
      }
    }

    return installMenu;
  }

  // Action handlers - now handle everything internally
  private void onDetails(ServerDetail serverDetail) {
    McpServerDetailDialog detailDialog = new McpServerDetailDialog(parentShell, serverDetail, installManager,
        this.mcpRegistryUrl);
    detailDialog.open();
  }

  private void onInstall(ServerDetail serverDetail, JsonObject config) {
    String serverName = serverDetail.getName();
    if (installManager != null) {
      installManager.installServer(serverName, config);
    }
  }

  private void onUninstall(ServerDetail serverDetail) {
    String serverName = serverDetail.getName();
    if (installManager != null) {
      installManager.uninstallServer(serverName);
    }
  }

  private void createMenuItem(Menu menu, String title, String tooltip, Runnable action) {
    MenuItem item = new MenuItem(menu, SWT.PUSH);
    item.setText(title);
    item.setToolTipText(tooltip);
    item.addListener(SWT.Selection, e -> action.run());
  }

  private void setupInstallButtonHandler(ToolItem installButton, Menu installMenu, ServerDetail serverDetail) {
    installButton.addListener(SWT.Selection, e -> {
      if (e.detail == SWT.ARROW) {
        showDropdownMenu(installButton, installMenu);
      }
    });
  }

  private void showDropdownMenu(ToolItem button, Menu menu) {
    Rectangle rect = button.getBounds();
    Point pt = new Point(rect.x, rect.y + rect.height);
    pt = button.getParent().toDisplay(pt);
    menu.setLocation(pt.x, pt.y);
    menu.setVisible(true);
  }

  private boolean isStatusRow(McpRegistryDialog.ServerItem serverItem) {
    // Heuristic: status rows use translated marker keys or have empty description.
    String n = serverItem.name != null ? serverItem.name : "";
    return n.equals(Messages.mcpRegistryDialog_loading) || n.equals(Messages.mcpRegistryDialog_errorLoading)
        || McpRegistryDialog.LOADING_SPINNER_ROW_NAME.equals(n);
  }

  /**
   * Cleanup method to unsubscribe from events when ActionItems is no longer needed.
   */
  public void dispose() {
    if (this.eventBroker != null) {
      this.eventBroker.unsubscribe(this);
    }
    disposeAllEditors();
  }

  /**
   * Listener implementation to measure table row height based on action cell size.
   */
  private final class TableMeasurementListener implements Listener {
    private final Composite actionCell;
    private int cachedHeight = -1;

    private TableMeasurementListener(Composite actionCell) {
      this.actionCell = actionCell;
    }

    @Override
    public void handleEvent(org.eclipse.swt.widgets.Event event) {
      if (actionCell == null || actionCell.isDisposed()) {
        return; // No action cell to measure
      }
      if (cachedHeight < 0) {
        // Cache the height after first measurement
        cachedHeight = actionCell.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
      }
      event.height = cachedHeight;
    }
  }
}

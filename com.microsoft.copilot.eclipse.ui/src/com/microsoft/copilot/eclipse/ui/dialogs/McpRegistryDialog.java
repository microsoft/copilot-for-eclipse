package com.microsoft.copilot.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.ListServersParams;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpRegistryAllowList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.RegistryAccess;
import com.microsoft.copilot.eclipse.core.lsp.mcp.ServerDetail;
import com.microsoft.copilot.eclipse.core.lsp.mcp.ServerList;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;
import com.microsoft.copilot.eclipse.ui.utils.McpUtils;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * MCP Registry dialog UI.
 */
public class McpRegistryDialog extends Dialog {
  public static final String LOADING_SPINNER_ROW_NAME = "__LOADING_SPINNER__";

  private static final int PAGE_SIZE = 30;
  private static final int SEARCH_DEBOUNCE_MS = 200;

  private String nextCursor = "";
  private boolean hasMoreData = true;
  private boolean isLoading = false;
  private List<ServerItem> allItems = new ArrayList<>();
  private String mcpRegistryUrl = "";
  private McpRegistryAllowList mcpAllowList = null;
  private int loadingSpinnerIndex = -1;

  private Text txtSearch;
  private TableViewer viewer;
  private CopilotLanguageServerConnection copilotLanguageServerConnection;
  private McpServerAction mcpServerAction;
  private Runnable pendingSearchRefresh;
  private SpinnerJob spinnerJob;
  private IPreferenceChangeListener preferenceChangeListener;

  /**
   * Create the MCP registry dialog.
   *
   */
  public McpRegistryDialog(Shell parentShell) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
    this.copilotLanguageServerConnection = CopilotCore.getPlugin() != null
        ? CopilotCore.getPlugin().getCopilotLanguageServer()
        : null;
    this.spinnerJob = new SpinnerJob();
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(Messages.mcpRegistryDialog_title);

    Image dialogIcon = UiUtils.buildImageFromPngPath("/icons/chat/mcp_registry.png");
    if (dialogIcon != null) {
      newShell.setImage(dialogIcon);
    }
    newShell.addDisposeListener(e -> {
      if (dialogIcon != null && !dialogIcon.isDisposed()) {
        dialogIcon.dispose();
      }
    });
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    container.setLayout(new GridLayout(1, false));

    // Add preference change listener to monitor MCP registry URL changes
    preferenceChangeListener = event -> {
      if (Constants.MCP_REGISTRY_URL.equals(event.getKey())) {
        // Dispose all children of the container
        if (container != null && !container.isDisposed()) {
          container.getDisplay().asyncExec(() -> {
            if (!container.isDisposed()) {
              // Dispose all children
              for (Control child : container.getChildren()) {
                child.dispose();
              }

              // Reset state and recreate content
              hasMoreData = true;
              isLoading = false;
              if (viewer != null) {
                viewer = null;
              }

              // Fetch MCP registry URL and create appropriate content
              fetchUrlAndRefreshContent(container);
            }
          });
        }
      }
    };

    // Register the listener with the preference store
    IEclipsePreferences configPrefs = ConfigurationScope.INSTANCE
        .getNode(CopilotUi.getPlugin().getBundle().getSymbolicName());
    configPrefs.addPreferenceChangeListener(preferenceChangeListener);

    // Wait for URL to be fetched before creating any content
    fetchUrlAndRefreshContent(container);

    return area;
  }

  /**
   * Fetch the MCP registry URL and refresh the content in the specified container.
   *
   * @param container the container to refresh after URL is fetched
   */
  private void fetchUrlAndRefreshContent(Composite container) {
    loadMcpRegistryAllowListAndUrl().thenRun(() -> {
      SwtUtils.getDisplay().asyncExec(() -> {
        if (!container.isDisposed()) {
          createContent(container);
          container.requestLayout();
        }
      });
    });
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, CANCEL, Messages.mcpRegistryDialog_close, true);
  }

  private void createSearchField(Composite parent) {
    txtSearch = new Text(parent, SWT.SEARCH | SWT.ICON_SEARCH | SWT.CANCEL);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
    gd.widthHint = getInitialSize().x / 3;
    txtSearch.setLayoutData(gd);
    txtSearch.setMessage(Messages.mcpRegistryDialog_searchPlaceholder);
    txtSearch.addModifyListener(e -> scheduleSearchRefresh());
  }

  private void scheduleSearchRefresh() {
    if (viewer == null || viewer.getControl().isDisposed()) {
      return;
    }
    // cancel any pending scheduled refresh
    if (pendingSearchRefresh != null) {
      viewer.getControl().getDisplay().timerExec(-1, pendingSearchRefresh);
    }
    pendingSearchRefresh = () -> {
      try {
        if (viewer != null && !viewer.getControl().isDisposed()) {
          refresh();
        }
      } finally {
        pendingSearchRefresh = null;
      }
    };
    viewer.getControl().getDisplay().timerExec(SEARCH_DEBOUNCE_MS, pendingSearchRefresh);
  }

  private void createHeaderButtons(Composite parent) {
    // Determine if we should show the edit button (not in registry_only mode)
    boolean shouldShowEditButton = !isRegistryOnlyMode();

    Composite buttonComposite = new Composite(parent, SWT.NONE);
    GridLayout buttonLayout = createZeroMarginGridLayout(shouldShowEditButton ? 2 : 1, false);
    buttonLayout.horizontalSpacing = 2;
    buttonComposite.setLayout(buttonLayout);
    buttonComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

    createRefreshButton(buttonComposite);

    if (shouldShowEditButton) {
      createChangeUrlButton(buttonComposite);
    }
  }

  private GridLayout createZeroMarginGridLayout(int numColumns, boolean makeColumnsEqualWidth) {
    GridLayout layout = new GridLayout(numColumns, makeColumnsEqualWidth);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    return layout;
  }

  /**
   * Check if the current registry access mode is registry_only.
   *
   * @return true if registry access is registry_only, false otherwise
   */
  private boolean isRegistryOnlyMode() {
    if (mcpAllowList == null || mcpAllowList.getMcpRegistries().isEmpty()) {
      return false;
    }
    return mcpAllowList.getMcpRegistries().get(0).getRegistryAccess() == RegistryAccess.registry_only;
  }

  private void createRefreshButton(Composite parent) {
    Button refreshButton = new Button(parent, SWT.PUSH);
    Image refreshIcon = UiUtils.buildImageFromPngPath("/icons/mcp/refresh.png");
    refreshButton.setImage(refreshIcon);
    refreshButton.setText(Messages.mcpRegistryDialog_button_refresh);
    refreshButton.setToolTipText(Messages.mcpRegistryDialog_button_refresh_tooltip);
    refreshButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    refreshButton.addListener(SWT.Selection, e -> handleRefreshBtnClick());
    refreshButton.addDisposeListener(e -> {
      if (refreshIcon != null && !refreshIcon.isDisposed()) {
        refreshIcon.dispose();
      }
    });
  }

  private void createChangeUrlButton(Composite parent) {
    Button editButton = new Button(parent, SWT.PUSH);
    Image editIcon = UiUtils.buildImageFromPngPath("/icons/edit_preferences.png");
    editButton.setImage(editIcon);
    editButton.setText(Messages.mcpRegistryDialog_button_changeUrl);
    editButton.setToolTipText(Messages.mcpRegistryDialog_button_changeUrl_tooltip);
    editButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    editButton.addListener(SWT.Selection,
        e -> PreferencesUtil
            .createPreferenceDialogOn(getShell(), McpPreferencePage.ID, PreferencesUtils.getAllPreferenceIds(), null)
            .open());
    editButton.addDisposeListener(e -> {
      if (editIcon != null && !editIcon.isDisposed()) {
        editIcon.dispose();
      }
    });
  }

  private void configureTable(Table table) {
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // Add scroll listener for lazy loading
    table.getVerticalBar().addListener(SWT.Selection, e -> loadMoreIfNearEnd());
    if (!PlatformUtils.isWindows()) {
      // MacOS uses different scrolling events
      table.addListener(SWT.MouseWheel, e -> loadMoreIfNearEnd());
    }
  }

  private void setupTableResize() {
    Table table = viewer.getTable();
    TableColumn nameColumn = table.getColumn(0);
    TableColumn descColumn = table.getColumn(1);
    TableColumn actionColumn = table.getColumn(2);

    table.addListener(SWT.Resize, e -> {
      int tableWidth = table.getClientArea().width;
      int nameWidth = nameColumn.getWidth();
      int actionWidth = actionColumn.getWidth();
      int descWidth = tableWidth - nameWidth - actionWidth;
      if (descWidth > 100) {
        descColumn.setWidth(descWidth);
      }
    });
  }

  /**
   * Check if we need to load more data based on scroll position.
   */
  private void loadMoreIfNearEnd() {
    if (!hasMoreData || isLoading) {
      return;
    }

    Table table = viewer.getTable();
    int topIndex = table.getTopIndex();
    int visibleRows = (table.getClientArea().height / table.getItemHeight()) + 1;
    int totalItems = table.getItemCount();
    int bottomVisibleIndex = Math.min(topIndex + visibleRows, totalItems - 1);

    // Trigger loading when near the end (within 10 items)
    if (totalItems - bottomVisibleIndex <= 10) {
      loadServers(txtSearch.getText().trim());
    }
  }

  private void handleRefreshBtnClick() {
    txtSearch.setText("");

    // Reset pagination state
    nextCursor = "";
    hasMoreData = true;
    isLoading = false;

    // Stop any running spinner
    stopSpinner();

    // Refresh the cached URL before checking
    loadMcpRegistryAllowListAndUrl().thenRun(() -> {
      getShell().getDisplay().asyncExec(() -> continueRefresh());
    });
  }

  private void continueRefresh() {
    if (StringUtils.isBlank(this.mcpRegistryUrl)) {
      return;
    } else if (viewer == null) {
      // URL state changed - recreate the dialog content
      recreateDialogContent();
      return;
    }

    // Clear existing items and refresh the table
    allItems.clear();
    refresh();

    // Load fresh data
    loadServers("");
  }

  /**
   * Recreate the dialog content based on URL availability. If URL is available, create table. If URL is empty, create
   * error label.
   */
  private void recreateDialogContent() {
    // Get the container (should be the first child of the dialog area)
    Composite dialogArea = (Composite) getDialogArea();
    if (dialogArea != null && dialogArea.getChildren().length > 0) {
      Composite container = (Composite) dialogArea.getChildren()[0];

      // Dispose existing content except header
      Control[] children = container.getChildren();
      for (int i = 1; i < children.length; i++) { // Keep header (index 0)
        children[i].dispose();
      }

      // Clear viewer reference if table is being disposed
      if (viewer != null) {
        viewer = null;
      }

      createContent(container);
      container.requestLayout();
    }
  }

  private void createContent(Composite parent) {
    if (StringUtils.isBlank(this.mcpRegistryUrl)) {
      createWelcomeView(parent);
    } else {
      createLoadingView(parent);
      loadServersThenInitializeTableWithHeader("");
    }
  }

  /**
   * Load servers from the registry.
   */
  private void loadServers(String searchText) {
    if (copilotLanguageServerConnection == null || !hasMoreData || isLoading) {
      // No connection available; leave placeholder.
      return;
    }

    isLoading = true;
    loadingSpinnerIndex = allItems.size();
    allItems.add(new ServerItem(LOADING_SPINNER_ROW_NAME, null));
    refresh();
    startSpinner();

    loadServersPage(nextCursor, searchText);
  }

  private void loadServersPage(String cursor, String searchText) {
    ListServersParams params = new ListServersParams(this.mcpRegistryUrl, PAGE_SIZE, cursor);
    CompletableFuture<ServerList> future = copilotLanguageServerConnection.listMcpServers(params);
    future.thenAccept(serverList -> {
      List<ServerItem> newItems = processServerList(serverList, searchText, true);
      showTableItems(newItems);
    });
  }

  private void showTableItems(List<ServerItem> newItems) {
    if (viewer != null && !viewer.getControl().isDisposed()) {
      viewer.getControl().getDisplay().asyncExec(() -> {
        if (!viewer.getControl().isDisposed()) {
          stopSpinner();
          removeSpinnerRow();

          if (!newItems.isEmpty()) {
            // For successful loads, add the new items
            allItems.addAll(newItems);
          }

          isLoading = false;
          refresh();
        }
      });
    }
  }

  private void removeSpinnerRow() {
    if (loadingSpinnerIndex >= 0 && loadingSpinnerIndex < allItems.size()
        && LOADING_SPINNER_ROW_NAME.equals(allItems.get(loadingSpinnerIndex).name)) {
      allItems.remove(loadingSpinnerIndex);
      loadingSpinnerIndex = -1;
    }
  }

  private void createWelcomeView(Composite parent) {
    // Create a centered composite for the button and label
    Composite centeredComposite = new Composite(parent, SWT.NONE);
    GridData centeredGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
    centeredComposite.setLayoutData(centeredGridData);
    centeredComposite.setLayout(new GridLayout(1, false));

    Button configButton = new Button(centeredComposite, SWT.PUSH);
    configButton.setText(Messages.mcpRegistryDialog_button_changeUrl);
    GridData btnGridData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
    configButton.setLayoutData(btnGridData);
    configButton.addListener(SWT.Selection,
        e -> PreferencesUtil
            .createPreferenceDialogOn(getShell(), McpPreferencePage.ID, PreferencesUtils.getAllPreferenceIds(), null)
            .open());

    Label emptyUrlLabel = new Label(centeredComposite, SWT.WRAP);
    emptyUrlLabel.setText(Messages.mcpRegistryDialog_error_empty_url);
    GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
    emptyUrlLabel.setLayoutData(gd);
  }

  private void createLoadingView(Composite parent) {
    createSingleLabelView(parent, Messages.mcpRegistryDialog_loading_label);
  }

  private void createErrorView(Composite parent, String errorMessage) {
    createSingleLabelView(parent, errorMessage);
  }

  private void createSingleLabelView(Composite parent, String labelText) {
    // Create a centered composite for the button and label
    Composite centeredComposite = new Composite(parent, SWT.NONE);
    GridData centeredGridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
    centeredComposite.setLayoutData(centeredGridData);
    centeredComposite.setLayout(new GridLayout(1, false));

    Label emptyUrlLabel = new Label(centeredComposite, SWT.WRAP);
    emptyUrlLabel.setText(labelText);
    GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
    emptyUrlLabel.setLayoutData(gd);
  }

  /**
   * Load servers and show the header/table only after loading is complete.
   */
  private void loadServersThenInitializeTableWithHeader(String searchText) {
    if (copilotLanguageServerConnection == null) {
      return;
    }

    isLoading = true;
    ListServersParams params = new ListServersParams(this.mcpRegistryUrl, PAGE_SIZE, "");
    copilotLanguageServerConnection.listMcpServers(params).thenAccept(serverList -> {
      initializeTableWithHeader(serverList, searchText);
    });
  }

  /**
   * Handle the server list response and show the UI with header and table.
   */
  private void initializeTableWithHeader(ServerList serverList, String searchText) {
    SwtUtils.getDisplay().asyncExec(() -> {
      // Get the container (should be the first child of the dialog area)
      Composite dialogArea = (Composite) getDialogArea();
      if (dialogArea == null || dialogArea.getChildren().length == 0) {
        return;
      }

      Composite container = (Composite) dialogArea.getChildren()[0];
      if (container.isDisposed()) {
        return;
      }

      // Dispose the loading view
      for (Control child : container.getChildren()) {
        child.dispose();
      }

      // Create header and table
      createHeader(container);
      createTable(container);
      container.requestLayout();

      // Process the server list data
      processServerList(serverList, searchText, false);

      isLoading = false;
      refresh();
    });
  }

  private void createHeader(Composite parent) {
    Composite header = new Composite(parent, SWT.NONE);
    header.setLayout(createZeroMarginGridLayout(2, false));
    header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    createSearchField(header);
    createHeaderButtons(header);
  }

  private void createTable(Composite parent) {
    viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
    Table table = viewer.getTable();
    configureTable(table);
    createTableColumns();
    setupTableResize();

    viewer.setContentProvider(ArrayContentProvider.getInstance());
    viewer.addFilter(new SearchFilter());

    allItems.clear();
    viewer.setInput(allItems);
  }

  private void createTableColumns() {
    createNameColumn();
    createDescriptionColumn();
    createActionsColumn();
  }

  private void createNameColumn() {
    TableViewerColumn colName = new TableViewerColumn(viewer, SWT.LEFT);
    TableColumn nameColumn = colName.getColumn();
    nameColumn.setText(Messages.mcpRegistryDialog_col_name);
    nameColumn.setWidth(200);
    colName.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        ServerItem item = (ServerItem) element;
        return LOADING_SPINNER_ROW_NAME.equals(item.name) ? Messages.mcpRegistryDialog_loading : item.name;
      }
    });
  }

  private void createDescriptionColumn() {
    TableViewerColumn colDesc = new TableViewerColumn(viewer, SWT.LEFT);
    TableColumn descColumn = colDesc.getColumn();
    descColumn.setText(Messages.mcpRegistryDialog_col_desc);
    descColumn.setWidth(380);
    colDesc.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        ServerItem item = (ServerItem) element;
        return LOADING_SPINNER_ROW_NAME.equals(item.name) ? ""
            : item.details != null ? item.details.getDescription() : "";
      }
    });
  }

  private void createActionsColumn() {
    TableViewerColumn colActions = new TableViewerColumn(viewer, SWT.LEFT);
    TableColumn actionColumn = colActions.getColumn();
    actionColumn.setText(Messages.mcpRegistryDialog_col_actions);
    actionColumn.setWidth(150);
    colActions.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return "";
      }
    });
  }

  /**
   * Process server list and add items to the table.
   *
   * @param serverList the server list response
   * @param searchText the search filter text
   * @param append true to append items, false to replace with error on null
   * @return the list of new items added
   */
  private List<ServerItem> processServerList(ServerList serverList, String searchText, boolean append) {
    List<ServerItem> newItems = new ArrayList<>();
    if (serverList == null) {
      disposeTableAndShowErrorView();
      allItems.clear();
      hasMoreData = false;
    } else {
      List<ServerDetail> servers = serverList.getServers();
      if (servers != null) {
        for (ServerDetail details : servers) {
          if (details != null) {
            // Filter based on search text before adding
            if (StringUtils.isBlank(searchText) || StringUtils.containsIgnoreCase(details.getName(), searchText)
                || StringUtils.containsIgnoreCase(details.getDescription(), searchText)) {
              // Only add latest official servers
              if (details.getMeta() != null && details.getMeta().getOfficial() != null
                  && details.getMeta().getOfficial().isLatest()) {
                ServerItem item = new ServerItem(details.getName(), details);
                if (append) {
                  newItems.add(item);
                } else {
                  allItems.add(item);
                }
              }
            }
          }
        }
      }
      updatePaginationState(serverList);
    }

    return newItems;
  }

  private void disposeTableAndShowErrorView() {
    SwtUtils.getDisplay().asyncExec(() -> {
      if (viewer != null && !viewer.getControl().isDisposed()) {
        Composite dialogArea = (Composite) getDialogArea();
        if (dialogArea != null && dialogArea.getChildren().length > 0) {
          Composite container = (Composite) dialogArea.getChildren()[0];
          if (!container.isDisposed()) {
            // Dispose only the table and keep action header
            Control[] children = container.getChildren();
            for (int i = 1; i < children.length; i++) {
              children[i].dispose();
            }
            viewer = null;

            createErrorView(container, Messages.mcpRegistryDialog_errorLoading);
            container.requestLayout();
          }
        }
      }
    });
  }

  private void updatePaginationState(ServerList serverList) {
    if (serverList != null && serverList.getMetadata() != null) {
      nextCursor = serverList.getMetadata().getNextCursor();
      hasMoreData = StringUtils.isNotEmpty(nextCursor);
    } else {
      hasMoreData = false;
    }
  }

  /**
   * Load the MCP registry allowlist and URL, and set them to this.mcpAllowList & this.mcpRegistryUrl.
   */
  private CompletableFuture<Void> loadMcpRegistryAllowListAndUrl() {
    return McpUtils.getMcpAllowList(copilotLanguageServerConnection).thenAccept(allowList -> {
      this.mcpAllowList = allowList;
      this.mcpRegistryUrl = McpUtils.parseMcpRegistryUrlFromAllowList(allowList);
      // Initialize mcpServerAction now that mcpAllowList is resolved
      if (this.mcpServerAction == null) {
        this.mcpServerAction = new McpServerAction(getShell(), this.mcpRegistryUrl);
      }
    });
  }

  private void startSpinner() {
    if (spinnerJob != null && !spinnerJob.isCancelled()) {
      spinnerJob.start();
    }
  }

  private void stopSpinner() {
    if (spinnerJob != null) {
      spinnerJob.stop();
    }
  }

  private void refresh() {
    if (mcpServerAction != null) {
      mcpServerAction.disposeAllEditors();
    }
    viewer.refresh();
    if (mcpServerAction != null) {
      mcpServerAction.buildActionEditors(viewer.getTable(), allItems);
    }

    // Check if we need to continue loading more data when filtered results don't fill the table
    loadMoreIfNearEnd();
  }

  @Override
  public Shell getShell() {
    return super.getShell();
  }

  @Override
  protected Point getInitialSize() {
    return new Point(880, 520);
  }

  @Override
  public boolean close() {
    // Remove preference change listener
    if (preferenceChangeListener != null) {
      IEclipsePreferences configPrefs = ConfigurationScope.INSTANCE
          .getNode(CopilotUi.getPlugin().getBundle().getSymbolicName());
      configPrefs.removePreferenceChangeListener(preferenceChangeListener);
      preferenceChangeListener = null;
    }

    // Dispose mcpServerAction to clean up event subscriptions
    if (mcpServerAction != null) {
      mcpServerAction.dispose();
    }

    // Stop spinner and dispose cached images when dialog closes
    if (spinnerJob != null) {
      spinnerJob.cancel();
      spinnerJob.dispose();
      spinnerJob = null;
    }
    return super.close();
  }

  private class SearchFilter extends ViewerFilter {
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      // Always show status rows (including loading spinner) regardless of search text
      if (element instanceof ServerItem) {
        ServerItem si = (ServerItem) element;
        if (isStatusRow(si)) {
          return true;
        }
      }

      String query = txtSearch == null ? null : txtSearch.getText();
      if (StringUtils.isBlank(query)) {
        return true;
      }

      ServerItem serverItem = (ServerItem) element;
      return StringUtils.contains(serverItem.name, query)
          || serverItem.details != null && StringUtils.containsIgnoreCase(serverItem.details.getDescription(), query);
    }

    private boolean isStatusRow(ServerItem serverItem) {
      // Heuristic: status rows use translated marker keys or have empty description.
      String n = serverItem.name != null ? serverItem.name : "";
      return n.equals(Messages.mcpRegistryDialog_loading) || n.equals(Messages.mcpRegistryDialog_errorLoading)
          || LOADING_SPINNER_ROW_NAME.equals(n);
    }
  }

  /**
   * Data model for each server item in the table.
   */
  public static class ServerItem {
    final String name;
    final ServerDetail details;

    ServerItem(String name, ServerDetail details) {
      this.name = Objects.toString(name, "");
      this.details = details;
    }
  }

  /**
   * Spinner job for loading indication with cached spinner icons.
   */
  private class SpinnerJob extends Job {
    private static final int INITIAL_ICON_INDEX = 1;
    private static final int TOTAL_SPINNER_ICONS = 8;
    private static final long COMPLETION_IN_PROGRESS_SPINNER_ROTATE_RATE_MILLIS = 100L;

    private int currentIconIndex = INITIAL_ICON_INDEX;
    private final Image[] cachedSpinnerIcons = new Image[TOTAL_SPINNER_ICONS];
    private boolean isRunning = false;
    private boolean isCancelled = false;

    public SpinnerJob() {
      super("MCP Registry Spinner Job");
      this.setSystem(true);
    }

    public void start() {
      if (!isRunning && !isCancelled) {
        isRunning = true;
        currentIconIndex = INITIAL_ICON_INDEX;
        schedule();
      }
    }

    public void stop() {
      isRunning = false;
    }

    public boolean isCancelled() {
      return isCancelled;
    }

    public void dispose() {
      isCancelled = true;
      cancel();
      // Dispose all cached spinner icons
      for (Image icon : cachedSpinnerIcons) {
        if (icon != null && !icon.isDisposed()) {
          icon.dispose();
        }
      }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        if (!isRunning || isCancelled || viewer == null || viewer.getControl().isDisposed()) {
          return Status.CANCEL_STATUS;
        }

        viewer.getControl().getDisplay().asyncExec(() -> {
          if (viewer.getControl().isDisposed() || !isRunning) {
            return;
          }

          // Update spinner icon for loading row
          Table table = viewer.getTable();
          for (int i = 0; i < table.getItemCount(); i++) {
            TableItem item = table.getItem(i);
            ServerItem serverItem = (ServerItem) item.getData();
            if (LOADING_SPINNER_ROW_NAME.equals(serverItem.name)) {
              // Rotate spinner icon
              int iconIndex = (currentIconIndex - 1) % TOTAL_SPINNER_ICONS;
              if (cachedSpinnerIcons[iconIndex] == null) {
                cachedSpinnerIcons[iconIndex] = UiUtils
                    .buildImageFromPngPath(String.format("/icons/spinner/%d.png", iconIndex + 1));
              }
              Image spinnerIcon = cachedSpinnerIcons[iconIndex];
              item.setImage(spinnerIcon);
            }
          }

          currentIconIndex++;
          if (currentIconIndex > TOTAL_SPINNER_ICONS) {
            currentIconIndex = INITIAL_ICON_INDEX;
          }

          // Repeat until stopped
          schedule(COMPLETION_IN_PROGRESS_SPINNER_ROTATE_RATE_MILLIS);
        });
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Error in MCP Registry spinner job", e);
      }
      return Status.OK_STATUS;
    }
  }
}

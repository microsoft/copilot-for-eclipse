package com.microsoft.copilot.eclipse.ui.dialogs.mcp;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerResponse;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * MCP Registry dialog UI. This is a View component in the MVC pattern that only handles UI rendering and delegates
 * business logic to {@link McpRegistryController}.
 */
public class McpRegistryDialog extends Dialog {
  private static final int MIN_WIDTH = 525;
  private static final int SEARCH_DEBOUNCE_MS = 200;

  // UI components
  private Text txtSearch;
  private ScrolledComposite scrolledComposite;
  private Composite itemsContainer;
  private Composite contentContainer;
  private Composite progressBarContainer;
  private Font serverListLabelFont;
  private Color serverListLabelDarkBackground;
  private Color serverListLabelLightGradientStart;
  private Color serverListLabelLightGradientEnd;

  // Controller
  private final McpRegistryController controller;

  // Preference listener
  private IPreferenceChangeListener preferenceChangeListener;

  // UI state
  private boolean isLoading = false;
  private Runnable pendingSearchRefresh;
  private Runnable pendingMinSizeRefresh;

  /**
   * Create the MCP registry dialog.
   */
  public McpRegistryDialog(Shell parentShell) {
    super(parentShell);
    setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
    this.controller = new McpRegistryController();
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(Messages.mcpRegistryDialog_mcpRegistry);
    newShell.setMinimumSize(MIN_WIDTH, SWT.DEFAULT);

    Image dialogIcon = UiUtils.buildImageFromPngPath("/icons/mcp/mcp_registry.png");
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
    GridLayout areaLayout = (GridLayout) area.getLayout();
    areaLayout.marginWidth = 0;
    areaLayout.marginHeight = 0;

    contentContainer = new Composite(area, SWT.NONE);
    contentContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout pageLayout = new GridLayout(1, false);
    pageLayout.marginWidth = 0;
    pageLayout.marginHeight = 0;
    contentContainer.setLayout(pageLayout);

    // Create progress bar container (initially hidden)
    progressBarContainer = new Composite(area, SWT.NONE);
    GridData progressBarGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    progressBarGridData.exclude = true;
    progressBarContainer.setLayoutData(progressBarGridData);
    GridLayout progressBarLayout = new GridLayout(1, false);
    progressBarLayout.marginWidth = 10;
    progressBarContainer.setLayout(progressBarLayout);
    progressBarContainer.setVisible(false);

    // Register preference listener for MCP registry URL changes
    preferenceChangeListener = event -> {
      if (Constants.MCP_REGISTRY_URL.equals(event.getKey())) {
        onContentNeedsRebuild();
      }
    };
    IEclipsePreferences configPrefs = ConfigurationScope.INSTANCE
        .getNode(CopilotUi.getPlugin().getBundle().getSymbolicName());
    configPrefs.addPreferenceChangeListener(preferenceChangeListener);

    // Initial load: fetch URL and create content
    fetchUrlAndRefreshContent();

    return area;
  }

  /**
   * Reset dialog content and rebuild it based on the current MCP registry URL.
   */
  private void resetAndRebuildContent() {
    if (contentContainer == null || contentContainer.isDisposed()) {
      return;
    }

    for (Control child : contentContainer.getChildren()) {
      child.dispose();
    }

    controller.resetState();
    scrolledComposite = null;
    itemsContainer = null;

    fetchUrlAndRefreshContent();
    contentContainer.requestLayout();
  }

  /**
   * Fetch the MCP registry URL and refresh the content.
   */
  private void fetchUrlAndRefreshContent() {
    controller.loadMcpRegistryAllowListAndUrl().thenRun(() -> {
      SwtUtils.getDisplay().asyncExec(() -> {
        if (contentContainer != null && !contentContainer.isDisposed()) {
          createContent(contentContainer);
          contentContainer.requestLayout();
        }
      });
    });
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, CANCEL, Messages.mcpRegistryDialog_close, true);
  }

  private GridLayout createZeroMarginGridLayout(int numColumns, boolean makeColumnsEqualWidth) {
    GridLayout layout = new GridLayout(numColumns, makeColumnsEqualWidth);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    return layout;
  }

  /**
   * Check if we need to load more data based on scroll position.
   */
  private void loadMoreIfNearEnd() {
    if (!controller.hasMoreData() || isLoading || scrolledComposite == null || scrolledComposite.isDisposed()) {
      return;
    }

    // Get scroll position
    ScrollBar verticalBar = scrolledComposite.getVerticalBar();
    if (verticalBar == null) {
      return;
    }

    int selection = verticalBar.getSelection();
    int maximum = verticalBar.getMaximum();
    int thumb = verticalBar.getThumb();
    int scrollableHeight = maximum - thumb;

    // Trigger loading when near the end (within 20% of total scrollable height)
    if (scrollableHeight > 0 && selection >= scrollableHeight * 0.8) {
      loadServers(txtSearch.getText().trim());
    }
  }

  /**
   * Refresh the server list by resetting pagination state and reloading servers.
   */
  private void refreshServerList() {
    controller.resetState();
    clearItems();
    loadServers(txtSearch.getText().trim());
  }

  private void createContent(Composite parent) {
    if (StringUtils.isBlank(controller.getMcpRegistryBaseUrl())) {
      createWelcomeView(parent);
    } else {
      createMcpServerListView(parent);
    }
  }

  private void onContentNeedsRebuild() {
    if (contentContainer != null && !contentContainer.isDisposed()) {
      contentContainer.getDisplay().asyncExec(() -> resetAndRebuildContent());
    }
  }

  private void onLoadingStarted() {
    isLoading = true;
    SwtUtils.getDisplay().asyncExec(() -> {
      showProgressBar();
      // Show a loading indicator in the list
      if (itemsContainer != null && !itemsContainer.isDisposed()) {
        createLoadingItem(itemsContainer);
        itemsContainer.requestLayout();
        updateScrolledListMinSize();
      }
    });
  }

  private void onLoadingCompleted() {
    isLoading = false;
    if (scrolledComposite != null && !scrolledComposite.isDisposed()) {
      scrolledComposite.getDisplay().asyncExec(() -> {
        if (!scrolledComposite.isDisposed()) {
          hideProgressBar();
          loadMoreIfNearEnd();
        }
      });
    }
  }

  private void loadServers(String searchText) {
    onLoadingStarted();
    controller.loadServers(searchText).thenAccept(serverList -> {
      SwtUtils.getDisplay().asyncExec(() -> {
        if (itemsContainer == null || itemsContainer.isDisposed()) {
          return;
        }
        appendServers(serverList);
      });
    }).thenRun(this::onLoadingCompleted).exceptionally(ex -> {
      Throwable cause = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
      if (cause instanceof CancellationException) {
        // Expected: search/refresh cancels an in-flight request.
        onLoadingCompleted();
        return null;
      }

      String message = cause != null && cause.getMessage() != null
          ? NLS.bind(Messages.mcpRegistryDialog_errorLoading_prefix, cause.getMessage())
          : Messages.mcpRegistryDialog_errorLoading_default;
      onLoadingError(message);
      return null;
    });
  }

  private void appendServers(ServerList serverList) {
    if (itemsContainer == null || itemsContainer.isDisposed()) {
      return;
    }

    disposeTrailingPlaceholderIfPresent();

    List<ServerResponse> servers = serverList != null ? serverList.servers() : null;
    if (servers == null || servers.isEmpty()) {
      return;
    }

    itemsContainer.setRedraw(false);
    try {
      for (ServerResponse serverResponse : servers) {
        if (serverResponse == null) {
          continue;
        }

        McpServerItem widget = new McpServerItem(itemsContainer, SWT.NONE, serverResponse, controller);
        widget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      }
    } finally {
      itemsContainer.setRedraw(true);
    }

    itemsContainer.requestLayout();
    updateScrolledListMinSize();
  }

  private void disposeTrailingPlaceholderIfPresent() {
    if (itemsContainer == null || itemsContainer.isDisposed()) {
      return;
    }

    Control[] children = itemsContainer.getChildren();
    // Child 0 is the header label.
    if (children.length <= 1) {
      return;
    }

    // Dispose any trailing placeholder/separator controls until we reach the last real server item.
    // This is more robust than relying on a specific placeholder marker being the last child.
    for (int i = children.length - 1; i >= 1; i--) {
      Control child = children[i];
      if (child instanceof McpServerItem) {
        break;
      }
      child.dispose();
    }
  }

  private void onLoadingError(String errorMessage) {
    isLoading = false;
    disposeScrolledListAndShowErrorView(errorMessage);
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

  private void createMcpServerListView(Composite parent) {
    // Create header and scrolled list immediately
    createBanner(parent);
    createHeaderContainer(parent);
    createScrolledList(parent);
    parent.requestLayout();

    // Add loading item and start loading servers
    loadServers("");
  }

  /**
   * Create a header container with search field and buttons.
   */
  private void createHeaderContainer(Composite parent) {
    // Create an inner composite with horizontal margins for the header and search controls
    Composite headerContainer = new Composite(parent, SWT.NONE);
    GridLayout headerContainerLayout = new GridLayout(1, false);
    headerContainerLayout.marginWidth = 10; // add left/right margin
    headerContainerLayout.marginHeight = 0;
    headerContainer.setLayout(headerContainerLayout);
    headerContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Create search and button header inside the container
    Composite header = new Composite(headerContainer, SWT.NONE);
    header.setLayout(createZeroMarginGridLayout(2, false));
    header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    createSearchField(header);
    createHeaderButtons(header);
  }

  private void createSearchField(Composite parent) {
    txtSearch = new Text(parent, SWT.SEARCH | SWT.ICON_SEARCH | SWT.CANCEL);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd.minimumWidth = getInitialSize().x / 5;
    txtSearch.setLayoutData(gd);
    txtSearch.setMessage(Messages.mcpRegistryDialog_searchPlaceholder);
    txtSearch.addModifyListener(e -> scheduleSearchRefresh());
  }

  private void scheduleSearchRefresh() {
    if (scrolledComposite == null || scrolledComposite.isDisposed()) {
      return;
    }
    // cancel any pending scheduled refresh
    if (pendingSearchRefresh != null) {
      scrolledComposite.getDisplay().timerExec(-1, pendingSearchRefresh);
    }
    pendingSearchRefresh = () -> {
      try {
        refreshServerList();
      } finally {
        pendingSearchRefresh = null;
      }
    };
    scrolledComposite.getDisplay().timerExec(SEARCH_DEBOUNCE_MS, pendingSearchRefresh);
  }

  private void createHeaderButtons(Composite parent) {
    // Determine if we should show the edit button (not in registry_only mode)
    boolean shouldShowEditButton = !controller.isRegistryOnlyMode();

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

  private void createRefreshButton(Composite parent) {
    Button refreshButton = new Button(parent, SWT.PUSH);
    Image refreshIcon = UiUtils.buildImageFromPngPath("/icons/mcp/refresh.png");
    refreshButton.setImage(refreshIcon);
    refreshButton.setText(Messages.mcpRegistryDialog_button_refresh);
    refreshButton.setToolTipText(Messages.mcpRegistryDialog_button_refresh_tooltip);
    refreshButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    refreshButton.addListener(SWT.Selection, e -> refreshServerList());
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

  /**
   * Create a banner similar to Eclipse Marketplace at the top of the dialog.
   */
  private void createBanner(Composite parent) {
    Composite banner = new Composite(parent, SWT.NONE);
    banner.setData(CssConstants.CSS_CLASS_NAME_KEY, "mcp-server-item");
    banner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    // Add internal left/right margins for banner content; avoid extra bottom margin so
    // the separator sits flush directly under the banner.
    GridLayout bannerLayout = new GridLayout(2, false);
    bannerLayout.marginTop = 0;
    bannerLayout.marginBottom = 0;
    bannerLayout.marginLeft = 0;
    bannerLayout.marginRight = 0;
    banner.setLayout(bannerLayout);

    // Draw a bottom border for the banner
    banner.addListener(SWT.Paint, e -> {
      Rectangle b = banner.getBounds();
      GC gc = e.gc;
      Color borderColor = banner.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
      gc.setForeground(borderColor);
      gc.drawLine(0, b.height - 1, b.width, b.height - 1);
    });

    // Create text composite on the left
    Composite textComposite = new Composite(banner, SWT.NONE);
    textComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout textLayout = new GridLayout(1, false);
    textLayout.marginWidth = 0;
    textLayout.marginHeight = 0;
    textComposite.setLayout(textLayout);

    // Title label
    Label titleLabel = new Label(textComposite, SWT.NONE);
    titleLabel.setText(Messages.mcpRegistryDialog_mcpRegistry);
    FontDescriptor boldDescriptor = FontDescriptor.createFrom(titleLabel.getFont()).setStyle(SWT.BOLD)
        .increaseHeight(2);
    Font boldFont = boldDescriptor.createFont(parent.getDisplay());
    titleLabel.setFont(boldFont);
    titleLabel.addDisposeListener(e -> {
      if (!boldFont.isDisposed()) {
        boldFont.dispose();
      }
    });
    titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // Description label
    Label descLabel = new Label(textComposite, SWT.WRAP);
    descLabel.setText(Messages.mcpRegistryDialog_banner_description);
    GridData descGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    descGridData.widthHint = SWT.DEFAULT;
    descLabel.setLayoutData(descGridData);

    // Create icon label on the right
    Label iconLabel = new Label(banner, SWT.NONE);
    Image bannerIcon = UiUtils.buildImageFromPngPath("/icons/mcp/mcp_marketplace_icon.png");
    if (bannerIcon != null) {
      iconLabel.setImage(bannerIcon);
      iconLabel.addDisposeListener(e -> {
        if (!bannerIcon.isDisposed()) {
          bannerIcon.dispose();
        }
      });
    }
    iconLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
  }

  private void createScrolledList(Composite parent) {
    Composite scrolledContainer = new Composite(parent, SWT.NONE);
    GridLayout scrolledContainerLayout = new GridLayout(1, false);
    scrolledContainerLayout.marginWidth = 10; // add left/right margin
    scrolledContainerLayout.marginHeight = 0;
    scrolledContainer.setLayout(scrolledContainerLayout);
    scrolledContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    scrolledComposite = new ScrolledComposite(scrolledContainer, SWT.V_SCROLL | SWT.BORDER);
    scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    scrolledComposite.setExpandHorizontal(true);
    scrolledComposite.setExpandVertical(true);

    itemsContainer = new Composite(scrolledComposite, SWT.NONE);
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.verticalSpacing = 0;
    layout.marginTop = 0;
    layout.marginBottom = 0;
    layout.marginLeft = 0;
    layout.marginRight = 0;
    itemsContainer.setLayout(layout);
    itemsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    // Add server list label with gradient background
    Label serverListLabel = new Label(itemsContainer, SWT.NONE);
    serverListLabel.setText(Messages.mcpRegistryDialog_mcpRegistry);
    GridData labelData = new GridData(SWT.FILL, SWT.FILL, true, false);
    labelData.horizontalIndent = 0;
    labelData.verticalIndent = 0;
    labelData.heightHint = 40; // Set a fixed height for the gradient to match native marketplace style
    serverListLabel.setLayoutData(labelData);

    // Create cached resources for paint listener (avoid allocation on every paint event)
    Font defaultFont = serverListLabel.getDisplay().getSystemFont();
    FontData[] fontData = defaultFont.getFontData();
    for (FontData fd : fontData) {
      fd.setStyle(SWT.BOLD);
      fd.setHeight(12);
    }
    serverListLabelFont = new Font(serverListLabel.getDisplay(), fontData);

    // Cache colors used by the paint listener and dispose them with the label.
    // (Avoid creating new Color instances on every SWT.Paint event.)
    serverListLabelDarkBackground = new Color(serverListLabel.getDisplay(), 72, 72, 76);
    serverListLabelLightGradientStart = new Color(serverListLabel.getDisplay(), 255, 255, 255);
    serverListLabelLightGradientEnd = new Color(serverListLabel.getDisplay(), 232, 238, 245);

    // Dispose cached resources when label is disposed
    serverListLabel.addDisposeListener(e -> {
      if (serverListLabelFont != null && !serverListLabelFont.isDisposed()) {
        serverListLabelFont.dispose();
        serverListLabelFont = null;
      }
      if (serverListLabelDarkBackground != null && !serverListLabelDarkBackground.isDisposed()) {
        serverListLabelDarkBackground.dispose();
        serverListLabelDarkBackground = null;
      }
      if (serverListLabelLightGradientStart != null && !serverListLabelLightGradientStart.isDisposed()) {
        serverListLabelLightGradientStart.dispose();
        serverListLabelLightGradientStart = null;
      }
      if (serverListLabelLightGradientEnd != null && !serverListLabelLightGradientEnd.isDisposed()) {
        serverListLabelLightGradientEnd.dispose();
        serverListLabelLightGradientEnd = null;
      }
    });

    // Add paint listener to draw background (gradient for light mode, solid for dark mode)
    serverListLabel.addListener(SWT.Paint, event -> {
      Rectangle bounds = serverListLabel.getBounds();
      GC gc = event.gc;
      if (UiUtils.isDarkTheme()) {
        // Draw solid background for dark mode
        gc.setBackground(serverListLabelDarkBackground);
        gc.fillRectangle(0, 0, bounds.width, bounds.height);

        // Draw text using cached font with white color for dark mode
        gc.setFont(serverListLabelFont);
        gc.setForeground(serverListLabel.getDisplay().getSystemColor(SWT.COLOR_WHITE));
      } else {
        // Draw gradient using cached colors for light mode
        gc.setForeground(serverListLabelLightGradientStart);
        gc.setBackground(serverListLabelLightGradientEnd);
        gc.fillGradientRectangle(0, 0, bounds.width, bounds.height, true);

        // Draw text using cached font with black color for light mode
        gc.setFont(serverListLabelFont);
        gc.setForeground(serverListLabel.getDisplay().getSystemColor(SWT.COLOR_BLACK));
      }

      String text = serverListLabel.getText();
      Point textSize = gc.textExtent(text);
      gc.drawText(text, 5, (bounds.height - textSize.y) / 2, true);
    });

    scrolledComposite.setContent(itemsContainer);
    updateScrolledListMinSize();

    // Keep minSize in sync with viewport width so wrapping/height recomputes correctly on dialog resize.
    scrolledComposite.addListener(SWT.Resize, e -> scheduleScrolledListMinSizeRefresh());

    // Add scroll listener for lazy loading
    scrolledComposite.getVerticalBar().addListener(SWT.Selection, e -> loadMoreIfNearEnd());
    if (!PlatformUtils.isWindows()) {
      // MacOS uses different scrolling events
      scrolledComposite.addListener(SWT.MouseWheel, e -> loadMoreIfNearEnd());
    }
  }

  private void disposeScrolledListAndShowErrorView(String errorMessage) {
    SwtUtils.getDisplay().asyncExec(() -> {
      hideProgressBar();

      // If itemsContainer exists, show error view inside it (same as loading view)
      if (itemsContainer != null && !itemsContainer.isDisposed()) {
        clearItems();
        createErrorItem(itemsContainer, errorMessage);
        itemsContainer.requestLayout();
        updateScrolledListMinSize();
        return;
      }
    });
  }

  private void clearItems() {
    if (itemsContainer != null && !itemsContainer.isDisposed()) {
      Control[] children = itemsContainer.getChildren();
      // Skip the first child (server list label) when clearing
      for (int i = 1; i < children.length; i++) {
        children[i].dispose();
      }
    }
  }

  private void createLoadingItem(Composite parent) {
    Composite loadingComposite = new Composite(parent, SWT.NONE);
    GridLayout loadingLayout = new GridLayout(1, false);
    loadingLayout.marginWidth = 10;
    loadingLayout.marginHeight = 10;
    loadingComposite.setLayout(loadingLayout);
    loadingComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

    Label loadingLabel = new Label(loadingComposite, SWT.NONE);
    loadingLabel.setText(Messages.mcpRegistryDialog_loading_label);
    loadingLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
  }

  private void createErrorItem(Composite parent, String errorMessage) {
    Composite errorComposite = new Composite(parent, SWT.NONE);
    GridLayout errorLayout = new GridLayout(1, false);
    errorLayout.marginWidth = 10;
    errorLayout.marginHeight = 10;
    errorComposite.setLayout(errorLayout);
    errorComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

    Label errorLabel = new Label(errorComposite, SWT.WRAP);
    errorLabel.setText(errorMessage);
    errorLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
  }

  /**
   * Show the progress bar at the bottom of the dialog.
   */
  private void showProgressBar() {
    if (progressBarContainer == null || progressBarContainer.isDisposed()) {
      return;
    }

    // Clear any existing progress bar and create a new one
    for (Control child : progressBarContainer.getChildren()) {
      child.dispose();
    }

    ProgressBar progressBar = new ProgressBar(progressBarContainer, SWT.INDETERMINATE);
    progressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Show the progress bar container
    GridData gridData = (GridData) progressBarContainer.getLayoutData();
    gridData.exclude = false;
    progressBarContainer.setVisible(true);
    progressBarContainer.requestLayout();
  }

  /**
   * Hide and dispose the progress bar.
   */
  private void hideProgressBar() {
    if (progressBarContainer == null || progressBarContainer.isDisposed()) {
      return;
    }

    // Dispose the progress bar
    for (Control child : progressBarContainer.getChildren()) {
      child.dispose();
    }

    // Hide the progress bar container
    GridData gridData = (GridData) progressBarContainer.getLayoutData();
    gridData.exclude = true;
    progressBarContainer.setVisible(false);
    progressBarContainer.requestLayout();
  }

  @Override
  protected Point getInitialSize() {
    Rectangle screenBounds = getShell().getDisplay().getPrimaryMonitor().getBounds();
    int width = screenBounds.width / 3;
    int height = screenBounds.height * 2 / 3;
    return new Point(width, height);
  }

  @Override
  protected Point getInitialLocation(Point initialSize) {
    // Center the dialog on the screen
    Rectangle screenBounds = getShell().getDisplay().getPrimaryMonitor().getBounds();
    int x = screenBounds.x + (screenBounds.width - initialSize.x) / 2;
    int y = screenBounds.y + (screenBounds.height - initialSize.y) / 2;
    return new Point(x, y);
  }

  @Override
  public boolean close() {
    if (preferenceChangeListener != null) {
      IEclipsePreferences configPrefs = ConfigurationScope.INSTANCE
          .getNode(CopilotUi.getPlugin().getBundle().getSymbolicName());
      configPrefs.removePreferenceChangeListener(preferenceChangeListener);
      preferenceChangeListener = null;
    }

    return super.close();
  }

  private void scheduleScrolledListMinSizeRefresh() {
    if (scrolledComposite == null || scrolledComposite.isDisposed()) {
      return;
    }

    if (pendingMinSizeRefresh != null) {
      scrolledComposite.getDisplay().timerExec(-1, pendingMinSizeRefresh);
    }

    pendingMinSizeRefresh = () -> {
      try {
        updateScrolledListMinSize();
      } finally {
        pendingMinSizeRefresh = null;
      }
    };

    scrolledComposite.getDisplay().timerExec(0, pendingMinSizeRefresh);
  }

  private void updateScrolledListMinSize() {
    if (scrolledComposite == null || scrolledComposite.isDisposed() || itemsContainer == null
        || itemsContainer.isDisposed()) {
      return;
    }

    itemsContainer.requestLayout();

    // For content with wrapping/variable height, compute using the current viewport width.
    Rectangle clientArea = scrolledComposite.getClientArea();
    int availableWidth = Math.max(0, clientArea.width);
    scrolledComposite.setMinSize(itemsContainer.computeSize(availableWidth, SWT.DEFAULT));
  }
}

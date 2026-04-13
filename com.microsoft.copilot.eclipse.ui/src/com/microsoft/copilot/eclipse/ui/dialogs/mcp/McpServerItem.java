package com.microsoft.copilot.eclipse.ui.dialogs.mcp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Icon;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.OfficialMeta;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Package;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Remote;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerDetail;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerResponse;
import com.microsoft.copilot.eclipse.ui.dialogs.mcp.McpServerInstallManager.ActionResult;
import com.microsoft.copilot.eclipse.ui.dialogs.mcp.McpServerInstallManager.ActionType;
import com.microsoft.copilot.eclipse.ui.dialogs.mcp.McpServerInstallManager.ButtonState;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.McpUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A composite widget representing a single MCP server item in the registry. Displays server icon, name, description,
 * metadata (version/dates), and install/uninstall actions.
 */
public class McpServerItem extends Composite implements EventHandler {
  private static final int SPACING = 5;

  private final ServerResponse serverResponse;
  private final McpRegistryController controller;
  private final IEventBroker eventBroker;
  private Label iconLabel;
  private Composite actionComposite;
  private Button actionButton;

  /**
   * Creates a new MCP server item.
   *
   * @param parent the parent composite
   * @param style the style bits
   * @param serverResponse the server response data
   * @param controller the registry controller
   */
  public McpServerItem(Composite parent, int style, ServerResponse serverResponse, McpRegistryController controller) {
    super(parent, style);
    this.serverResponse = serverResponse;
    this.controller = controller;
    this.setData(CssConstants.CSS_CLASS_NAME_KEY, "mcp-server-item");

    // Subscribe to server state change events
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (this.eventBroker != null) {
      this.eventBroker.subscribe(CopilotEventConstants.TOPIC_MCP_SERVER_STATE_CHANGE, this);
    }

    // Unsubscribe from event broker on dispose
    addDisposeListener(e -> {
      if (this.eventBroker != null) {
        this.eventBroker.unsubscribe(this);
      }
    });

    createContent();
  }

  private void createContent() {
    GridLayout layout = new GridLayout(1, false);
    layout.horizontalSpacing = 0;
    setLayout(layout);
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    // Main content area with icon and info
    final Composite contentArea = new Composite(this, SWT.NONE);
    contentArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    contentArea.setLayout(new GridLayout(2, false));

    // Left: Server icon
    iconLabel = new Label(contentArea, SWT.NONE);
    int iconSize = getScaledIconSize();
    GridData iconData = new GridData(SWT.CENTER, SWT.BEGINNING, false, false);
    iconData.widthHint = iconSize;
    iconData.heightHint = iconSize;
    iconLabel.setLayoutData(iconData);
    loadServerIcon();

    // Right: Info section (title, description, meta)
    createInfoSection(contentArea);

    // Bottom: Action buttons and a separator
    createActionSection(this);
    createBottomSeparator(this);
  }

  private void createInfoSection(Composite parent) {
    final Composite infoComposite = new Composite(parent, SWT.NONE);
    infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    GridLayout infoLayout = new GridLayout(1, false);
    infoLayout.marginHeight = 0;
    infoComposite.setLayout(infoLayout);

    ServerDetail detail = serverResponse.getDetail();
    if (detail == null) {
      return;
    }

    // Title (bold, slightly larger)
    Label titleLabel = new Label(infoComposite, SWT.NONE);
    titleLabel.setText(detail.name() != null ? detail.name() : "");
    FontDescriptor fontDescriptor = FontDescriptor.createFrom(titleLabel.getFont()).setStyle(SWT.BOLD)
        .setHeight(titleLabel.getFont().getFontData()[0].getHeight() + 1);
    titleLabel.setFont(fontDescriptor.createFont(titleLabel.getDisplay()));
    titleLabel.addDisposeListener(e -> fontDescriptor.destroyFont(titleLabel.getFont()));

    // Align icon vertically with description
    if (iconLabel != null) {
      Point titleSize = titleLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      GridData iconData = (GridData) iconLabel.getLayoutData();
      if (iconData != null) {
        iconData.verticalIndent = titleSize.y + SPACING;
      }
    }

    // Description with "more info" link
    if (StringUtils.isNotBlank(detail.description())) {
      Link descLink = new Link(infoComposite, SWT.WRAP);
      descLink.setText(NLS.bind(Messages.mcpServerItem_moreInfo_suffix, detail.description()));
      descLink.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          controller.openServerDetailDialog(getShell(), serverResponse);
        }
      });
      GridData descData = new GridData(SWT.FILL, SWT.FILL, true, false);
      descData.widthHint = 400;
      descLink.setLayoutData(descData);
    }

    // Meta row (version, published, updated)
    createMetaInfoRow(infoComposite, detail);
  }

  private void createMetaInfoRow(Composite parent, ServerDetail detail) {
    String version = detail.version();
    String publishedAt = getMetaOfficial() != null ? getMetaOfficial().publishedAt() : Messages.mcpServer_unknown;
    String updatedAt = getMetaOfficial() != null ? getMetaOfficial().updatedAt() : Messages.mcpServer_unknown;

    boolean hasAnyMeta = StringUtils.isNotBlank(version) || StringUtils.isNotBlank(publishedAt)
        || StringUtils.isNotBlank(updatedAt);

    if (!hasAnyMeta) {
      return;
    }

    final Composite metaRow = new Composite(parent, SWT.NONE);
    RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
    rowLayout.wrap = true;
    rowLayout.spacing = 12;
    rowLayout.marginLeft = 0;
    rowLayout.marginRight = 0;
    rowLayout.marginTop = 0;
    rowLayout.marginBottom = 0;
    metaRow.setLayout(rowLayout);

    boolean isDark = UiUtils.isDarkTheme();

    // Version
    if (StringUtils.isNotBlank(version)) {
      String iconPath = isDark ? "/icons/mcp/versions_dark.png" : "/icons/mcp/versions.png";
      String text = Messages.mcpServerDetailDialog_version + " " + version;
      createIconTextLabel(metaRow, iconPath, text, NLS.bind(Messages.mcpServerItem_versionTooltip, version));
    }

    // Published date
    if (StringUtils.isNotBlank(publishedAt)) {
      String iconPath = isDark ? "/icons/mcp/history_dark.png" : "/icons/mcp/history.png";
      String relativeTime = formatRelativeTime(publishedAt);
      String text = relativeTime != null ? Messages.mcpServerDetailDialog_published + " " + relativeTime
          : Messages.mcpServerDetailDialog_noPublishedDate;
      createIconTextLabel(metaRow, iconPath, text,
          NLS.bind(Messages.mcpServerItem_publishedTooltip, formatDetailedDate(publishedAt)));
    }

    // Updated date
    if (StringUtils.isNotBlank(updatedAt)) {
      String iconPath = isDark ? "/icons/mcp/update_dark.png" : "/icons/mcp/update.png";
      String relativeTime = formatRelativeTime(updatedAt);
      String text = relativeTime != null ? Messages.mcpServerDetailDialog_updated + " " + relativeTime
          : Messages.mcpServerDetailDialog_noUpdatedDate;
      createIconTextLabel(metaRow, iconPath, text,
          NLS.bind(Messages.mcpServerItem_updatedTooltip, formatDetailedDate(updatedAt)));
    }
  }

  /**
   * Gets the official from server meta.
   *
   * @return the OfficialMeta, or null if not available
   */
  private OfficialMeta getMetaOfficial() {
    if (serverResponse == null || serverResponse.meta() == null) {
      return null;
    }
    return serverResponse.meta().official();
  }

  private void createIconTextLabel(Composite parent, String iconPath, String text, String tooltip) {
    Composite row = new Composite(parent, SWT.NONE);
    GridLayout rowLayout = new GridLayout(2, false);
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    row.setLayout(rowLayout);

    // Icon
    Label iconLabel = new Label(row, SWT.NONE);
    Image icon = UiUtils.buildImageFromPngPath(iconPath);
    iconLabel.setImage(icon);
    iconLabel.addDisposeListener(e -> {
      if (icon != null && !icon.isDisposed()) {
        icon.dispose();
      }
    });

    // Text
    Label textLabel = new Label(row, SWT.NONE);
    textLabel.setText(text != null ? text : "");
    if (tooltip != null) {
      textLabel.setToolTipText(tooltip);
    }
  }

  private void createActionSection(Composite parent) {
    actionComposite = new Composite(parent, SWT.NONE);
    actionComposite.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
    GridLayout actionLayout = new GridLayout(1, false);
    actionLayout.marginHeight = 0;
    actionComposite.setLayout(actionLayout);

    populateActionButtons();
  }

  /**
   * Populates the action buttons in the action composite based on current installation state.
   */
  private void populateActionButtons() {
    ServerDetail detail = serverResponse.getDetail();
    if (detail == null) {
      return;
    }

    ButtonState initialState = controller.getInitialButtonState(detail.name(), controller.getMcpRegistryBaseUrl());
    boolean isInstalled = (initialState == ButtonState.UNINSTALL);

    if (isInstalled) {
      // Uninstall button
      Button uninstallButton = new Button(actionComposite, SWT.PUSH);
      uninstallButton.setText(initialState.getText());
      uninstallButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          controller.uninstallServer(detail.name());
        }
      });
      actionButton = uninstallButton;
    } else {
      // Install dropdown with options
      List<InstallOption> installOptions = buildInstallOptions(detail);

      if (installOptions.isEmpty()) {
        Button disabledInstallButton = new Button(actionComposite, SWT.PUSH);
        disabledInstallButton.setText(initialState.getText());
        disabledInstallButton.setEnabled(false);
        disabledInstallButton.setToolTipText(Messages.mcpServerItem_noInstallOptions);
        actionButton = disabledInstallButton;
      } else {
        DropDownButton dropDownInstallButton = createDropDownInstallButton(actionComposite, initialState,
            installOptions);
        actionButton = dropDownInstallButton.getButton();
      }
    }
  }

  private DropDownButton createDropDownInstallButton(Composite parent, ButtonState state, List<InstallOption> options) {
    DropDownButton dropDownInstallButton = new DropDownButton(parent, SWT.NONE);
    dropDownInstallButton.setShowArrow(options.size() > 1);
    dropDownInstallButton.setText(state.getText());

    Menu menu = new Menu(getShell());
    for (InstallOption option : options) {
      MenuItem item = new MenuItem(menu, SWT.PUSH);
      item.setText(option.label);
      item.addListener(SWT.Selection, e -> option.action.run());
    }

    dropDownInstallButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (e.detail == SWT.ARROW) {
          // Show dropdown menu below button
          Rectangle rect = dropDownInstallButton.getButton().getBounds();
          Point pt = dropDownInstallButton.getButton().getParent().toDisplay(rect.x, rect.y + rect.height);
          menu.setLocation(pt);
          menu.setVisible(true);
        } else if (!options.isEmpty()) {
          // Default action: execute first option
          options.get(0).action.run();
        }
      }
    });

    return dropDownInstallButton;
  }

  private void createBottomSeparator(Composite parent) {
    Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private List<InstallOption> buildInstallOptions(ServerDetail serverDetail) {
    List<InstallOption> items = new ArrayList<>();
    List<Remote> remotes = serverDetail.remotes();
    List<Package> packages = serverDetail.packages();

    // Remote server options
    if (remotes != null) {
      for (Remote remote : remotes) {
        JsonObject config = controller.createRemoteConfig(remote, serverDetail);
        String transportType = remote.transportType() != null ? remote.transportType() : "";
        String typeSuffix = StringUtils.isNotBlank(transportType) ? " (" + transportType + ")" : "";
        String label = NLS.bind(Messages.mcpServerItem_remoteLabel, remote.url(), typeSuffix);
        items.add(new InstallOption(label, () -> controller.installServer(serverDetail.name(), config)));
      }
    }

    // Package options
    if (packages != null) {
      for (Package pkg : packages) {
        JsonObject config = controller.createPackageConfig(pkg, serverDetail);
        String label = NLS.bind(Messages.mcpServerItem_packageLabel, pkg.registryType(), pkg.identifier());
        items.add(new InstallOption(label, () -> controller.installServer(serverDetail.name(), config)));
      }
    }

    return items;
  }

  private String formatDetailedDate(String dateString) {
    if (StringUtils.isBlank(dateString)) {
      return null;
    }
    Instant instant = Instant.parse(dateString);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm:ss a z")
        .withZone(ZoneId.systemDefault());
    return formatter.format(instant);
  }

  private String formatRelativeTime(String dateString) {
    if (StringUtils.isBlank(dateString)) {
      return null;
    }
    Instant instant = Instant.parse(dateString);
    return UiUtils.formatRelativeDateTime(instant);
  }

  @Override
  public void handleEvent(Event event) {
    if (!CopilotEventConstants.TOPIC_MCP_SERVER_STATE_CHANGE.equals(event.getTopic())) {
      return;
    }

    String eventServerName = (String) event.getProperty(McpServerInstallManager.EVENT_DATA_SERVER_NAME);
    ActionType actionType = (ActionType) event.getProperty(McpServerInstallManager.EVENT_DATA_ACTION_TYPE);
    ActionResult actionResult = (ActionResult) event.getProperty(McpServerInstallManager.EVENT_DATA_ACTION_RESULT);

    ServerDetail detail = serverResponse != null ? serverResponse.getDetail() : null;
    if (detail == null || !detail.name().equals(eventServerName)) {
      return;
    }

    getDisplay().asyncExec(() -> {
      ButtonState buttonState = McpServerInstallManager.determineButtonState(actionType, actionResult);
      updateButtonState(buttonState);
    });
  }

  /**
   * Updates the button state based on the new state.
   */
  private void updateButtonState(ButtonState state) {
    if (actionButton == null || actionButton.isDisposed()) {
      return;
    }

    switch (state) {
      case INSTALL:
      case UNINSTALL:
        // Refresh the entire action section for completed states
        refreshActionSection();
        break;
      case INSTALLING:
      case UNINSTALLING:
        // Update button text and disable for in-progress states
        actionButton.setText(state.getText());
        actionButton.setEnabled(false);
        break;
      default:
        break;
    }
  }

  /**
   * Refreshes the action section by disposing existing buttons and recreating them.
   */
  private void refreshActionSection() {
    if (actionComposite == null || actionComposite.isDisposed()) {
      return;
    }

    // Dispose all existing children in the action composite
    for (Control child : actionComposite.getChildren()) {
      child.dispose();
    }

    // Recreate buttons based on current state
    populateActionButtons();

    // Request layout update
    actionComposite.requestLayout();
  }

  /**
   * Simple holder for install option label and action.
   */
  private static class InstallOption {
    final String label;
    final Runnable action;

    InstallOption(String label, Runnable action) {
      this.label = label;
      this.action = action;
    }
  }

  private static final int BASE_ICON_SIZE = 40;

  /**
   * Loads the server icon from the server's icon list, preferring PNG format. Falls back to the default MCP icon if no
   * suitable icon is found or if loading fails.
   */
  private void loadServerIcon() {
    ServerDetail detail = serverResponse != null ? serverResponse.getDetail() : null;
    int iconSize = getScaledIconSize();

    List<Icon> icons = detail != null ? detail.icons() : null;
    String iconUrl = McpUtils.getPreferredIconUrl(icons);

    McpUtils.loadServerIcon(iconUrl, iconSize, iconSize).thenAccept(image -> {
      if (image == null) {
        return;
      }
      if (iconLabel == null || iconLabel.isDisposed()) {
        if (!image.isDisposed()) {
          image.dispose();
        }
        return;
      }
      iconLabel.setImage(image);
      iconLabel.addDisposeListener(e -> {
        if (!image.isDisposed()) {
          image.dispose();
        }
      });
    });
  }

  /**
   * Gets the scaled icon size based on the display's DPI settings. Returns 40 for standard displays and 80 for HiDPI
   * displays (200% scaling).
   *
   * @return the scaled icon size in pixels
   */
  private int getScaledIconSize() {
    int dpi = getDisplay().getDPI().x;
    // Standard DPI is 96 on Windows/Linux, 72 on macOS
    // For HiDPI (200% scaling), DPI would be 192 on Windows/Linux or 144 on macOS
    if (dpi >= 144) {
      return BASE_ICON_SIZE * 2; // 80x80 for HiDPI
    }
    return BASE_ICON_SIZE; // 40x40 for standard DPI
  }
}

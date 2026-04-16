// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.dialogs.mcp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.VerticalRuler;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Icon;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Package;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Remote;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerDetail;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerResponse;
import com.microsoft.copilot.eclipse.ui.dialogs.mcp.McpServerInstallManager.ButtonState;
import com.microsoft.copilot.eclipse.ui.utils.McpUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.TextMateUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog to display detailed information about an MCP server using event-driven architecture.
 */
public class McpServerDetailDialog extends Dialog implements EventHandler {
  private ServerResponse serverResponse;
  private Combo installOptionsCombo;
  private SourceViewer configurationPreviewViewer;
  private List<InstallOption> installOptions;
  private Button actionButton;
  private IEventBroker eventBroker;
  private McpServerInstallManager installManager;
  private String mcpRegistryBaseUrl;

  /**
   * Create a new MCP Server Detail Dialog with a shared install manager.
   *
   * @param parentShell The parent shell.
   * @param serverResponse The server response to display.
   * @param installManager Install manager from parent dialog.
   */
  public McpServerDetailDialog(Shell parentShell, ServerResponse serverResponse, McpServerInstallManager installManager,
      String mcpRegistryBaseUrl) {
    super(parentShell);
    this.serverResponse = serverResponse;
    this.installManager = installManager;
    this.mcpRegistryBaseUrl = mcpRegistryBaseUrl;
    setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

    try {
      this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to get IEventBroker from workbench service", e);
    }

    if (eventBroker != null) {
      eventBroker.subscribe(CopilotEventConstants.TOPIC_MCP_SERVER_STATE_CHANGE, this);
    }
  }

  /**
   * Convenience method to get the server detail from the server response.
   *
   * @return the server detail, or null if serverResponse is null
   */
  private ServerDetail getServerDetail() {
    return serverResponse != null ? serverResponse.getDetail() : null;
  }

  @Override
  public void handleEvent(Event event) {
    String eventServerName = (String) event.getProperty(McpServerInstallManager.EVENT_DATA_SERVER_NAME);
    McpServerInstallManager.ActionType actionType = (McpServerInstallManager.ActionType) event
        .getProperty(McpServerInstallManager.EVENT_DATA_ACTION_TYPE);
    McpServerInstallManager.ActionResult actionResult = (McpServerInstallManager.ActionResult) event
        .getProperty(McpServerInstallManager.EVENT_DATA_ACTION_RESULT);

    ServerDetail serverDetail = getServerDetail();
    if (serverDetail != null && Objects.equals(serverDetail.name(), eventServerName)
        && CopilotEventConstants.TOPIC_MCP_SERVER_STATE_CHANGE.equals(event.getTopic())) {
      // Determine button state based on action type + action result
      ButtonState buttonState = McpServerInstallManager.determineButtonState(actionType, actionResult);
      boolean success = !McpServerInstallManager.ActionResult.FAILURE.equals(actionResult);
      handleServerStateChange(buttonState, success);
    }
  }

  private void handleServerStateChange(ButtonState state, boolean success) {
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (actionButton != null && !actionButton.isDisposed()) {
        switch (state) {
          case INSTALLING:
          case UNINSTALLING:
            actionButton.setText(state.getText());
            actionButton.setEnabled(false);
            break;
          case INSTALL:
          case UNINSTALL:
            if (success) {
              // Update button text and state
              actionButton.setText(state.getText());
              actionButton.setEnabled(true);
              // Remove all Selection listeners and add appropriate listener based on state
              removeAllSelectionListeners(actionButton);
              if (state == ButtonState.UNINSTALL) {
                // Server was just installed, button should now uninstall
                actionButton.addListener(SWT.Selection, e -> onUninstall());
              } else {
                // Server was just uninstalled, button should now install
                actionButton.addListener(SWT.Selection, e -> onInstallSelected());
              }
            } else {
              // Failed operation - reset to previous state
              actionButton.setText(state.getText());
              actionButton.setEnabled(true);
            }
            break;
          default:
            break;
        }
      }
    });
  }

  /**
   * Helper method to remove all Selection event listeners from a widget.
   */
  private void removeAllSelectionListeners(Button button) {
    if (button == null || button.isDisposed()) {
      return;
    }

    for (Listener listener : button.getListeners(SWT.Selection)) {
      button.removeListener(SWT.Selection, listener);
    }
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    ServerDetail serverDetail = getServerDetail();
    String title = serverDetail != null && serverDetail.name() != null
        ? Messages.mcpServerDetailDialog_title + " - " + serverDetail.name()
        : Messages.mcpServerDetailDialog_title;
    newShell.setText(title);

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
  protected Point getInitialLocation(Point initialSize) {
    // Center the dialog on the parent shell (MCP registry dialog)
    Shell parent = getParentShell();
    if (parent != null && !parent.isDisposed()) {
      Point parentLocation = parent.getLocation();
      Point parentSize = parent.getSize();

      // Calculate center position
      int x = parentLocation.x + (parentSize.x - initialSize.x) / 2;
      int y = parentLocation.y + (parentSize.y - initialSize.y) / 2;

      return new Point(x, y);
    }

    // Fallback to default positioning if parent is not available
    return super.getInitialLocation(initialSize);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);

    ServerDetail serverDetail = getServerDetail();
    if (serverDetail == null) {
      createErrorContent(area);
      return area;
    }

    // Set up consistent layout for the entire area
    GridLayout areaLayout = new GridLayout(1, false);
    areaLayout.marginWidth = 30;
    areaLayout.marginTop = 10;
    areaLayout.verticalSpacing = 10;
    area.setLayout(areaLayout);

    // Server name and meta information section with icon
    createServerHeader(area);

    // Description section
    createSectionHeader(area, Messages.mcpServerDetailDialog_description);
    createDescriptionContent(area);

    // Installation options and configuration preview sections
    createInstallationOptionsSection(area);
    createConfigurationPreviewSection(area);
    populateInstallationOptionsAndSetInitialPreview();

    return area;
  }

  private void createServerHeader(Composite parent) {
    ServerDetail serverDetail = getServerDetail();
    if (serverDetail == null) {
      return;
    }

    Composite headerComposite = new Composite(parent, SWT.NONE);
    GridLayout headerLayout = new GridLayout(2, false);
    headerLayout.marginWidth = 0;
    headerLayout.marginHeight = 0;
    headerComposite.setLayout(headerLayout);
    headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    // Left: server icon, matching the list item appearance
    Label iconLabel = new Label(headerComposite, SWT.NONE);
    int iconSize = getScaledIconSizeForHeader(iconLabel);
    GridData iconData = new GridData(SWT.CENTER, SWT.BEGINNING, false, false);
    iconData.widthHint = iconSize;
    iconData.heightHint = iconSize;
    iconLabel.setLayoutData(iconData);
    loadServerIconForHeader(iconLabel);

    // Right: name and meta information stacked vertically
    Composite infoComposite = new Composite(headerComposite, SWT.NONE);
    GridLayout infoLayout = new GridLayout(1, false);
    infoLayout.marginHeight = 0;
    infoComposite.setLayout(infoLayout);
    infoComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    createSectionHeader(infoComposite, serverDetail.name());
    createInfoContent(infoComposite);
  }

  private int getScaledIconSizeForHeader(Label label) {
    int dpi = label.getDisplay().getDPI().x;
    if (dpi >= 144) {
      return 80;
    }
    return 40;
  }

  private void loadServerIconForHeader(Label iconLabel) {
    int iconSize = getScaledIconSizeForHeader(iconLabel);
    ServerDetail serverDetail = getServerDetail();
    List<Icon> icons = serverDetail != null ? serverDetail.icons() : null;
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

  private void createErrorContent(Composite parent) {
    Label errorLabel = new Label(parent, SWT.WRAP);
    errorLabel.setText(Messages.mcpServerDetailDialog_noDetailsAvailable);
    errorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void createSectionHeader(Composite parent, String headerText) {
    Label headerLabel = new Label(parent, SWT.NONE);
    headerLabel.setText(headerText);
    headerLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Font font = headerLabel.getFont();
    FontData[] fontData = font.getFontData();
    for (FontData fd : fontData) {
      fd.setStyle(SWT.BOLD);
    }
    Font boldFont = new Font(headerLabel.getDisplay(), fontData);
    headerLabel.setFont(boldFont);
    headerLabel.addDisposeListener(e -> boldFont.dispose());
  }

  private void createInfoContent(Composite parent) {
    Composite metaRow = new Composite(parent, SWT.NONE);
    metaRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
    rowLayout.wrap = true;
    rowLayout.spacing = 12;
    rowLayout.marginLeft = 0;
    rowLayout.marginRight = 0;
    rowLayout.marginTop = 0;
    rowLayout.marginBottom = 0;
    metaRow.setLayout(rowLayout);

    createVersionContent(metaRow);
    createPublishedContent(metaRow);
    createUpdatedContent(metaRow);

    if (hasRepositoryLink()) {
      createRepositoryLink(metaRow);
    }
  }

  /**
   * Gets the publishedAt date string from meta.official.
   *
   * @return the publishedAt date string, or unknown if not available
   */
  private String getPublishedAt() {
    if (serverResponse == null || serverResponse.meta() == null || serverResponse.meta().official() == null) {
      return Messages.mcpServer_unknown;
    }
    String publishedAt = serverResponse.meta().official().publishedAt();
    return StringUtils.isNotBlank(publishedAt) ? publishedAt : Messages.mcpServer_unknown;
  }

  /**
   * Gets the updatedAt date string from meta.official.
   *
   * @return the updatedAt date string, or unknown if not available
   */
  private String getUpdatedAt() {
    if (serverResponse == null || serverResponse.meta() == null || serverResponse.meta().official() == null) {
      return Messages.mcpServer_unknown;
    }
    String updatedAt = serverResponse.meta().official().updatedAt();
    return StringUtils.isNotBlank(updatedAt) ? updatedAt : Messages.mcpServer_unknown;
  }

  private boolean hasRepositoryLink() {
    return serverResponse != null && serverResponse.getDetail() != null
        && serverResponse.getDetail().repository() != null
        && StringUtils.isNotBlank(serverResponse.getDetail().repository().url());
  }

  private void createDescriptionContent(Composite parent) {
    ServerDetail serverDetail = getServerDetail();
    String description = serverDetail == null ? "" : serverDetail.description();
    Label descLabel = new Label(parent, SWT.WRAP);
    descLabel.setText(StringUtils.isBlank(description) ? Messages.mcpServerDetailDialog_noDescription : description);
    GridData descData = new GridData(SWT.FILL, SWT.TOP, true, false);
    descData.widthHint = 1; // Force wrapping by setting minimal width hint
    descLabel.setLayoutData(descData);
  }

  private void createVersionContent(Composite parent) {
    ServerDetail serverDetail = getServerDetail();
    String version = serverDetail == null ? Messages.mcpServer_unknown : serverDetail.version();
    createIconTextRow(parent, UiUtils.isDarkTheme() ? "/icons/mcp/versions_dark.png" : "/icons/mcp/versions.png",
        Messages.mcpServerDetailDialog_version + " " + version, Messages.mcpServerDetailDialog_version + " " + version);
  }

  private void createPublishedContent(Composite parent) {
    String publishedAt = getPublishedAt();
    String relativeTime = getFormattedRelativeTime(publishedAt);
    String detailedDate = getDetailedFormattedDate(publishedAt);
    String text = relativeTime != null ? Messages.mcpServerDetailDialog_published + " " + relativeTime
        : Messages.mcpServerDetailDialog_noPublishedDate;
    createIconTextRow(parent, UiUtils.isDarkTheme() ? "/icons/mcp/history_dark.png" : "/icons/mcp/history.png", text,
        detailedDate);
  }

  private void createUpdatedContent(Composite parent) {
    String updatedAt = getUpdatedAt();
    String relativeTime = getFormattedRelativeTime(updatedAt);
    String detailedDate = getDetailedFormattedDate(updatedAt);
    String text = relativeTime != null ? Messages.mcpServerDetailDialog_updated + " " + relativeTime
        : Messages.mcpServerDetailDialog_noUpdatedDate;
    createIconTextRow(parent, UiUtils.isDarkTheme() ? "/icons/mcp/update_dark.png" : "/icons/mcp/update.png", text,
        detailedDate);
  }

  private void createRepositoryLink(Composite parent) {
    Composite row = createRowWithIcon(parent,
        UiUtils.isDarkTheme() ? "/icons/mcp/repository_dark.png" : "/icons/mcp/repository.png");

    Link repoLink = new Link(row, SWT.NONE);
    repoLink.setText("<a>" + Messages.mcpServerDetailDialog_repository + "</a>");
    repoLink.setToolTipText(Messages.mcpServerDetailDialog_repository_tooltip);
    repoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    repoLink.addListener(SWT.Selection, ex -> {
      ServerDetail serverDetail = getServerDetail();
      String url = (serverDetail == null || serverDetail.repository() == null) ? "" : serverDetail.repository().url();
      if (url != null) {
        Program.launch(url);
      }
    });
  }

  /**
   * Builds a two-column row with an icon on the left and a text label on the right. Disposes the image when the icon
   * label is disposed.
   */
  private Composite createIconTextRow(Composite parent, String imagePath, String text, String tooltip) {
    Composite row = createRowWithIcon(parent, imagePath);

    Label textLabel = new Label(row, SWT.NONE);
    textLabel.setText(text != null ? text : "");
    if (tooltip != null) {
      textLabel.setToolTipText(tooltip);
    }
    textLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    return row;
  }

  private Composite createRowWithIcon(Composite parent, String imagePath) {
    GridLayout rowLayout = new GridLayout(2, false);
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    Composite row = new Composite(parent, SWT.NONE);
    row.setLayout(rowLayout);

    Label iconLabel = new Label(row, SWT.NONE);
    final Image icon = UiUtils.buildImageFromPngPath(imagePath);
    iconLabel.setImage(icon);
    iconLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    iconLabel.addDisposeListener(e -> {
      if (icon != null && !icon.isDisposed()) {
        icon.dispose();
      }
    });

    return row;
  }

  private String getDetailedFormattedDate(String dateString) {
    if (StringUtils.isBlank(dateString) || Messages.mcpServer_unknown.equals(dateString)) {
      return null;
    }
    try {
      // Parse ISO 8601 date format like "2025-09-09T14:46:07.969809594Z"
      Instant instant = Instant.parse(dateString);
      // Format with full date, time and timezone
      DateTimeFormatter detailedFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm:ss a z")
          .withZone(ZoneId.systemDefault());
      return detailedFormatter.format(instant);
    } catch (Exception e) {
      // If parsing fails, return the original string
      return dateString;
    }
  }

  /**
   * Formats the given date string as a relative time using UiUtils.formatRelativeDateTime.
   *
   * @param dateString the ISO 8601 date string to format
   * @return formatted relative time string, or null if the date string is blank or invalid
   */
  private String getFormattedRelativeTime(String dateString) {
    if (StringUtils.isBlank(dateString) || Messages.mcpServer_unknown.equals(dateString)) {
      return null;
    }
    Instant instant = Instant.parse(dateString);
    return UiUtils.formatRelativeDateTime(instant);
  }

  private void createInstallationOptionsSection(Composite parent) {
    // Installation Options header
    createSectionHeader(parent, Messages.mcpServerDetailDialog_installation_options);

    // Create combo box for installation options
    installOptionsCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
    installOptionsCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Add selection listener to update configuration preview
    installOptionsCombo.addListener(SWT.Selection, e -> updateConfigurationPreview());
  }

  private void createConfigurationPreviewSection(Composite parent) {
    // Configuration Preview header
    createSectionHeader(parent, Messages.mcpServerDetailDialog_configuration_preview);

    // Create text area for configuration preview
    configurationPreviewViewer = new SourceViewer(parent, new VerticalRuler(0),
        SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    GridData textData = new GridData(SWT.FILL, SWT.FILL, true, true);
    textData.heightHint = 100;
    configurationPreviewViewer.setEditable(false);

    // Configure JSON syntax highlighting
    configurationPreviewViewer.configure(TextMateUtils.getConfiguration("json"));

    configurationPreviewViewer.getControl().setLayoutData(textData);
    configurationPreviewViewer.getControl().setBackground(
        configurationPreviewViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
  }

  private void populateInstallationOptions() {
    installOptions = new ArrayList<>();

    ServerDetail serverDetail = getServerDetail();
    if (serverDetail == null) {
      return;
    }

    // Cache lists locally and compute presence flags
    List<Remote> remotes = serverDetail.remotes();
    List<Package> packages = serverDetail.packages();
    boolean hasRemotes = remotes != null && !remotes.isEmpty();
    boolean hasPackages = packages != null && !packages.isEmpty();

    // Add remote server options first (typically preferred)
    if (hasRemotes) {
      for (Remote remote : remotes) {
        JsonObject config = McpServerConfigurationBuilder.createRemoteServerConfiguration(remote, serverDetail,
            this.mcpRegistryBaseUrl);
        String typeSuffix = (remote.transportType() != null && StringUtils.isNotBlank(remote.transportType().toString())
            ? " (" + remote.transportType().toString() + ")"
            : "");
        InstallOption option = new InstallOption(
            NLS.bind(Messages.mcpServerDetailDialog_remote_prefix, remote.url(), typeSuffix), config);

        installOptions.add(option);
        installOptionsCombo.add(option.getDisplayName());
      }
    }

    // Add package options
    if (hasPackages) {
      for (Package pkg : packages) {
        JsonObject config = McpServerConfigurationBuilder.createPackageServerConfiguration(pkg, serverDetail,
            this.mcpRegistryBaseUrl);
        InstallOption option = new InstallOption(pkg.registryType() + ": " + pkg.identifier(), config);
        installOptions.add(option);
        installOptionsCombo.add(option.getDisplayName());
      }
    }

    if (installOptions.isEmpty()) {
      // Set placeholder text when no install options available
      installOptionsCombo.add(Messages.mcpServerDetailDialog_no_install_options);
      installOptionsCombo.select(0);
      installOptionsCombo.setEnabled(false);
    } else {
      // Select first option by default
      installOptionsCombo.select(0);
    }
  }

  private void populateInstallationOptionsAndSetInitialPreview() {
    populateInstallationOptions();

    // Update the configuration preview for the initially selected option
    updateConfigurationPreview();
  }

  private void updateConfigurationPreview() {
    if (installOptionsCombo == null || configurationPreviewViewer == null) {
      return;
    }

    if (installOptions == null || installOptions.isEmpty()) {
      configurationPreviewViewer.setDocument(new Document(Messages.mcpServerDetailDialog_no_configuration_available));
      return;
    }

    int selectedIndex = installOptionsCombo.getSelectionIndex();
    if (selectedIndex >= 0 && selectedIndex < installOptions.size()) {
      InstallOption selectedOption = installOptions.get(selectedIndex);
      JsonObject config = selectedOption.getConfiguration();

      // Format the JSON for display
      Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
      String jsonString = gson.toJson(config);
      configurationPreviewViewer.setDocument(new Document(jsonString));
    } else {
      configurationPreviewViewer.setDocument(new Document(""));
    }
  }

  @Override
  protected Point getInitialSize() {
    return new Point(650, 500);
  }

  private void onInstallSelected() {
    ServerDetail serverDetail = getServerDetail();
    if (installOptionsCombo == null || serverDetail == null || installOptions == null) {
      return;
    }

    // Get the selected configuration
    int selectedIndex = installOptionsCombo.getSelectionIndex();
    if (selectedIndex >= 0 && selectedIndex < installOptions.size()) {
      InstallOption selectedOption = installOptions.get(selectedIndex);
      JsonObject config = selectedOption.getConfiguration();

      if (installManager != null) {
        installManager.installServer(serverDetail.name(), config);
      }
    }
  }

  private void onUninstall() {
    ServerDetail serverDetail = getServerDetail();
    if (serverDetail == null) {
      return;
    }

    if (installManager != null) {
      installManager.uninstallServer(serverDetail.name());
    }
  }

  @Override
  public boolean close() {
    if (eventBroker != null) {
      eventBroker.unsubscribe(this);
    }
    return super.close();
  }

  /**
   * Class to represent an installation option with display name and configuration.
   */
  private static class InstallOption {
    private final String displayName;
    private final JsonObject configuration;

    public InstallOption(String displayName, JsonObject configuration) {
      this.displayName = displayName;
      this.configuration = configuration;
    }

    public String getDisplayName() {
      return displayName;
    }

    public JsonObject getConfiguration() {
      return configuration;
    }
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    ServerDetail serverDetail = getServerDetail();

    // Create action button only if server detail exists and has valid installation options
    if (serverDetail != null && this.installOptions != null && !this.installOptions.isEmpty()) {
      ButtonState initialState = this.installManager.getInitialState(serverDetail.name(), this.mcpRegistryBaseUrl);
      boolean isInstalled = initialState == ButtonState.UNINSTALL;

      actionButton = createButton(parent, 1000, initialState.getText(), true);
      actionButton.addListener(SWT.Selection, e -> {
        if (isInstalled) {
          onUninstall();
        } else {
          onInstallSelected();
        }
      });
    }

    // Always create Close button
    createButton(parent, CANCEL, Messages.mcpServerDetailDialog_close, false);
  }
}

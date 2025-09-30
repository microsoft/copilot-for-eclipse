package com.microsoft.copilot.eclipse.ui.dialogs;

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
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.VerticalRuler;
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
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.registry.IGrammarRegistryManager;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.TMUIPlugin;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.mcp.Package;
import com.microsoft.copilot.eclipse.core.lsp.mcp.Remote;
import com.microsoft.copilot.eclipse.core.lsp.mcp.ServerDetail;
import com.microsoft.copilot.eclipse.ui.dialogs.McpServerInstallManager.ButtonState;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog to display detailed information about an MCP server using event-driven architecture.
 */
public class McpServerDetailDialog extends Dialog implements EventHandler {
  private ServerDetail serverDetail;
  private Combo installOptionsCombo;
  private SourceViewer configurationPreviewViewer;
  private List<InstallOption> installOptions;
  private Button installButton;
  private IEventBroker eventBroker;
  private McpServerInstallManager installManager;

  /**
   * Create a new MCP Server Detail Dialog with a shared install manager.
   *
   * @param parentShell The parent shell.
   * @param serverDetail The server detail to display.
   * @param installManager Install manager from parent dialog.
   */
  public McpServerDetailDialog(Shell parentShell, ServerDetail serverDetail, McpServerInstallManager installManager) {
    super(parentShell);
    this.serverDetail = serverDetail;
    this.installManager = installManager;
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

  @Override
  public void handleEvent(Event event) {
    String eventServerName = (String) event.getProperty(McpServerInstallManager.EVENT_DATA_SERVER_NAME);
    McpServerInstallManager.ActionType actionType = (McpServerInstallManager.ActionType) event
        .getProperty(McpServerInstallManager.EVENT_DATA_ACTION_TYPE);
    McpServerInstallManager.ActionResult actionResult = (McpServerInstallManager.ActionResult) event
        .getProperty(McpServerInstallManager.EVENT_DATA_ACTION_RESULT);

    if (Objects.equals(serverDetail.getName(), eventServerName)
        && CopilotEventConstants.TOPIC_MCP_SERVER_STATE_CHANGE.equals(event.getTopic())) {
      // Determine button state based on action type + action result
      ButtonState buttonState = determineButtonState(actionType, actionResult);
      boolean success = !McpServerInstallManager.ActionResult.FAILURE.equals(actionResult);
      handleServerStateChange(buttonState, success);
    }
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

    // Default fallback
    return ButtonState.INSTALL;
  }

  private void handleServerStateChange(ButtonState state, boolean success) {
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (installButton != null && !installButton.isDisposed()) {
        switch (state) {
          case INSTALLING:
          case UNINSTALLING:
            installButton.setText(state.getText());
            installButton.setEnabled(false);
            break;
          case INSTALL:
          case UNINSTALL:
            if (success) {
              // Update button text and state
              installButton.setText(state.getText());
              installButton.setEnabled(true);
              // Remove all Selection listeners and add appropriate listener based on state
              removeAllSelectionListeners(installButton);
              if (state == ButtonState.UNINSTALL) {
                // Server was just installed, button should now uninstall
                installButton.addListener(SWT.Selection, e -> onUninstall());
              } else {
                // Server was just uninstalled, button should now install
                installButton.addListener(SWT.Selection, e -> onInstallSelected());
              }
            } else {
              // Failed operation - reset to previous state
              installButton.setText(state.getText());
              installButton.setEnabled(true);
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
    String title = serverDetail != null && serverDetail.getName() != null
        ? Messages.mcpServerDetailDialog_title + " - " + serverDetail.getName()
        : Messages.mcpServerDetailDialog_title;
    newShell.setText(title);

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

    // Server name and meta information section
    createSectionHeader(area, serverDetail.getName());
    createInfoContent(area);

    // Description section
    createSectionHeader(area, Messages.mcpServerDetailDialog_description);
    createDescriptionContent(area);

    // Installation options and configuration preview sections
    createInstallationOptionsSection(area);
    createConfigurationPreviewSection(area);
    populateInstallationOptionsAndSetInitialPreview();

    return area;
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
    // Build a single row (wrappable) for Version, Published, Updated if any exist
    boolean showVersion = hasVersion();
    boolean showPublished = hasPublishedDate();
    boolean showUpdated = hasUpdatedDate();
    boolean showRepositoryLink = hasRepositoryLink();

    if (showVersion || showPublished || showUpdated || showRepositoryLink) {
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

      if (showVersion) {
        createVersionContent(metaRow);
      }
      if (showPublished) {
        createPublishedContent(metaRow);
      }
      if (showUpdated) {
        createUpdatedContent(metaRow);
      }
      if (showRepositoryLink) {
        createRepositoryLink(metaRow);
      }
    }
  }

  private boolean hasVersion() {
    return serverDetail != null && StringUtils.isNotBlank(serverDetail.getVersion());
  }

  private boolean hasPublishedDate() {
    return serverDetail != null && serverDetail.getMeta() != null && serverDetail.getMeta().getOfficial() != null
        && StringUtils.isNotBlank(serverDetail.getMeta().getOfficial().getPublishedAt());
  }

  private boolean hasUpdatedDate() {
    return serverDetail != null && serverDetail.getMeta() != null && serverDetail.getMeta().getOfficial() != null
        && StringUtils.isNotBlank(serverDetail.getMeta().getOfficial().getUpdatedAt());
  }

  private boolean hasRepositoryLink() {
    return serverDetail != null && serverDetail.getRepository() != null
        && StringUtils.isNotBlank(serverDetail.getRepository().getUrl());
  }

  private void createDescriptionContent(Composite parent) {
    String description = serverDetail.getDescription();
    Label descLabel = new Label(parent, SWT.WRAP);
    descLabel.setText(StringUtils.isBlank(description) ? Messages.mcpServerDetailDialog_noDescription : description);
    GridData descData = new GridData(SWT.FILL, SWT.TOP, true, false);
    descData.widthHint = 1; // Force wrapping by setting minimal width hint
    descLabel.setLayoutData(descData);
  }

  private void createVersionContent(Composite parent) {
    String version = serverDetail.getVersion();
    createIconTextRow(parent, UiUtils.isDarkTheme() ? "/icons/mcp/versions_dark.png" : "/icons/mcp/versions.png",
        Messages.mcpServerDetailDialog_version + " " + version, Messages.mcpServerDetailDialog_version + " " + version);
  }

  private void createPublishedContent(Composite parent) {
    String relativeTime = getFormattedRelativeTime(serverDetail.getMeta().getOfficial().getPublishedAt());
    String detailedDate = getDetailedFormattedDate(serverDetail.getMeta().getOfficial().getPublishedAt());
    String text = relativeTime != null ? Messages.mcpServerDetailDialog_published + " " + relativeTime
        : Messages.mcpServerDetailDialog_noPublishedDate;
    createIconTextRow(parent, UiUtils.isDarkTheme() ? "/icons/mcp/history_dark.png" : "/icons/mcp/history.png", text,
        detailedDate);
  }

  private void createUpdatedContent(Composite parent) {
    String relativeTime = getFormattedRelativeTime(serverDetail.getMeta().getOfficial().getUpdatedAt());
    String detailedDate = getDetailedFormattedDate(serverDetail.getMeta().getOfficial().getUpdatedAt());
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
      String url = serverDetail.getRepository().getUrl();
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
    if (StringUtils.isBlank(dateString)) {
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
    if (StringUtils.isBlank(dateString)) {
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
    configurationPreviewViewer.configure(createJsonSourceViewerConfiguration());
    
    configurationPreviewViewer.getControl().setLayoutData(textData);
    configurationPreviewViewer.getControl().setBackground(
        configurationPreviewViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
  }

  /**
   * Creates a SourceViewerConfiguration with JSON syntax highlighting using TextMate grammar.
   * This follows the same pattern as the existing SourceViewerComposite implementation.
   */
  private SourceViewerConfiguration createJsonSourceViewerConfiguration() {
    TMPresentationReconciler reconciler = new TMPresentationReconciler();
    IGrammarRegistryManager mgr = TMEclipseRegistryPlugin.getGrammarRegistryManager();
    IGrammar grammar = mgr.getGrammarForFileExtension("json");
    reconciler.setGrammar(grammar);
    
    if (grammar != null) {
      reconciler.setTheme(TMUIPlugin.getThemeManager().getThemeForScope(grammar.getScopeName()));
    }
    
    return new SourceViewerConfiguration() {
      @Override
      public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        return reconciler;
      }
    };
  }

  private void populateInstallationOptions() {
    installOptions = new ArrayList<>();

    if (serverDetail == null) {
      return;
    }

    // Cache lists locally and compute presence flags
    List<Remote> remotes = serverDetail.getRemotes();
    List<Package> packages = serverDetail.getPackages();
    boolean hasRemotes = remotes != null && !remotes.isEmpty();
    boolean hasPackages = packages != null && !packages.isEmpty();

    // Add remote server options first (typically preferred)
    if (hasRemotes) {
      for (Remote remote : remotes) {
        JsonObject config = McpServerConfigurationBuilder.createRemoteServerConfiguration(remote, serverDetail);
        String typeSuffix = (remote.getTransportType() != null
            && StringUtils.isNotBlank(remote.getTransportType().toString())
                ? " (" + remote.getTransportType().toString() + ")"
                : "");
        InstallOption option = new InstallOption(
            Messages.mcpServerDetailDialog_remote_prefix.replace("{0}", remote.getUrl()).replace("{1}", typeSuffix),
            config);

        installOptions.add(option);
        installOptionsCombo.add(option.getDisplayName());
      }
    }

    // Add package options
    if (hasPackages) {
      for (Package pkg : packages) {
        JsonObject config = McpServerConfigurationBuilder.createPackageServerConfiguration(pkg, serverDetail);
        InstallOption option = new InstallOption(pkg.getRegistryType() + ": " + pkg.getIdentifier(), config);
        installOptions.add(option);
        installOptionsCombo.add(option.getDisplayName());
      }
    }

    // Select first option by default
    if (!installOptions.isEmpty()) {
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

    int selectedIndex = installOptionsCombo.getSelectionIndex();
    if (selectedIndex >= 0 && selectedIndex < installOptions.size()) {
      InstallOption selectedOption = installOptions.get(selectedIndex);
      JsonObject config = selectedOption.getConfiguration();

      // Format the JSON for display
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
    if (installOptionsCombo == null || serverDetail == null || installOptions == null) {
      return;
    }

    // Get the selected configuration
    int selectedIndex = installOptionsCombo.getSelectionIndex();
    if (selectedIndex >= 0 && selectedIndex < installOptions.size()) {
      InstallOption selectedOption = installOptions.get(selectedIndex);
      JsonObject config = selectedOption.getConfiguration();

      if (installManager != null) {
        installManager.installServer(serverDetail.getName(), config);
      }
    }
  }

  private void onUninstall() {
    if (serverDetail == null) {
      return;
    }

    if (installManager != null) {
      installManager.uninstallServer(serverDetail.getName());
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
    // Determine initial button state based on whether server is installed
    String serverName = serverDetail != null ? serverDetail.getName() : "";
    ButtonState initialState = McpServerInstallManager.getInitialState(serverName);
    boolean isInstalled = initialState == ButtonState.UNINSTALL;

    // Create Install/Uninstall button
    installButton = createButton(parent, 1000, initialState.getText(), false);

    if (isInstalled) {
      // If installed, set up uninstall functionality
      installButton.addListener(SWT.Selection, e -> onUninstall());
    } else {
      // If not installed, set up install functionality
      installButton.addListener(SWT.Selection, e -> onInstallSelected());
    }

    // Create Close button
    createButton(parent, CANCEL, Messages.mcpServerDetailDialog_close, true);
  }
}

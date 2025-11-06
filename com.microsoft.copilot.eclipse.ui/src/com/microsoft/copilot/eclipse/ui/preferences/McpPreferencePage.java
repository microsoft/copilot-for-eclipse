package com.microsoft.copilot.eclipse.ui.preferences;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;
import com.microsoft.copilot.eclipse.core.chat.CustomChatModeManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerStatus;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.RegistryAccess;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.McpExtensionPointManager;
import com.microsoft.copilot.eclipse.ui.dialogs.McpRegistryDialog;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.McpUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Preference page for GitHub Copilot MCP settings.
 */
public class McpPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage";

  private static final int GROUP_HEIGHT_HINT = 260;
  private static final Gson GSON = new Gson();

  private Group toolsGroup;
  private Group mcpGroup;
  private Group mcpRegistryGroup;
  private Tree toolsTree;
  private boolean hasFailedMcpServer;
  private Combo modeSelector;
  private Composite modeSelectorComposite;
  private String currentModeId = "agent-mode";
  private Map<String, Map<String, Map<String, Boolean>>> modeToolStatus = new HashMap<>();
  private StringFieldEditor mcpField;
  private Image redNotice;
  private Label redNoticeLabel;
  private Composite extMcpTitleComposite; // store title composite for layout refresh and dynamic icon creation
  private StringFieldEditor mcpRegistryField;
  private Button openRegistryButton;
  private Composite registryInfoMessageComposite;
  private CopilotLanguageServerConnection copilotLanguageServerConnection;

  /**
   * Constructor.
   */
  public McpPreferencePage() {
    super(GRID);
  }

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
    this.copilotLanguageServerConnection = CopilotCore.getPlugin() != null
        ? CopilotCore.getPlugin().getCopilotLanguageServer()
        : null;

    Job job = new Job("Binding to MCP service...") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          Job.getJobManager().join(CopilotUi.INIT_JOB_FAMILY, null);
        } catch (OperationCanceledException | InterruptedException e) {
          CopilotCore.LOGGER.error(e);
        }
        CopilotUi.getPlugin().getChatServiceManager().getMcpConfigService()
            .bindWithMcpPreferencePage(McpPreferencePage.this);
        return Status.OK_STATUS;
      }
    };
    job.setUser(true);
    job.schedule();
  }

  @Override
  public void applyData(Object data) {
    super.applyData(data);

    // If data is a mode ID, select that mode when the page opens
    if (data instanceof String) {
      String modeId = (String) data;
      if (modeSelector != null && !modeSelector.isDisposed()) {
        selectModeById(modeId);
      } else {
        // Store for later when the UI is created
        currentModeId = modeId;
      }
    }
  }

  @Override
  protected Control createContents(Composite parent) {
    // Create a simple note for the feature disabled case
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    if (flags != null && !flags.isMcpEnabled()) {
      return WrappableIconLink.createWithSharedImage(parent,
          PlatformUI.getWorkbench().getSharedImages().getImage(org.eclipse.ui.ISharedImages.IMG_OBJS_INFO_TSK),
          Messages.preferences_page_mcp_disabled_tip);
    }

    // Call the default implementation for enabled case
    return super.createContents(parent);
  }

  @Override
  protected void createFieldEditors() {
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    if (flags != null && !flags.isMcpEnabled()) {
      // Don't create field editors when MCP is disabled - handled in createContents
      return;
    }

    Composite parent = getFieldEditorParent();
    parent.setLayout(new GridLayout(1, true));
    GridLayout gl = new GridLayout(1, true);
    gl.marginTop = 2;
    gl.marginLeft = 2;

    GridDataFactory gdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true);
    mcpGroup = new Group(parent, SWT.NONE);
    mcpGroup.setLayout(gl);
    gdf.applyTo(mcpGroup);
    mcpGroup.setText(Messages.preferences_page_mcp_settings);
    // add mcp field
    Composite mcpFieldContainer = new Composite(mcpGroup, SWT.NONE);
    mcpFieldContainer.setLayout(gl);
    mcpFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    mcpField = new StringFieldEditor(Constants.MCP, Messages.preferences_page_mcp, StringFieldEditor.UNLIMITED, 20,
        StringFieldEditor.VALIDATE_ON_KEY_STROKE, mcpFieldContainer) {
      @Override
      protected boolean doCheckState() {
        return validateMcpField(this);
      }

      @Override
      protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        getTextControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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

    // add note to mcp field using WrappableNoteLabel
    new WrappableNoteLabel(mcpGroup, Messages.preferences_page_note_prefix, Messages.preferences_page_mcp_note_content);

    createExtMcpRegistrationArea(mcpGroup);

    if (PlatformUtils.isNightly()) {
      // add mcp registry field and button
      mcpRegistryGroup = new Group(parent, SWT.NONE);
      mcpRegistryGroup.setLayout(gl);
      // Use a separate GridDataFactory for registry group without height constraint
      GridDataFactory registryGdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true,
          false);
      registryGdf.applyTo(mcpRegistryGroup);
      mcpRegistryGroup.setText(Messages.preferences_page_mcp_registry_settings);

      // Add description label with link
      PreferencePageUtils.createExternalLink(mcpRegistryGroup, Messages.preferences_page_mcp_registry_description,
          null);

      // Create container for URL field and button on the same line
      Composite fieldButtonContainer = new Composite(mcpRegistryGroup, SWT.NONE);
      GridLayout fieldButtonLayout = new GridLayout(2, false);
      fieldButtonLayout.marginHeight = 0;
      fieldButtonLayout.marginWidth = 0;
      fieldButtonContainer.setLayout(fieldButtonLayout);
      fieldButtonContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

      // Add URL field
      Composite mcpRegistryFieldContainer = new Composite(fieldButtonContainer, SWT.NONE);
      mcpRegistryFieldContainer.setLayout(new GridLayout(1, false));
      mcpRegistryFieldContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      mcpRegistryField = new StringFieldEditor(Constants.MCP_REGISTRY_URL, Messages.preferences_page_mcp_registry_url,
          StringFieldEditor.UNLIMITED, StringFieldEditor.VALIDATE_ON_KEY_STROKE, mcpRegistryFieldContainer) {
        @Override
        protected void doFillIntoGrid(Composite parent, int numColumns) {
          super.doFillIntoGrid(parent, numColumns);
          getTextControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        }

        @Override
        protected void doLoad() {
          McpUtils.getMcpAllowList(copilotLanguageServerConnection).thenAccept(allowList -> {
            SwtUtils.invokeOnDisplayThreadAsync(() -> {
              if (allowList == null || allowList.mcpRegistries.isEmpty()) {
                getTextControl().setText(CopilotUi.getStringPreference(Constants.MCP_REGISTRY_URL,
                    CopilotPreferenceInitializer.DEFAULT_MCP_REGISTRY_URL));
              } else {
                // Use the first registry URL from the allowlist
                String registryUrl = McpUtils.parseMcpRegistryUrlFromAllowList(allowList);
                getTextControl().setText(registryUrl != null ? registryUrl
                    : CopilotUi.getStringPreference(Constants.MCP_REGISTRY_URL,
                        CopilotPreferenceInitializer.DEFAULT_MCP_REGISTRY_URL));
                if (allowList.mcpRegistries.get(0).getRegistryAccess() == RegistryAccess.registry_only) {
                  // Disable the field
                  getTextControl().setEnabled(false);

                  // Show info message
                  String owner = allowList.mcpRegistries.get(0).getOwner().getLogin();
                  String message = NLS.bind(Messages.preferences_page_mcp_registry_restricted_info, owner);
                  showRegistryInfoMessage(message);
                }
              }
            });
          }).exceptionally(ex -> {
            CopilotCore.LOGGER.error("Failed to load MCP registry URL", ex);
            return null;
          });

          // Add modify listener after text control is created and loaded
          getTextControl().addModifyListener(e -> {
            boolean hasContent = StringUtils.isNotBlank(getStringValue());
            if (openRegistryButton != null && !openRegistryButton.isDisposed()) {
              openRegistryButton.setEnabled(hasContent);
            }
          });

          updateRegistryButtonState();
        }

        @Override
        protected void doStore() {
          saveMcpRegistryUrlToGlobalScope();
        }

        @Override
        protected void doLoadDefault() {
          getTextControl().setText(CopilotPreferenceInitializer.DEFAULT_MCP_REGISTRY_URL);
          updateRegistryButtonState();
        }
      };
      addField(mcpRegistryField);

      // Add Open MCP Registry button
      openRegistryButton = new Button(fieldButtonContainer, SWT.PUSH);
      openRegistryButton.setText(Messages.preferences_page_mcp_registry_button);
      GridData buttonData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
      openRegistryButton.setLayoutData(buttonData);
      openRegistryButton.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          saveMcpRegistryUrlToGlobalScope();

          Shell parentShell = getShell();
          if (getContainer() != null && getContainer() instanceof PreferenceDialog) {
            ((PreferenceDialog) getContainer()).close();
          }

          Shell mcpRegistryDialogShell = findExistingMcpRegistryDialog();
          if (mcpRegistryDialogShell != null) {
            mcpRegistryDialogShell.forceActive();
            mcpRegistryDialogShell.setActive();
          } else {
            McpRegistryDialog mcpRegistryDialog = new McpRegistryDialog(parentShell);
            mcpRegistryDialog.open();
          }
        }
      });

      // Set initial button state after field is created - use async to ensure field is fully initialized
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        boolean initialHasContent = StringUtils.isNotBlank(mcpRegistryField.getStringValue());
        openRegistryButton.setEnabled(initialHasContent);
      });
    }

    toolsGroup = new Group(parent, SWT.WRAP);
    toolsGroup.setLayout(gl);
    GridDataFactory toolsGdf = GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true);
    toolsGdf.applyTo(toolsGroup);
    toolsGroup.setText(Messages.preferences_page_mcp_tools_settings);

    // Add mode selector
    createModeSelector(toolsGroup);

    // Set equal height constraint for both groups
    ((GridData) mcpGroup.getLayoutData()).heightHint = GROUP_HEIGHT_HINT;
    ((GridData) toolsGroup.getLayoutData()).heightHint = GROUP_HEIGHT_HINT;
  }

  /**
   * Shows an info message below the registry field when the URL is managed and cannot be modified.
   *
   * @param message the message to display
   */
  private void showRegistryInfoMessage(String message) {
    if (mcpRegistryGroup == null || mcpRegistryGroup.isDisposed()) {
      return;
    }

    // Remove existing info message if present
    if (registryInfoMessageComposite != null && !registryInfoMessageComposite.isDisposed()) {
      registryInfoMessageComposite.dispose();
    }

    // Create info message composite using WrappableIconLink
    registryInfoMessageComposite = WrappableIconLink.createWithCustomizedImage(mcpRegistryGroup,
        "/icons/information.png", message);

    // Trigger layout update on the entire hierarchy to ensure proper sizing
    mcpRegistryGroup.requestLayout();
  }

  private void createExtMcpRegistrationArea(Composite parent) {
    McpExtensionPointManager extMcpManager = CopilotUi.getPlugin().getChatServiceManager()
        .getMcpExtensionPointManager();
    if (!extMcpManager.hasExtMcpRegistration()) {
      return;
    }

    // Create a composite to hold the title label and optional red notice icon
    extMcpTitleComposite = new Composite(parent, SWT.NONE);
    GridLayout extMcpTitleLayout = new GridLayout(2, false);
    extMcpTitleLayout.marginWidth = 0;
    extMcpTitleLayout.marginHeight = 0;
    extMcpTitleComposite.setLayout(extMcpTitleLayout);
    extMcpTitleComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    // Red notice icon
    var service = CopilotUi.getPlugin().getChatServiceManager().getMcpConfigService();
    boolean newExtMcpRegFound = service.isNewExtMcpRegFound();
    if (newExtMcpRegFound) {
      if (redNotice == null || redNotice.isDisposed()) {
        redNotice = UiUtils.buildImageFromPngPath("/icons/chat/red_notice.png");
      }
      redNoticeLabel = new Label(extMcpTitleComposite, SWT.NONE);
      redNoticeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
      redNoticeLabel.setImage(redNotice);
      redNoticeLabel.setToolTipText(Messages.chat_actionBar_toolButton_detected_toolTip);
      redNoticeLabel.addDisposeListener(e -> {
        if (redNotice != null && !redNotice.isDisposed()) {
          redNotice.dispose();
          redNotice = null;
        }
      });
    }

    // Title label
    new WrappableNoteLabel(extMcpTitleComposite, Messages.preferences_page_extMcp_title, "");

    // Edit button
    Button extMcpButton = new Button(parent, SWT.PUSH);
    extMcpButton.setText(Messages.preferences_page_extMcp_button_edit);
    extMcpButton.setToolTipText(Messages.preferences_page_extMcp_button_tooltip);
    extMcpButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    extMcpButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String res = CopilotUi.getPlugin().getChatServiceManager().getMcpExtensionPointManager()
            .approveExtMcpRegistration();
        if (StringUtils.isNotBlank(res)) {
          CopilotUi.getPlugin().getLanguageServerSettingManager().syncMcpRegistrationConfiguration();
        }
      }
    });
  }

  /**
   * Dispose the notice icon when for example, user has made actions to the registration.
   */
  public void disposeNoticeIcon() {
    if (extMcpTitleComposite == null || extMcpTitleComposite.isDisposed()) {
      return; // nothing to update
    }

    if (redNoticeLabel != null && !redNoticeLabel.isDisposed()) {
      redNoticeLabel.setImage(null); // detach before dispose
      redNoticeLabel.dispose();
      redNoticeLabel = null;
    }

    if (redNotice != null && !redNotice.isDisposed()) {
      redNotice.dispose();
      redNotice = null;
    }

    // Refresh layout to reflect changes immediately
    extMcpTitleComposite.requestLayout();
  }

  /**
   * Updates the registry button state based on the current field value.
   */
  private void updateRegistryButtonState() {
    if (openRegistryButton != null && !openRegistryButton.isDisposed() && mcpRegistryField != null) {
      boolean hasContent = StringUtils.isNotBlank(mcpRegistryField.getStringValue());
      openRegistryButton.setEnabled(hasContent);
    }
  }

  private boolean validateMcpField(StringFieldEditor mcpField) {
    String stringValue = mcpField.getStringValue();
    if (StringUtils.isBlank(stringValue)) {
      return true;
    }

    try {
      // First check for basic JSON syntax using GSON parser
      GSON.fromJson(stringValue, Object.class);

      // Second check for duplicate keys in the JSON
      try (JsonReader reader = new JsonReader(new StringReader(stringValue))) {
        // Configure the reader to be lenient using version-appropriate method
        configureLenientJsonReader(reader);
        return validateDuplicateKeys(mcpField, reader);
      }
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

  /**
   * Recursively checks for duplicate keys in a JSON structure.
   */
  private boolean validateDuplicateKeys(StringFieldEditor mcpField, JsonReader reader) throws IOException {
    JsonToken token = reader.peek();

    switch (token) {
      case BEGIN_OBJECT:
        reader.beginObject();
        Set<String> objectKeys = new HashSet<>();

        while (reader.hasNext()) {
          String key = reader.nextName();
          if (!objectKeys.add(key)) {
            mcpField.setErrorMessage("Error: Duplicate key '" + key + "' found in JSON object");
            return false;
          }

          if (!validateDuplicateKeys(mcpField, reader)) {
            return false;
          }
        }

        reader.endObject();
        break;

      case BEGIN_ARRAY:
        reader.beginArray();

        while (reader.hasNext()) {
          if (!validateDuplicateKeys(mcpField, reader)) {
            return false;
          }
        }

        reader.endArray();
        break;

      case STRING:
        reader.nextString();
        break;

      case NUMBER:
        reader.nextDouble();
        break;

      case BOOLEAN:
        reader.nextBoolean();
        break;

      case NULL:
        reader.nextNull();
        break;

      default:
        reader.skipValue();
    }

    return true;
  }

  private String getServerRunningStatusHint(McpServerToolsCollection server) {
    switch (server.getStatus()) {
      case running:
      case blocked:
        return StringUtils.isNotBlank(server.getRegistryInfo()) ? " - " + server.getRegistryInfo() : StringUtils.EMPTY;
      case stopped:
        return StringUtils.EMPTY;
      case error:
        return " " + Messages.preferences_page_mcp_server_init_error;
      default:
        return StringUtils.EMPTY;
    }
  }

  /**
   * Updates the UI based on the MCP enabled setting.
   *
   * @param mcpEnabled true if MCP is enabled, false otherwise
   */
  public void updateMcpPreferencePage(Boolean mcpEnabled) {
    if (mcpEnabled) {
      return;
    }

    if (mcpGroup != null && !mcpGroup.isDisposed()) {
      mcpGroup.dispose();
      mcpGroup = null;
    }

    if (toolsGroup != null && !toolsGroup.isDisposed()) {
      toolsGroup.dispose();
      toolsGroup = null;
    }
  }

  /**
   * Displays the server names and tool names in the tools group using a tree view.
   */
  public void displayServerToolsInfo(List<McpServerToolsCollection> servers) {
    if (toolsGroup == null || toolsGroup.isDisposed()) {
      return;
    }

    // Clear existing children except mode selector composite
    for (var child : toolsGroup.getChildren()) {
      if (child != null && !child.isDisposed() && child != modeSelectorComposite) {
        child.dispose();
      }
    }

    // Create a new Tree widget with checkboxes
    toolsTree = new Tree(toolsGroup, SWT.SINGLE | SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL);
    GridData treeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    toolsTree.setLayoutData(treeGridData);

    final Map<String, Map<String, Boolean>> savedServerToolStatusMap = modeToolStatus.get(currentModeId) != null
        ? modeToolStatus.get(currentModeId)
        : new HashMap<>();

    // Fetch built-in tools from AgentToolService
    // Get cached built-in tools that were registered at initialization
    try {
      var chatServiceManager = CopilotUi.getPlugin().getChatServiceManager();
      if (chatServiceManager != null) {
        var agentToolService = chatServiceManager.getAgentToolService();
        if (agentToolService != null) {
          var builtInTools = agentToolService.getBuiltInTools();
          if (!builtInTools.isEmpty()) {
            addBuiltInToolsToTree(builtInTools, savedServerToolStatusMap);
          }
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to fetch built-in tools", e);
    }

    // Add MCP servers and tools to the tree
    for (McpServerToolsCollection server : servers) {
      if (server == null) {
        continue;
      }

      TreeItem serverNode = new TreeItem(toolsTree, SWT.NONE);
      serverNode.setText(server.getName() + getServerRunningStatusHint(server));
      boolean isBlocked = server.getStatus() == McpServerStatus.blocked;

      // Store blocked status in the tree item data for disabled reference
      serverNode.setData("blocked", isBlocked);

      if (isBlocked) {
        serverNode.setGrayed(true);
        serverNode.setChecked(false);
      }

      for (LanguageModelToolInformation tool : server.getTools()) {
        if (tool == null) {
          continue;
        }

        TreeItem toolNode = new TreeItem(serverNode, SWT.NONE);
        toolNode.setText(tool.getName() + " - " + tool.getDescription());

        // For agent mode, default new tools to enabled; for custom modes default to disabled
        boolean shouldEnable = false;
        if ("agent-mode".equals(currentModeId)) {
          // Check if this tool exists in saved status
          if (savedServerToolStatusMap.containsKey(server.getName())
              && savedServerToolStatusMap.get(server.getName()).containsKey(tool.getName())) {
            shouldEnable = savedServerToolStatusMap.get(server.getName()).get(tool.getName());
          } else {
            shouldEnable = true; // New tool in agent mode - auto-enable
          }
        } else {
          // For custom modes, use saved status or default to false
          if (savedServerToolStatusMap.containsKey(server.getName())
              && savedServerToolStatusMap.get(server.getName()).containsKey(tool.getName())) {
            shouldEnable = savedServerToolStatusMap.get(server.getName()).get(tool.getName());
          }
        }

        toolNode.setChecked(isBlocked ? false : shouldEnable);
        toolNode.setData("toolName", tool.getName());
        toolNode.setData("blocked", isBlocked);

        if (isBlocked) {
          toolNode.setGrayed(true);
        }
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

          // Check if the item is blocked - if so, prevent the action
          boolean isBlocked = (Boolean) item.getData("blocked");
          if (isBlocked) {
            // Revert the check state change for blocked items
            item.setChecked(false);
            return;
          }

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
      hasFailedMcpServer = true;
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
        result = GSON.fromJson(jsonStatus, new com.google.gson.reflect.TypeToken<Map<String, Map<String, Boolean>>>() {
        }.getType());
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
        String toolName = (String) toolNode.getData("toolName");
        if (StringUtils.isNotBlank(serverName)) {
          toolStatus.put(toolName, toolNode.getChecked());
        }
      }
      serverToolStatus.put(serverName, toolStatus);
    }

    String jsonResult = GSON.toJson(serverToolStatus);
    IPreferenceStore preferenceStore = getPreferenceStore();
    preferenceStore.setValue(Constants.MCP_TOOLS_STATUS, jsonResult);
  }

  /**
   * Resynchronizes MCP servers when there are failed server instances. This method is specifically designed to handle
   * cases where the MCP field value remains unchanged but server synchronization is still required. When the field
   * value doesn't change, the normal property change event mechanism is not triggered, so this method provides an
   * alternative way to force server re-synchronization.
   */
  private void resyncMcpServers() {
    if (!hasFailedMcpServer) {
      return;
    }
    hasFailedMcpServer = false;

    IPreferenceStore preferenceStore = getPreferenceStore();
    String storedMcp = preferenceStore.getString(Constants.MCP);
    String currentMcp = mcpField.getStringValue();
    if (StringUtils.equals(currentMcp, storedMcp)) {
      CopilotUi.getPlugin().getLanguageServerSettingManager().syncMcpRegistrationConfiguration();
    }
  }

  @Override
  public boolean performOk() {
    // Save current mode's tool status before saving all
    saveModeToolStatus(currentModeId);
    // Only save to MCP_TOOLS_STATUS when in agent mode (for backward compatibility)
    if ("agent-mode".equals(currentModeId)) {
      saveToolStatusToPreferences();
    }
    saveModeToolStatusToPreferences();
    if (PlatformUtils.isNightly()) {
      saveMcpRegistryUrlToGlobalScope();
    }
    resyncMcpServers();

    // Update LSP with tool status for all modes
    updateAllModesToolStatus();

    return super.performOk();
  }

  /**
   * Update tool status for all modes (agent mode and custom modes) via LSP.
   */
  private void updateAllModesToolStatus() {
    LanguageServerSettingManager lsManager = CopilotUi.getPlugin().getLanguageServerSettingManager();

    // Update tool status for each mode
    for (Map.Entry<String, Map<String, Map<String, Boolean>>> modeEntry : modeToolStatus.entrySet()) {
      String modeId = modeEntry.getKey();
      Map<String, Map<String, Boolean>> toolStatus = modeEntry.getValue();

      // Convert to JSON format for LSP
      String toolStatusJson = GSON.toJson(toolStatus);

      // Update via LSP with mode context
      lsManager.updateToolStatusForMode(toolStatusJson, modeId);
    }
  }

  /**
   * Create mode selector dropdown for selecting which mode to configure tools for.
   */
  private void createModeSelector(Composite parent) {
    modeSelectorComposite = new Composite(parent, SWT.NONE);
    GridLayout modeSelectorLayout = new GridLayout(2, false);
    modeSelectorLayout.marginWidth = 0;
    modeSelectorLayout.marginHeight = 5;
    modeSelectorComposite.setLayout(modeSelectorLayout);
    modeSelectorComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label label = new Label(modeSelectorComposite, SWT.NONE);
    label.setText(Messages.preferences_page_mcp_tools_mode_selector);
    label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

    modeSelector = new Combo(modeSelectorComposite, SWT.READ_ONLY);
    modeSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    loadModeOptions();
    loadModeToolStatusFromPreferences();

    modeSelector.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onModeChanged();
      }
    });
  }

  /**
   * Load available modes into the mode selector.
   */
  private void loadModeOptions() {
    List<String> options = new ArrayList<>();
    options.add(Messages.preferences_page_mcp_tools_agent_mode);

    // Add custom modes
    try {
      List<CustomChatMode> customModes = CustomChatModeManager.INSTANCE.getCustomModes();
      for (CustomChatMode mode : customModes) {
        String workspaceName = getWorkspaceNameForMode(mode);
        String prefix = workspaceName.isEmpty() ? "Custom: " : workspaceName + ": ";
        options.add(prefix + mode.getDisplayName());
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to load custom modes", e);
    }

    modeSelector.setItems(options.toArray(new String[0]));

    // Select the current mode
    selectModeById(currentModeId);
  }

  /**
   * Select a mode by its ID in the mode selector.
   */
  private void selectModeById(String modeId) {
    if (modeSelector == null || modeSelector.isDisposed()) {
      return;
    }

    if ("agent-mode".equals(modeId)) {
      modeSelector.select(0);
    } else {
      // Find the custom mode
      try {
        List<CustomChatMode> customModes = CustomChatModeManager.INSTANCE.getCustomModes();
        for (int i = 0; i < customModes.size(); i++) {
          if (customModes.get(i).getId().equals(modeId)) {
            modeSelector.select(i + 1); // +1 because agent mode is at index 0
            currentModeId = modeId;
            // Load the tool status for this mode
            if (toolsTree != null && !toolsTree.isDisposed()) {
              loadModeToolStatus(modeId);
            }
            return;
          }
        }
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to select mode by ID: " + modeId, e);
      }

      // If mode not found, default to agent mode
      modeSelector.select(0);
      currentModeId = "agent-mode";
    }
  }

  /**
   * Handle mode selection change.
   */
  private void onModeChanged() {
    // Save current mode's tool status
    saveModeToolStatus(currentModeId);

    // Load new mode's tool status
    String selectedText = modeSelector.getText();
    currentModeId = extractModeIdFromSelection(selectedText);
    loadModeToolStatus(currentModeId);
  }

  /**
   * Extract mode ID from selection text.
   */
  private String extractModeIdFromSelection(String selectionText) {
    if (selectionText.equals(Messages.preferences_page_mcp_tools_agent_mode)) {
      return "agent-mode";
    } else {
      // Extract display name by removing workspace prefix or "Custom: " prefix
      String displayName;
      if (selectionText.contains("/")) {
        // Format: "workspace/displayName"
        displayName = selectionText.substring(selectionText.indexOf("/") + 1);
      } else if (selectionText.startsWith("Custom: ")) {
        // Format: "Custom: displayName"
        displayName = selectionText.substring(8);
      } else {
        displayName = selectionText;
      }
      
      try {
        List<CustomChatMode> customModes = CustomChatModeManager.INSTANCE.getCustomModes();
        for (CustomChatMode mode : customModes) {
          if (mode.getDisplayName().equals(displayName)) {
            return mode.getId();
          }
        }
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to extract mode ID", e);
      }
    }
    return "agent-mode";
  }

  /**
   * Get the workspace name for a custom mode based on its file path.
   */
  private String getWorkspaceNameForMode(CustomChatMode mode) {
    try {
      String modeId = mode.getId();
      Path modePath = Paths.get(java.net.URI.create(modeId));
      
      List<WorkspaceFolder> workspaceFolders = LSPEclipseUtils.getWorkspaceFolders();
      if (workspaceFolders != null) {
        for (WorkspaceFolder folder : workspaceFolders) {
          Path folderPath = Paths.get(java.net.URI.create(folder.getUri()));
          if (modePath.startsWith(folderPath)) {
            return folder.getName();
          }
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to get workspace name for mode", e);
    }
    return "";
  }

  /**
   * Load per-mode tool status from preferences.
   */
  private void loadModeToolStatusFromPreferences() {
    IPreferenceStore preferenceStore = getPreferenceStore();
    String json = preferenceStore.getString(Constants.MCP_TOOLS_MODE_STATUS);

    if (StringUtils.isNotBlank(json)) {
      try {
        modeToolStatus = GSON.fromJson(json,
            new com.google.gson.reflect.TypeToken<Map<String, Map<String, Map<String, Boolean>>>>() {
            }.getType());
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to parse MCP mode tools status JSON", e);
        modeToolStatus = new HashMap<>();
      }
    }

    // Initialize mode tool status from custom mode definitions loaded from LSP
    initializeModeToolStatusFromCustomModes();
  }

  /**
   * Initialize mode tool status for custom modes based on their .agent.md file definitions. This ensures that tools
   * defined in the .agent.md files are properly synced to the preference page.
   */
  private void initializeModeToolStatusFromCustomModes() {
    try {
      List<CustomChatMode> customModes = CustomChatModeManager.INSTANCE.getCustomModes();

      // Collect all valid mode IDs (agent-mode + custom modes)
      Set<String> validModeIds = new HashSet<>();
      validModeIds.add("agent-mode"); // Always keep agent-mode

      for (CustomChatMode mode : customModes) {
        validModeIds.add(mode.getId());
      }

      // Remove tool status for non-existing modes
      modeToolStatus.keySet().removeIf(modeId -> !validModeIds.contains(modeId));

      for (CustomChatMode mode : customModes) {
        String modeId = mode.getId();
        List<String> toolsFromFile = mode.getTools();

        if (toolsFromFile == null || toolsFromFile.isEmpty()) {
          continue; // No tools defined in this mode
        }

        // Check if we already have preferences saved for this mode
        Map<String, Map<String, Boolean>> existingModeStatus = modeToolStatus.get(modeId);
        if (existingModeStatus != null) {
          // We have saved preferences, but we need to ensure tools from the .agent.md file are included
          // Mark tools from .agent.md as enabled if not already present in preferences
          for (String toolSpec : toolsFromFile) {
            String serverName;
            String toolName;

            // Parse tool specification: either "tool" or "server/tool"
            if (toolSpec.contains("/")) {
              String[] parts = toolSpec.split("/", 2);
              serverName = parts[0];
              toolName = parts[1];
            } else {
              // Built-in tool
              serverName = Messages.preferences_page_mcp_tools_builtin;
              toolName = toolSpec;
            }

            // Get or create server map
            Map<String, Boolean> serverTools = existingModeStatus.computeIfAbsent(serverName, k -> new HashMap<>());

            // Only add if not already present (respect existing preferences)
            if (!serverTools.containsKey(toolName)) {
              serverTools.put(toolName, true); // Enable tools from .agent.md by default
            }
          }
        } else {
          // No saved preferences for this mode - initialize from .agent.md file
          Map<String, Map<String, Boolean>> newModeStatus = new HashMap<>();

          for (String toolSpec : toolsFromFile) {
            String serverName;
            String toolName;

            // Parse tool specification: either "tool" or "server/tool"
            if (toolSpec.contains("/")) {
              String[] parts = toolSpec.split("/", 2);
              serverName = parts[0];
              toolName = parts[1];
            } else {
              // Built-in tool
              serverName = Messages.preferences_page_mcp_tools_builtin;
              toolName = toolSpec;
            }

            // Get or create server map
            Map<String, Boolean> serverTools = newModeStatus.computeIfAbsent(serverName, k -> new HashMap<>());
            serverTools.put(toolName, true); // Enable tools from .agent.md
          }

          modeToolStatus.put(modeId, newModeStatus);
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to initialize mode tool status from custom modes", e);
    }
  }

  /**
   * Save per-mode tool status to preferences.
   */
  private void saveModeToolStatusToPreferences() {
    String json = GSON.toJson(modeToolStatus);
    IPreferenceStore preferenceStore = getPreferenceStore();
    preferenceStore.setValue(Constants.MCP_TOOLS_MODE_STATUS, json);
  }

  /**
   * Save current tool tree state to the specified mode.
   */
  private void saveModeToolStatus(String modeId) {
    if (toolsTree == null || toolsTree.isDisposed()) {
      return;
    }

    Map<String, Map<String, Boolean>> serverToolStatus = new HashMap<>();
    for (TreeItem serverNode : toolsTree.getItems()) {
      String serverName = extractServerName(serverNode.getText());
      Map<String, Boolean> toolStatus = new HashMap<>();
      for (TreeItem toolNode : serverNode.getItems()) {
        String toolName = extractToolName(toolNode.getText());
        toolStatus.put(toolName, toolNode.getChecked());
      }
      serverToolStatus.put(serverName, toolStatus);
    }

    modeToolStatus.put(modeId, serverToolStatus);
  }

  /**
   * Load tool tree state from the specified mode.
   */
  private void loadModeToolStatus(String modeId) {
    if (toolsTree == null || toolsTree.isDisposed()) {
      return;
    }

    Map<String, Map<String, Boolean>> serverToolStatus = modeToolStatus.get(modeId);
    if (serverToolStatus == null) {
      // No saved status for this mode - default all tools to checked for agent-mode
      if ("agent-mode".equals(modeId)) {
        for (TreeItem serverNode : toolsTree.getItems()) {
          updateToolsCheckStatus(serverNode);
          serverNode.setChecked(true);
          updateToolsCheckStatus(serverNode);
        }
      } else {
        // For custom modes, default to unchecked
        for (TreeItem serverNode : toolsTree.getItems()) {
          for (TreeItem toolNode : serverNode.getItems()) {
            toolNode.setChecked(false);
          }
          updateServerCheckStatus(serverNode);
        }
      }
      return;
    }

    // Load saved status
    for (TreeItem serverNode : toolsTree.getItems()) {
      String serverName = extractServerName(serverNode.getText());
      Map<String, Boolean> toolStatus = serverToolStatus.get(serverName);

      for (TreeItem toolNode : serverNode.getItems()) {
        String toolName = extractToolName(toolNode.getText());
        boolean isChecked = toolStatus != null ? toolStatus.getOrDefault(toolName, false) : false;
        toolNode.setChecked(isChecked);
      }

      updateServerCheckStatus(serverNode);
    }
  }

  /**
   * Extract server name from tree item text (removes hints like " - registry info").
   */
  private String extractServerName(String text) {
    int dashIndex = text.indexOf(" - ");
    return dashIndex > 0 ? text.substring(0, dashIndex).trim() : text.trim();
  }

  /**
   * Extract tool name from tree item text (removes description after " - ").
   */
  private String extractToolName(String text) {
    int dashIndex = text.indexOf(" - ");
    return dashIndex > 0 ? text.substring(0, dashIndex).trim() : text.trim();
  }

  /**
   * Saves the MCP Registry URL to global scope (ConfigurationScope) to persist across workspaces.
   */
  private void saveMcpRegistryUrlToGlobalScope() {
    // Get the current value from the text field instead of the preference store
    String newMcpRegistryUrl = mcpRegistryField.getStringValue();
    String oldMcpRegistryUrl = CopilotUi.getStringPreference(Constants.MCP_REGISTRY_URL,
        CopilotPreferenceInitializer.DEFAULT_MCP_REGISTRY_URL);

    // Ensure the preference change is updated in configuration scope too
    if (!oldMcpRegistryUrl.equals(newMcpRegistryUrl)) {
      try {
        IEclipsePreferences configPrefs = ConfigurationScope.INSTANCE
            .getNode(CopilotUi.getPlugin().getBundle().getSymbolicName());
        configPrefs.put(Constants.MCP_REGISTRY_URL, newMcpRegistryUrl);
        configPrefs.flush();
      } catch (BackingStoreException ex) {
        CopilotCore.LOGGER.error("Failed to persist MCP Registry URL preference in ConfigurationScope", ex);
      }
    }
  }

  /**
   * Configures a JsonReader to be lenient using reflection to handle different Gson versions. Tries to use
   * setStrictness(Strictness.LENIENT) for newer Gson versions, falls back to setLenient(true) for older versions.
   */
  private void configureLenientJsonReader(JsonReader reader) {
    try {
      // Load Strictness enum class dynamically
      Class<?> strictnessClass = Class.forName("com.google.gson.Strictness");
      Object lenientValue = null;

      // Get the LENIENT enum value
      for (Object enumConstant : strictnessClass.getEnumConstants()) {
        if ("LENIENT".equals(enumConstant.toString())) {
          lenientValue = enumConstant;
          break;
        }
      }

      // Get setStrictness method and invoke it
      if (lenientValue != null) {
        Method setStrictnessMethod = JsonReader.class.getMethod("setStrictness", strictnessClass);
        setStrictnessMethod.invoke(reader, lenientValue);
      }
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      reader.setLenient(true); // Fallback to older API
    }
  }

  /**
   * Adds built-in tools to the tree widget.
   *
   * @param builtInTools The list of built-in tools from the language server
   * @param savedServerToolStatusMap The saved tool status map for the current mode
   */
  private void addBuiltInToolsToTree(List<LanguageModelToolInformation> builtInTools,
      Map<String, Map<String, Boolean>> savedServerToolStatusMap) {
    TreeItem builtInServerNode = new TreeItem(toolsTree, SWT.NONE, 0);
    builtInServerNode.setText(Messages.preferences_page_mcp_tools_builtin);
    builtInServerNode.setData("blocked", false);

    for (var tool : builtInTools) {
      if (tool == null) {
        continue;
      }

      TreeItem toolNode = new TreeItem(builtInServerNode, SWT.NONE);
      toolNode.setText(tool.getName() + " - " + tool.getDescription());
      toolNode.setData("blocked", false);
      toolNode.setData("toolName", tool.getName());

      // For agent mode, default new tools to enabled; for custom modes default to disabled
      boolean shouldEnable = false;
      if ("agent-mode".equals(currentModeId)) {
        // Check if this tool exists in saved status
        if (savedServerToolStatusMap.containsKey(Messages.preferences_page_mcp_tools_builtin)
            && savedServerToolStatusMap.get(Messages.preferences_page_mcp_tools_builtin).containsKey(tool.getName())) {
          shouldEnable = savedServerToolStatusMap.get(Messages.preferences_page_mcp_tools_builtin).get(tool.getName());
        } else {
          shouldEnable = true; // New tool in agent mode - auto-enable
        }
      } else {
        // For custom modes, use saved status or default to false
        if (savedServerToolStatusMap.containsKey(Messages.preferences_page_mcp_tools_builtin)
            && savedServerToolStatusMap.get(Messages.preferences_page_mcp_tools_builtin).containsKey(tool.getName())) {
          shouldEnable = savedServerToolStatusMap.get(Messages.preferences_page_mcp_tools_builtin).get(tool.getName());
        }
      }

      toolNode.setChecked(shouldEnable);
    }

    builtInServerNode.setExpanded(true);
    updateServerCheckStatus(builtInServerNode);
    toolsGroup.requestLayout();
  }

  /**
   * Finds an existing MCP Registry dialog if opened, used to avoid opening multiple dialogs.
   *
   * @return the existing dialog shell, or null if not found
   */
  private Shell findExistingMcpRegistryDialog() {
    Shell[] shells = Display.getDefault().getShells();
    for (Shell shell : shells) {
      if (shell.getText().equals(com.microsoft.copilot.eclipse.ui.dialogs.Messages.mcpRegistryDialog_title)) {
        return shell;
      }
    }
    return null;
  }
}

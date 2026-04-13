package com.microsoft.copilot.eclipse.ui.dialogs.mcp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.McpUtils;

/**
 * Manager class for handling MCP server installation operations and state management using IEventBroker.
 */
public class McpServerInstallManager {
  // Event data keys
  public static final String EVENT_DATA_SERVER_NAME = "serverName";
  public static final String EVENT_DATA_ACTION_TYPE = "actionType";
  public static final String EVENT_DATA_ACTION_RESULT = "actionResult";
  public static final String EVENT_DATA_CONFIG = "config";

  /**
   * Enum for action types.
   */
  public enum ActionType {
    INSTALL, UNINSTALL
  }

  /**
   * Enum for action results.
   */
  public enum ActionResult {
    IN_PROGRESS, SUCCESS, FAILURE
  }

  private IPreferenceStore store;
  private String installedMcps;
  private Set<String> installedMcpServerIdentities;
  private final Object lock = new Object();

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final String SERVER_KEY = "servers";
  private static final String IDENTITY_SEPARATOR = "|";

  /**
   * Creates a new MCP server install manager.
   */
  public McpServerInstallManager() {
    this.store = CopilotUi.getPlugin().getPreferenceStore();
    this.installedMcps = store.getString(Constants.MCP);
    initializeInstalledMcpServerIdentities();
  }

  /**
   * Initializes the set of installed server identities from current MCP configuration. Each identity is stored as
   * "baseUrl|serverId".
   */
  private void initializeInstalledMcpServerIdentities() {
    this.installedMcpServerIdentities = new HashSet<>();

    if (StringUtils.isBlank(installedMcps)) {
      return;
    }

    try {
      JsonObject mcpObject = JsonParser.parseString(installedMcps).getAsJsonObject();
      if (!mcpObject.has(SERVER_KEY) || !mcpObject.get(SERVER_KEY).isJsonObject()) {
        return;
      }

      JsonObject serversObject = mcpObject.getAsJsonObject(SERVER_KEY);

      for (String serverName : serversObject.keySet()) {
        JsonElement serverElement = serversObject.get(serverName);
        if (serverElement.isJsonObject()) {
          JsonObject serverConfig = serverElement.getAsJsonObject();
          String registryBaseUrl = getBaseUrlFromLocalConfig(serverConfig);
          String mcpServerName = getMcpServerNameFromLocalConfig(serverConfig);
          String identity = createRegistryServerKey(registryBaseUrl, mcpServerName);
          if (identity != null) {
            installedMcpServerIdentities.add(identity);
          }
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error initializing installed server identities", e);
    }
  }

  /**
   * Creates a server identity string from serverId and URL. Returns "registryBaseUrl|serverName" format.
   */
  private static String createRegistryServerKey(String registryBaseUrl, String serverName) {
    if (StringUtils.isBlank(registryBaseUrl) || StringUtils.isBlank(serverName)) {
      return null;
    }
    return registryBaseUrl + IDENTITY_SEPARATOR + serverName;
  }

  /**
   * Installs a server configuration using event-driven approach.
   */
  public void installServer(String serverName, JsonObject serverConfig) {
    // Check for server name conflict before proceeding
    if (hasServerNameConflict(serverName)) {
      // Show confirmation dialog on UI thread
      final boolean[] shouldProceed = new boolean[1];
      Display.getDefault().syncExec(() -> {
        String message = NLS.bind(Messages.mcpServerInstallManager_overrideServer_message, serverName);
        shouldProceed[0] = MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
            Messages.mcpServerInstallManager_overrideServer_title, message);
      });

      if (!shouldProceed[0]) {
        // User declined to override
        CopilotCore.LOGGER.info("User declined to override server: " + serverName);
        return;
      }
    }

    // Publish install start event
    publishServerStateChangeEvent(serverName, ActionType.INSTALL, ActionResult.IN_PROGRESS, serverConfig);

    Job installJob = new Job("Installing MCP Server: " + serverName) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        synchronized (lock) {
          try {
            monitor.beginTask("Installing " + serverName, 100);

            // Re-read current MCP config to get latest state
            installedMcps = store.getString(Constants.MCP);

            JsonObject mcpObject;
            if (StringUtils.isBlank(installedMcps)) {
              mcpObject = new JsonObject();
            } else {
              mcpObject = JsonParser.parseString(installedMcps).getAsJsonObject();
            }

            monitor.worked(30);

            // Ensure the servers object exists
            JsonObject serversObject;
            if (mcpObject.has("servers") && mcpObject.get("servers").isJsonObject()) {
              serversObject = mcpObject.getAsJsonObject("servers");
            } else {
              serversObject = new JsonObject();
              mcpObject.add("servers", serversObject);
            }

            // Add the new server configuration to the servers object (will override if exists)
            serversObject.add(serverName, serverConfig);
            monitor.worked(30);

            // Update the preference store and installedMcps
            String newMcpConfig = GSON.toJson(mcpObject);
            store.setValue(Constants.MCP, newMcpConfig);
            installedMcps = newMcpConfig;

            // Reinitialize the identity set with the new configuration
            initializeInstalledMcpServerIdentities();
            monitor.worked(10);

            // Publish install complete event
            publishServerStateChangeEvent(serverName, ActionType.INSTALL, ActionResult.SUCCESS, null);

            // Trigger MCP server synchronization to update the preference page
            triggerMcpServerSync(newMcpConfig);
            monitor.worked(30);
            monitor.done();

            return Status.OK_STATUS;

          } catch (Exception e) {
            CopilotCore.LOGGER.error("Error installing MCP server: " + serverName, e);
            publishServerStateChangeEvent(serverName, ActionType.INSTALL, ActionResult.FAILURE, null);
            return new Status(IStatus.ERROR, Constants.PLUGIN_ID, "Failed to install MCP server: " + serverName, e);
          }
        }
      }
    };

    installJob.setUser(true);
    installJob.schedule();
  }

  /**
   * Uninstalls a server configuration using event-driven approach.
   */
  public void uninstallServer(String serverName) {
    // Publish uninstall start event
    publishServerStateChangeEvent(serverName, ActionType.UNINSTALL, ActionResult.IN_PROGRESS, null);

    Job uninstallJob = new Job("Uninstalling MCP Server: " + serverName) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        synchronized (lock) {
          try {
            monitor.beginTask("Uninstalling " + serverName, 100);

            // Re-read installed MCP config to get latest state
            installedMcps = store.getString(Constants.MCP);

            if (StringUtils.isBlank(installedMcps)) {
              // Nothing to uninstall
              publishServerStateChangeEvent(serverName, ActionType.UNINSTALL, ActionResult.SUCCESS, null);
              return Status.OK_STATUS;
            }

            monitor.worked(30);

            JsonObject mcpObject = JsonParser.parseString(installedMcps).getAsJsonObject();

            monitor.worked(30);

            // Remove the server configuration from the servers object
            if (mcpObject.has("servers") && mcpObject.get("servers").isJsonObject()) {
              JsonObject serversObject = mcpObject.getAsJsonObject("servers");
              serversObject.remove(serverName);
            }

            monitor.worked(10);

            // Update the preference store and installedMcps
            String newMcpConfig = GSON.toJson(mcpObject);
            store.setValue(Constants.MCP, newMcpConfig);
            installedMcps = newMcpConfig;

            // Reinitialize the identity set with the new configuration
            initializeInstalledMcpServerIdentities();
            publishServerStateChangeEvent(serverName, ActionType.UNINSTALL, ActionResult.SUCCESS, null);

            // Trigger MCP server synchronization to update the preference page
            triggerMcpServerSync(newMcpConfig);
            monitor.worked(30);
            monitor.done();
            return Status.OK_STATUS;
          } catch (Exception e) {
            CopilotCore.LOGGER.error("Error uninstalling MCP server: " + serverName, e);
            publishServerStateChangeEvent(serverName, ActionType.UNINSTALL, ActionResult.FAILURE, null);
            return new Status(IStatus.ERROR, Constants.PLUGIN_ID, "Failed to uninstall MCP server: " + serverName, e);
          }
        }
      }
    };

    uninstallJob.setUser(true);
    uninstallJob.schedule();
  }

  /**
   * Publishes a server state change event.
   */
  private void publishServerStateChangeEvent(String serverName, ActionType actionType, ActionResult actionResult,
      JsonObject config) {
    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      Map<String, Object> eventData = new HashMap<>();
      eventData.put(EVENT_DATA_SERVER_NAME, serverName);
      eventData.put(EVENT_DATA_ACTION_TYPE, actionType);
      eventData.put(EVENT_DATA_ACTION_RESULT, actionResult);

      if (config != null) {
        eventData.put(EVENT_DATA_CONFIG, config);
      }

      eventBroker.post(CopilotEventConstants.TOPIC_MCP_SERVER_STATE_CHANGE, eventData);
    }
  }

  /**
   * Triggers MCP server synchronization to update connected components like the preference page.
   */
  private void triggerMcpServerSync(String mcpConfig) {
    try {
      // Get the language server setting manager to trigger synchronization
      LanguageServerSettingManager mgr = CopilotUi.getPlugin().getLanguageServerSettingManager();
      if (mgr != null) {
        CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
        settings.setMcpServers(mcpConfig);
        mgr.syncSingleConfiguration(settings);
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error triggering MCP server synchronization", e);
    }
  }

  /**
   * Determines the initial state based on whether the server is installed.
   */
  public ButtonState getInitialState(String serverId, String url) {
    return isServerInstalled(serverId, url) ? ButtonState.UNINSTALL : ButtonState.INSTALL;
  }

  /**
   * Checks if a server is already installed by comparing baseUrl and serverId. Uses a pre-built set for O(1) lookup
   * performance.
   *
   * @param serverId The serverId to check
   * @param url The URL to check
   * @return true if a server with the same serverId and URL is already installed
   */
  public boolean isServerInstalled(String serverId, String url) {
    return installedMcpServerIdentities.contains(createRegistryServerKey(McpUtils.extractBaseUrl(url), serverId));
  }

  /**
   * Checks if there's a server with the same name but different serverId/URL.
   *
   * @param serverName The name of the server to check
   * @return true if a conflict exists
   */
  public boolean hasServerNameConflict(String serverName) {
    if (StringUtils.isBlank(installedMcps)) {
      return false;
    }

    try {
      JsonObject mcpObject = JsonParser.parseString(installedMcps).getAsJsonObject();

      if (!mcpObject.has(SERVER_KEY) || !mcpObject.get(SERVER_KEY).isJsonObject()) {
        return false;
      }

      JsonObject serversObject = mcpObject.getAsJsonObject(SERVER_KEY);
      return serversObject.keySet().contains(serverName);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error checking for server name conflict: " + serverName, e);
      return false;
    }
  }

  /**
   * Gets the baseUrl from server configuration's x-metadata.registry.api.baseUrl.
   */
  private static String getBaseUrlFromLocalConfig(JsonObject serverConfig) {
    JsonObject registry = getRegistryFromConfig(serverConfig);
    if (registry != null && registry.has("api") && registry.get("api").isJsonObject()) {
      JsonObject api = registry.getAsJsonObject("api");
      if (api.has("baseUrl")) {
        return api.get("baseUrl").getAsString();
      }
    }
    return null;
  }

  /**
   * Gets the MCP server name from server configuration's x-metadata.registry.mcpServer.name.
   */
  private static String getMcpServerNameFromLocalConfig(JsonObject serverConfig) {
    JsonObject registry = getRegistryFromConfig(serverConfig);
    if (registry != null && registry.has("mcpServer") && registry.get("mcpServer").isJsonObject()) {
      JsonObject mcpServer = registry.getAsJsonObject("mcpServer");
      if (mcpServer.has("name")) {
        return mcpServer.get("name").getAsString();
      }
    }
    return null;
  }

  /**
   * Extracts the registry JsonObject from server configuration's x-metadata.registry.
   *
   * @param serverConfig The server configuration JsonObject
   * @return The registry JsonObject, or null if not found
   */
  private static JsonObject getRegistryFromConfig(JsonObject serverConfig) {
    try {
      if (serverConfig.has("x-metadata") && serverConfig.get("x-metadata").isJsonObject()) {
        JsonObject metadata = serverConfig.getAsJsonObject("x-metadata");
        if (metadata.has("registry") && metadata.get("registry").isJsonObject()) {
          return metadata.getAsJsonObject("registry");
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error getting registry from config", e);
    }
    return null;
  }

  /**
   * Enum representing button states.
   */
  public enum ButtonState {
    INSTALL("Install"), INSTALLING("Installing"), UNINSTALL("Uninstall"), UNINSTALLING("Uninstalling");

    private final String text;

    ButtonState(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }
  }

  /**
   * Maps an installation action + result pair into a {@link ButtonState}.
   *
   * @param actionType the action type
   * @param actionResult the action result
   * @return the resulting button state
   */
  public static ButtonState determineButtonState(ActionType actionType, ActionResult actionResult) {
    if (ActionType.INSTALL == actionType) {
      switch (actionResult) {
        case IN_PROGRESS:
          return ButtonState.INSTALLING;
        case SUCCESS:
          return ButtonState.UNINSTALL;
        case FAILURE:
        default:
          return ButtonState.INSTALL;
      }
    } else if (ActionType.UNINSTALL == actionType) {
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
}

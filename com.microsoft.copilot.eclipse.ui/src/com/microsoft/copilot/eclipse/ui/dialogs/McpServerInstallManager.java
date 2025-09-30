package com.microsoft.copilot.eclipse.ui.dialogs;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

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
  private String currentMcp;
  private final Object lock = new Object();

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final String SERVER_KEY = "servers";

  /**
   * Creates a new MCP server install manager.
   */
  public McpServerInstallManager() {
    this.store = CopilotUi.getPlugin().getPreferenceStore();
    this.currentMcp = store.getString(Constants.MCP);
  }

  /**
   * Installs a server configuration using event-driven approach.
   */
  public void installServer(String serverName, JsonObject serverConfig) {
    // Publish install start event
    publishServerStateChangeEvent(serverName, ActionType.INSTALL, ActionResult.IN_PROGRESS, serverConfig);

    Job installJob = new Job("Installing MCP Server: " + serverName) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        synchronized (lock) {
          try {
            monitor.beginTask("Installing " + serverName, 100);

            // Re-read current MCP config to get latest state
            currentMcp = store.getString(Constants.MCP);

            JsonObject mcpObject;
            if (StringUtils.isBlank(currentMcp)) {
              mcpObject = new JsonObject();
            } else {
              mcpObject = JsonParser.parseString(currentMcp).getAsJsonObject();
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

            // Add the new server configuration to the servers object
            serversObject.add(serverName, serverConfig);
            monitor.worked(30);

            // Update the preference store and currentMcp
            String newMcpConfig = GSON.toJson(mcpObject);
            store.setValue(Constants.MCP, newMcpConfig);
            currentMcp = newMcpConfig;
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

            // Re-read current MCP config to get latest state
            currentMcp = store.getString(Constants.MCP);

            if (StringUtils.isBlank(currentMcp)) {
              // Nothing to uninstall
              publishServerStateChangeEvent(serverName, ActionType.UNINSTALL, ActionResult.SUCCESS, null);
              return Status.OK_STATUS;
            }

            monitor.worked(30);

            JsonObject mcpObject = JsonParser.parseString(currentMcp).getAsJsonObject();

            monitor.worked(30);

            // Remove the server configuration from the servers object
            if (mcpObject.has("servers") && mcpObject.get("servers").isJsonObject()) {
              JsonObject serversObject = mcpObject.getAsJsonObject("servers");
              serversObject.remove(serverName);
            }

            monitor.worked(10);

            // Update the preference store and currentMcp
            String newMcpConfig = GSON.toJson(mcpObject);
            store.setValue(Constants.MCP, newMcpConfig);
            currentMcp = newMcpConfig;
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
  public static ButtonState getInitialState(String serverName) {
    return isServerInstalled(serverName) ? ButtonState.UNINSTALL : ButtonState.INSTALL;
  }

  /**
   * Checks if a server is already installed.
   */
  public static boolean isServerInstalled(String serverName) {
    IPreferenceStore store = CopilotUi.getPlugin().getPreferenceStore();
    String mcpConfig = store.getString(Constants.MCP);

    if (StringUtils.isBlank(mcpConfig)) {
      return false;
    }

    try {
      JsonObject mcpObject = JsonParser.parseString(mcpConfig).getAsJsonObject();
      // Check if server exists in the servers object
      if (mcpObject.has(SERVER_KEY) && mcpObject.get(SERVER_KEY).isJsonObject()) {
        JsonObject serversObject = mcpObject.getAsJsonObject(SERVER_KEY);
        return serversObject.has(serverName);
      }
      return false;
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error checking if server is installed: " + serverName, e);
      return false;
    }
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
}
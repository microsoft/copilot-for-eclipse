// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.dialogs.mcp;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpRegistryAllowList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.RegistryAccess;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ListServersParams;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Package;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Remote;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerDetail;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerResponse;
import com.microsoft.copilot.eclipse.ui.utils.McpUtils;

/**
 * Controller for MCP Registry dialog following MVC pattern. Handles all business logic, data operations, pagination,
 * and coordinates between views.
 */
public class McpRegistryController {
  public static final String MCP_REGISTRY_VERSION_SUFFIX = "/" + Constants.MCP_REGISTRY_VERSION + "/servers";

  private static final String LATEST = "latest";
  private static final int PAGE_SIZE = 30;

  // nextCursor semantics:
  // null - initial state (start of list, more data available)
  // "" - no more data available
  // other - cursor for next page
  private String nextCursor = null;

  private CompletableFuture<ServerList> currentLoadFuture;

  private McpRegistryAllowList mcpAllowList;
  private String mcpRegistryBaseUrl = "";
  private String mcpRegistryFullUrl = "";

  private final CopilotLanguageServerConnection copilotLanguageServerConnection;
  private final McpServerInstallManager mcpServerInstallManager;

  /**
   * Creates a new MCP registry controller.
   */
  public McpRegistryController() {
    this.copilotLanguageServerConnection = CopilotCore.getPlugin() != null
        ? CopilotCore.getPlugin().getCopilotLanguageServer()
        : null;
    this.mcpServerInstallManager = new McpServerInstallManager();
  }

  /**
   * Resets all pagination and data state.
   */
  public synchronized void resetState() {
    if (currentLoadFuture != null) {
      currentLoadFuture.cancel(true);
      currentLoadFuture = null;
    }
    nextCursor = null;
  }

  /**
   * Loads the MCP registry URL and allow list from the server.
   *
   * @return a future that completes when the URL is loaded
   */
  public CompletableFuture<Void> loadMcpRegistryAllowListAndUrl() {
    return McpUtils.getMcpAllowList(copilotLanguageServerConnection).thenAccept(allowList -> {
      this.mcpAllowList = allowList;
      this.mcpRegistryBaseUrl = McpUtils.parseMcpRegistryBaseUrlFromAllowList(allowList);
      this.mcpRegistryFullUrl = this.mcpRegistryBaseUrl + MCP_REGISTRY_VERSION_SUFFIX;
    });
  }

  /**
   * Loads a page of servers from the registry.
   *
   * <p>This method starts the server loading on a background thread via the language server connection. UI callers
   * should attach continuations using {@link CompletableFuture} and marshal any UI updates back to the SWT UI thread.
   *
   * @param searchText the search text to filter servers
   * @return future that completes with the server list response; completes exceptionally if an error occurs or an
   *     invalid response is returned
   */
  public synchronized CompletableFuture<ServerList> loadServers(String searchText) {
    if (copilotLanguageServerConnection == null) {
      return CompletableFuture.completedFuture(null);
    }
    // No more data if nextCursor is explicitly empty string.
    if (nextCursor != null && nextCursor.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    ListServersParams params = new ListServersParams(this.mcpRegistryFullUrl, nextCursor, PAGE_SIZE, searchText,
        LATEST);
    CompletableFuture<ServerList> future = copilotLanguageServerConnection.listMcpServers(params);
    currentLoadFuture = future;

    return future.whenComplete((serverList, throwable) -> handleServerListResult(future, serverList, throwable));
  }

  private synchronized void handleServerListResult(CompletableFuture<ServerList> sourceFuture, ServerList serverList,
      Throwable throwable) {
    if (sourceFuture.isCancelled() || sourceFuture != currentLoadFuture) {
      // Ignore results from cancelled or outdated futures.
      return;
    }

    if (throwable != null || serverList == null) {
      nextCursor = "";
      if (throwable != null) {
        throw new IllegalStateException(throwable.getMessage(), throwable);
      }
      String errorMessage = NLS.bind(Messages.mcpRegistryDialog_invalidResponse, mcpRegistryBaseUrl,
          Messages.mcpRegistryDialog_button_changeUrl);
      throw new IllegalStateException(errorMessage);
    }

    updatePaginationState(serverList);
  }

  /**
   * Updates pagination state from server response.
   */
  private synchronized void updatePaginationState(ServerList serverList) {
    if (serverList != null && serverList.metadata() != null) {
      String serverCursor = serverList.metadata().nextCursor();
      if (StringUtils.isEmpty(serverCursor)) {
        nextCursor = "";
      } else {
        nextCursor = serverCursor;
      }
    } else {
      nextCursor = "";
    }
  }

  /**
   * Installs a server with the given configuration.
   *
   * @param serverName the server name
   * @param config the server configuration
   */
  public void installServer(String serverName, JsonObject config) {
    mcpServerInstallManager.installServer(serverName, config);
  }

  /**
   * Uninstalls a server.
   *
   * @param serverName the server name
   */
  public void uninstallServer(String serverName) {
    mcpServerInstallManager.uninstallServer(serverName);
  }

  /**
   * Gets the initial button state for a server.
   *
   * @param serverId the server ID
   * @param url the registry URL
   * @return the initial button state
   */
  public McpServerInstallManager.ButtonState getInitialButtonState(String serverId, String url) {
    return mcpServerInstallManager.getInitialState(serverId, url);
  }

  /**
   * Checks if a server is installed.
   *
   * @param serverId the server ID
   * @param url the registry URL
   * @return true if installed
   */
  public boolean isServerInstalled(String serverId, String url) {
    return mcpServerInstallManager.isServerInstalled(serverId, url);
  }

  /**
   * Opens the server detail dialog for the given response.
   *
   * @param parentShell the parent shell
   * @param serverResponse the server response
   */
  public void openServerDetailDialog(Shell parentShell, ServerResponse serverResponse) {
    McpServerDetailDialog dialog = new McpServerDetailDialog(parentShell, serverResponse, mcpServerInstallManager,
        mcpRegistryBaseUrl);
    dialog.open();
  }

  /**
   * Creates a configuration JSON object for a remote server.
   *
   * @param remote the remote definition
   * @param serverDetail the server detail
   * @return the configuration JSON
   */
  public JsonObject createRemoteConfig(Remote remote, ServerDetail serverDetail) {
    return McpServerConfigurationBuilder.createRemoteServerConfiguration(remote, serverDetail, mcpRegistryBaseUrl);
  }

  /**
   * Creates a configuration JSON object for a package-based server.
   *
   * @param pkg the package definition
   * @param serverDetail the server detail
   * @return the configuration JSON
   */
  public JsonObject createPackageConfig(Package pkg, ServerDetail serverDetail) {
    return McpServerConfigurationBuilder.createPackageServerConfiguration(pkg, serverDetail, mcpRegistryBaseUrl);
  }

  // Getters

  /**
   * Gets the MCP registry base URL.
   *
   * @return the registry base URL
   */
  public String getMcpRegistryBaseUrl() {
    return mcpRegistryBaseUrl;
  }

  /**
   * Checks if there is more data to load.
   *
   * @return true if more data is available
   */
  public synchronized boolean hasMoreData() {
    if (nextCursor == null) {
      return true;
    }
    return !nextCursor.isEmpty();
  }

  /**
   * Checks if the current registry access mode is registry_only.
   *
   * @return true if registry access is registry_only
   */
  public boolean isRegistryOnlyMode() {
    if (mcpAllowList == null || mcpAllowList.getMcpRegistries().isEmpty()) {
      return false;
    }
    return mcpAllowList.getMcpRegistries().get(0).getRegistryAccess() == RegistryAccess.registry_only;
  }
}
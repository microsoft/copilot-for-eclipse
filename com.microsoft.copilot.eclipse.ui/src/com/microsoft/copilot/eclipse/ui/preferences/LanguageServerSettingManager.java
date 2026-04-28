// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.CustomChatModeManager;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsStatusCollection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpToolStatus;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpToolsStatusCollection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationToolStatus;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings.GitHubSettings;
import com.microsoft.copilot.eclipse.core.lsp.protocol.UpdateConversationToolsStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.UpdateMcpToolsStatusParams;
import com.microsoft.copilot.eclipse.core.utils.GsonUtils;
import com.microsoft.copilot.eclipse.core.utils.McpFileConfigService;
import com.microsoft.copilot.eclipse.core.utils.WorkspaceUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.McpExtensionPointManager;

/**
 * A class to manage the proxy service for the Copilot Language Server.
 */
public class LanguageServerSettingManager implements IProxyChangeListener, IPropertyChangeListener {
  IProxyService proxyService = null;
  CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
  CopilotLanguageServerConnection copilotLanguageServerConnection = null;
  IPreferenceStore preferenceStore;
  IProxyData proxyData = null;
  private IResourceChangeListener mcpFileChangeListener;

  /**
   * Gets the settings.
   *
   * @return the settings
   */
  public CopilotLanguageServerSettings getSettings() {
    return settings;
  }

  /**
   * Initializes the LanguageServerSettingManager.
   */
  public LanguageServerSettingManager(CopilotLanguageServerConnection conn, IProxyService proxyService,
      IPreferenceStore preferenceStore) {
    this.copilotLanguageServerConnection = conn;
    this.proxyService = proxyService;
    this.preferenceStore = preferenceStore;
    // add listeners
    proxyService.addProxyChangeListener(this);
    preferenceStore.addPropertyChangeListener(this);

    // load settings on init
    updateProxySettings();
    getSettings().setEnableAutoCompletions(preferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION));
    getSettings().getHttp().setProxyStrictSsl(preferenceStore.getBoolean(Constants.ENABLE_STRICT_SSL));
    getSettings().getHttp().setProxyKerberosServicePrincipal(preferenceStore.getString(Constants.PROXY_KERBEROS_SP));
    getSettings().getGithubEnterprise().setUri(preferenceStore.getString(Constants.GITHUB_ENTERPRISE));

    // agent related settings
    getSettings().getGithubSettings().getCopilotSettings().getAgent()
        .setAgentMaxRequests(preferenceStore.getInt(Constants.AGENT_MAX_REQUESTS));

    // Set workspace context instructions when it is enabled
    if (preferenceStore.getBoolean(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED)) {
      getSettings().getGithubSettings()
          .setWorkspaceCopilotInstructions(preferenceStore.getString(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE));
    } else {
      getSettings().getGithubSettings().setWorkspaceCopilotInstructions(null);
    }

    // Initialize the custom instructions git commit preference if not set
    getSettings().getGithubSettings()
        .setGitCommitCopilotInstructions(preferenceStore.getString(Constants.CUSTOM_INSTRUCTIONS_GIT_COMMIT));

    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    eventBroker.subscribe(CopilotEventConstants.TOPIC_DID_CHANGE_MCP_CONTRIBUTION_POINT_POLICY, event -> {
      Boolean enabled = (Boolean) event.getProperty(IEventBroker.DATA);
      if (!enabled.booleanValue()) {
        // if the MCP contribution point is enabled, the sync action will be triggered after user approval.
        syncMcpRegistrationConfiguration();
      }
    });

    // Watch for mcp.json file changes in workspace projects
    registerMcpFileChangeListener();
  }

  /**
   * A listener for the proxy service.
   */
  @Override
  public void proxyInfoChanged(IProxyChangeEvent event) {
    updateProxySettings();
    updateGithubPanicErrorReport();
    syncSingleConfiguration(new CopilotLanguageServerSettings(null, settings.getHttp(), null, null));
  }

  /**
   * A listener for the preferences.
   */
  @Override
  public void propertyChange(PropertyChangeEvent event) {
    CopilotLanguageServerSettings singleSetting;

    switch (event.getProperty()) {
      case Constants.AUTO_SHOW_COMPLETION:
        settings.setEnableAutoCompletions(preferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION));
        singleSetting = new CopilotLanguageServerSettings(settings.isEnableAutoCompletions(), null, null, null);
        break;
      case Constants.ENABLE_STRICT_SSL:
        settings.getHttp().setProxyStrictSsl(preferenceStore.getBoolean(Constants.ENABLE_STRICT_SSL));
        singleSetting = new CopilotLanguageServerSettings(null, settings.getHttp(), null, null);
        updateGithubPanicErrorReport();
        break;
      case Constants.PROXY_KERBEROS_SP:
        settings.getHttp().setProxyKerberosServicePrincipal(preferenceStore.getString(Constants.PROXY_KERBEROS_SP));
        singleSetting = new CopilotLanguageServerSettings(null, settings.getHttp(), null, null);
        break;
      case Constants.GITHUB_ENTERPRISE:
        settings.getGithubEnterprise().setUri(preferenceStore.getString(Constants.GITHUB_ENTERPRISE));
        singleSetting = new CopilotLanguageServerSettings(null, null, settings.getGithubEnterprise(), null);
        break;
      case Constants.MCP, Constants.MCP_EXTENSION_POINT_CONTRIB:
        syncMcpRegistrationConfiguration();
        return;
      case Constants.MCP_TOOLS_STATUS:
        updateMcpToolsStatus(preferenceStore.getString(Constants.MCP_TOOLS_STATUS), null);
        return;
      case Constants.CUSTOM_INSTRUCTIONS_WORKSPACE:
        settings.getGithubSettings()
            .setWorkspaceCopilotInstructions(preferenceStore.getString(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE));
        if (preferenceStore.getBoolean(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED)) {
          singleSetting = new CopilotLanguageServerSettings(null, null, null, settings.getGithubSettings());
          break;
        }
        return;
      case Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED:
        singleSetting = updateWorkspaceInstructionEnabled(
            preferenceStore.getBoolean(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED));
        break;
      case Constants.CUSTOM_INSTRUCTIONS_GIT_COMMIT:
        String gitCommitInstructions = preferenceStore.getString(Constants.CUSTOM_INSTRUCTIONS_GIT_COMMIT);
        settings.getGithubSettings().setGitCommitCopilotInstructions(gitCommitInstructions);
        singleSetting = new CopilotLanguageServerSettings(null, null, null, settings.getGithubSettings());
        break;
      case Constants.AGENT_MAX_REQUESTS:
        settings.getGithubSettings().getCopilotSettings().getAgent()
            .setAgentMaxRequests(preferenceStore.getInt(Constants.AGENT_MAX_REQUESTS));
        singleSetting = new CopilotLanguageServerSettings(null, null, null, settings.getGithubSettings());
        break;
      default:
        return;
    }

    syncSingleConfiguration(singleSetting);
  }

  /**
   * Synchronizes the configuration with the language server.
   */
  public void syncConfiguration() {
    DidChangeConfigurationParams params = new DidChangeConfigurationParams();
    params.setSettings(settings);
    updateGithubPanicErrorReport();
    this.copilotLanguageServerConnection.updateConfig(params);
  }

  /**
   * Synchronizes the configuration with the language server.
   */
  public void syncSingleConfiguration(CopilotLanguageServerSettings singleSetting) {
    DidChangeConfigurationParams params = new DidChangeConfigurationParams();
    params.setSettings(singleSetting);
    this.copilotLanguageServerConnection.updateConfig(params);
  }

  private void updateGithubPanicErrorReport() {
    CopilotCore copilotCore = CopilotCore.getPlugin();
    if (copilotCore != null && copilotCore.getGithubPanicErrorReport() != null) {
      copilotCore.getGithubPanicErrorReport().setProxyStrictSsl(settings.getHttp().isProxyStrictSsl());
      copilotCore.getGithubPanicErrorReport().setProxyData(proxyData);
    }
  }

  /**
   * Sync MCP registration from file-based configs, preference store and extension points.
   * Merge order: Global mcp.json < Project mcp.json < Preference store < Extension points. 
   */
  public void syncMcpRegistrationConfiguration() {
    String fileBasedServers = McpFileConfigService.loadAndMerge(getProjectRootPaths());
    if (StringUtils.isNotBlank(fileBasedServers)) {
      settings.setMcpServers(fileBasedServers);
    }
    settings.addMcpServers(preferenceStore.getString(Constants.MCP));

    if (CopilotCore.getPlugin().getFeatureFlags().isMcpContributionPointEnabled()) {
      McpExtensionPointManager mgr = CopilotUi.getPlugin().getChatServiceManager().getMcpExtensionPointManager();
      settings.addMcpServers(mgr.getApprovedExtMcpServers());
    }

    syncSingleConfiguration(new CopilotLanguageServerSettings(null, null, null, settings.getGithubSettings()));
  }
  
  private List<Path> getProjectRootPaths() {
    return WorkspaceUtils.listTopLevelProjects().stream()
        .map(IProject::getLocation)
        .map(loc -> loc.toFile().toPath())
        .toList();
  }

  /**
   * Initializes the MCP tools status from the preference store for built-in agent mode only.
   * Custom agent modes get their tool configuration from the LSP/file, not from preferences.
   */
  public void initializeMcpToolsStatus() {
    // Load per-mode tool status
    String savedModeToolsStatus = preferenceStore.getString(Constants.MCP_TOOLS_MODE_STATUS);

    if (StringUtils.isNotBlank(savedModeToolsStatus)) {
      try {
        Map<String, Map<String, Map<String, Boolean>>> modeToolStatus = GsonUtils.getDefault()
            .fromJson(savedModeToolsStatus, new TypeToken<Map<String, Map<String, Map<String, Boolean>>>>() {
            }.getType());

        // Only initialize tool status for built-in agent mode, not custom modes.
        // Custom modes get their tool configuration from the LSP/file, not from preferences.
        for (Map.Entry<String, Map<String, Map<String, Boolean>>> modeEntry : modeToolStatus.entrySet()) {
          String modeId = modeEntry.getKey();

          // Skip custom agent modes - they should use tool configuration from their file/LSP
          if (CustomChatModeManager.INSTANCE.isCustomMode(modeId)) {
            continue;
          }

          Map<String, Map<String, Boolean>> toolStatus = modeEntry.getValue();
          String toolStatusJson = GsonUtils.getDefault().toJson(toolStatus);
          updateToolStatusForMode(toolStatusJson, modeId);
        }
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Failed to parse MCP mode tools status JSON", e);
      }
    } else {
      // Fallback to legacy MCP_TOOLS_STATUS for agent mode if MCP_TOOLS_MODE_STATUS is not available
      String savedMcpToolsStatus = preferenceStore.getString(Constants.MCP_TOOLS_STATUS);
      updateMcpToolsStatus(savedMcpToolsStatus, null);
    }
  }

  /**
   * Initializes the MCP tools status from the preference store with mode context.
   *
   * @param modeId the mode ID (e.g., "agent-mode" or custom mode ID)
   */
  public void initializeMcpToolsStatus(String modeId) {
    String savedMcpToolsStatus = preferenceStore.getString(Constants.MCP_TOOLS_STATUS);
    updateMcpToolsStatus(savedMcpToolsStatus, modeId);
  }

  /**
   * Update tool status for a specific mode.
   *
   * @param toolStatusJson the tool status in JSON format for this mode
   * @param modeId the mode ID ("agent-mode" for built-in agent mode, or "file://..." for custom modes)
   */
  public void updateToolStatusForMode(String toolStatusJson, String modeId) {
    updateMcpToolsStatus(toolStatusJson, modeId);
  }

  /**
   * Updates the MCP tools status.
   *
   * @param mcpToolsStatus the MCP tools status in JSON format. e.g.
   *     {"server1":{"tool1":true,"tool2":false},"server2":{"tool1":true}}
   * @param modeId the mode ID (null for agent mode, custom mode ID for custom modes)
   */
  private void updateMcpToolsStatus(String mcpToolsStatus, String modeId) {
    if (StringUtils.isBlank(mcpToolsStatus)) {
      return;
    }

    final Map<String, Map<String, Boolean>> toolStatusMap;
    try {
      toolStatusMap = GsonUtils.getDefault().fromJson(mcpToolsStatus,
          new TypeToken<Map<String, Map<String, Boolean>>>() {
          }.getType());
    } catch (JsonSyntaxException e) {
      CopilotCore.LOGGER.error("Failed to parse MCP tools status JSON", e);
      return;
    }

    UpdateMcpToolsStatusParams mcpParams = new UpdateMcpToolsStatusParams();
    List<McpServerToolsStatusCollection> serverList = new ArrayList<>();
    mcpParams.setServers(serverList);
    mcpParams.setWorkspaceFolders(WorkspaceUtils.listWorkspaceFolders());

    // Set custom mode ID only if this is for a custom mode (ID starts with "file://")
    // For built-in agent mode, customChatModeId should not be set
    if (modeId != null && modeId.startsWith("file://")) {
      mcpParams.setCustomChatModeId(modeId);
    }

    // Separate built-in tools from MCP server tools
    Map<String, Boolean> builtInTools = null;
    for (Map.Entry<String, Map<String, Boolean>> serverEntry : toolStatusMap.entrySet()) {
      String serverName = serverEntry.getKey();
      Map<String, Boolean> tools = serverEntry.getValue();

      // Check if this is the built-in tools entry
      if (Messages.preferences_page_mcp_tools_builtin.equals(serverName)) {
        builtInTools = tools;
        continue;
      }

      // This is an MCP server
      McpServerToolsStatusCollection serverToolsStatus = new McpServerToolsStatusCollection();
      serverToolsStatus.setName(serverName);

      List<McpToolsStatusCollection> toolStatusList = new ArrayList<>();
      serverToolsStatus.setTools(toolStatusList);

      for (Map.Entry<String, Boolean> toolEntry : tools.entrySet()) {
        String toolName = toolEntry.getKey();
        boolean enabled = toolEntry.getValue();

        McpToolsStatusCollection toolStatus = new McpToolsStatusCollection();
        toolStatus.setName(toolName);
        toolStatus.setStatus(enabled ? McpToolStatus.enabled.toString() : McpToolStatus.disabled.toString());
        toolStatusList.add(toolStatus);
      }

      serverList.add(serverToolsStatus);
    }

    // Prepare futures for both operations
    CompletableFuture<?> updateMcpToolsStatusFuture = null;
    CompletableFuture<?> updateConversationToolsStatusFuture = null;

    // Update MCP server tools
    if (!serverList.isEmpty()) {
      updateMcpToolsStatusFuture = this.copilotLanguageServerConnection.updateMcpToolsStatus(mcpParams);
    }

    // Update built-in tools using conversation/updateToolsStatus
    if (builtInTools != null && !builtInTools.isEmpty()) {
      UpdateConversationToolsStatusParams conversationParams = new UpdateConversationToolsStatusParams();
      conversationParams.setChatModeKind("Agent");
      conversationParams.setWorkspaceFolders(WorkspaceUtils.listWorkspaceFolders());

      // Set custom mode ID only if this is for a custom mode (ID starts with "file://")
      // For built-in agent mode, customChatModeId should not be set
      if (modeId != null && modeId.startsWith("file://")) {
        conversationParams.setCustomChatModeId(modeId);
      }

      List<ConversationToolStatus> toolsList = new ArrayList<>();
      for (Map.Entry<String, Boolean> toolEntry : builtInTools.entrySet()) {
        ConversationToolStatus toolStatus = new ConversationToolStatus();
        toolStatus.setName(toolEntry.getKey());
        toolStatus.setStatus(toolEntry.getValue().booleanValue() ? "enabled" : "disabled");
        toolsList.add(toolStatus);
      }
      conversationParams.setTools(toolsList);

      updateConversationToolsStatusFuture = this.copilotLanguageServerConnection
          .updateConversationToolsStatus(conversationParams);
    }

    // Execute both futures sequentially in background
    final CompletableFuture<?> mcpFuture = updateMcpToolsStatusFuture;
    final CompletableFuture<?> conversationFuture = updateConversationToolsStatusFuture;
    CompletableFuture.runAsync(() -> {
      if (mcpFuture != null) {
        mcpFuture.join();
      }
      if (conversationFuture != null) {
        conversationFuture.join();
      }
    });
  }

  /**
   * Updates the proxy settings.
   */
  public void updateProxySettings() {
    proxyData = getProxy();
    settings.getHttp().setProxy(createProxyString(proxyData));
    settings.getHttp().setProxyAuthorization(createProxyAuthString(proxyData));
    settings.getHttp().setNoProxy(getNonProxiedHosts());
  }

  /**
   * Gets the proxy data.
   *
   * @return the proxy data
   */
  private IProxyData getProxy() {
    if (proxyService == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("Proxy service is null"));
      return null;
    }
    if (!proxyService.isProxiesEnabled()) {
      return null;
    }
    IProxyData[] proxyDataArr = proxyService.select(URI.create(Constants.GITHUB_COPILOT_URL));
    if (proxyDataArr != null && proxyDataArr.length > 0) {
      return proxyDataArr[0];
    }
    return null;
  }

  /**
   * Gets the non-proxied hosts from the proxy service.
   *
   * @return the array of non-proxied hosts, or null if proxy is not enabled
   */
  private String[] getNonProxiedHosts() {
    if (proxyService == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("Proxy service is null"));
      return null;
    }
    if (!proxyService.isProxiesEnabled()) {
      return null;
    }
    return proxyService.getNonProxiedHosts();
  }

  /**
   * Creates a proxy string from the given proxy data.
   *
   * @param proxyData the proxy data
   * @return the proxy string
   */
  private String createProxyString(IProxyData proxyData) {
    if (proxyData == null) {
      return null;
    }

    String proxyString = proxyData.getType() + "://";
    String host = proxyData.getHost();
    int port = proxyData.getPort();
    proxyString += host + ":" + port;
    return proxyString;
  }

  private String createProxyAuthString(IProxyData proxyData) {
    if (proxyData == null || !proxyData.isRequiresAuthentication()) {
      return null;
    }

    return proxyData.getUserId() + ":" + proxyData.getPassword();
  }

  /**
   * Updates the workspace instruction enabled/disabled state and manages the instruction content accordingly. This
   * method is called when the user toggles the workspace instructions on or off in preferences. When enabled, it loads
   * the stored workspace instructions; when disabled, it clears them.
   *
   * @param isEnabled true to enable workspace instructions and load the stored content, false to disable them and clear
   *     the content.
   * @return the CopilotLanguageServerSettings to sync with the language server if workspace instructions are being
   *     changed.
   */
  private CopilotLanguageServerSettings updateWorkspaceInstructionEnabled(boolean isEnabled) {
    GitHubSettings githubSettings = new GitHubSettings();
    githubSettings.setWorkspaceCopilotInstructions(
        isEnabled ? preferenceStore.getString(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE) : null);
    return new CopilotLanguageServerSettings(null, null, null, githubSettings);
  }

  /**
   * Gets the preference store.
   *
   */
  public void registerPropertyChangeListener(IPropertyChangeListener listener) {
    if (preferenceStore == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("Preference store is null"));
      return;
    }
    preferenceStore.addPropertyChangeListener(listener);
  }

  /**
   * Unregisters the property change listener.
   *
   * @param listener the listener to unregister
   */
  public void unregisterPropertyChangeListener(IPropertyChangeListener listener) {
    if (preferenceStore == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("Preference store is null"));
      return;
    }
    preferenceStore.removePropertyChangeListener(listener);
  }

  /**
   * Gets the if auto show completions is enabled.
   */
  public boolean isAutoShowCompletionEnabled() {
    return preferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION);
  }

  /**
   * Enable or disable auto show completions.
   */
  public void setAutoShowCompletion(boolean autoShowCompletion) {
    preferenceStore.setValue(Constants.AUTO_SHOW_COMPLETION, autoShowCompletion);
  }

  /**
   * Disposes the resources of this LanguageServerSettingManager.
   */
  public void dispose() {
    proxyService.removeProxyChangeListener(this);
    if (mcpFileChangeListener != null) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(mcpFileChangeListener);
    }
  }

  private void registerMcpFileChangeListener() {
    mcpFileChangeListener = event -> {
      if (event.getDelta() != null && containsMcpJsonChange(event.getDelta())) {
        syncMcpRegistrationConfiguration();
      }
    };
    ResourcesPlugin.getWorkspace().addResourceChangeListener(mcpFileChangeListener,
        IResourceChangeEvent.POST_CHANGE);
  }

  /**
   * Checks whether a resource delta contains a change to an mcp.json file
   * at one of the supported project-level locations.
   */
  private boolean containsMcpJsonChange(IResourceDelta delta) {
    boolean[] found = { false };
    try {
      delta.accept(d -> {
        String path = d.getResource().getProjectRelativePath().toString();
        if (Constants.MCP_FILE_VSCODE.equals(path) || Constants.MCP_FILE_COPILOT.equals(path)) {
          found[0] = true;
          return false;
        }
        return true;
      });
    } catch (CoreException e) {
      CopilotCore.LOGGER.error("Error checking for MCP file changes", e);
    }
    return found[0];
  }
}
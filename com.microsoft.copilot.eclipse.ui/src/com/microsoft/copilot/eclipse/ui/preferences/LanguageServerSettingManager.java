package com.microsoft.copilot.eclipse.ui.preferences;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.lsp4j.DidChangeConfigurationParams;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings.GitHubSettings;
import com.microsoft.copilot.eclipse.core.lsp.protocol.McpServerToolsStatusCollection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.McpToolStatus;
import com.microsoft.copilot.eclipse.core.lsp.protocol.McpToolsStatusCollection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.UpdateMcpToolsStatusParams;
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

    // Set workspace context instructions when it is enabled
    if (preferenceStore.getBoolean(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED)) {
      getSettings().getGithubSettings()
          .setWorkspaceCopilotInstructions(preferenceStore.getString(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE));
    } else {
      getSettings().getGithubSettings().setWorkspaceCopilotInstructions(null);
    }
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
      case Constants.MCP:
        syncMcpRegistrationConfiguration();
        return;
      case Constants.MCP_TOOLS_STATUS:
        updateMcpToolsStatus(preferenceStore.getString(Constants.MCP_TOOLS_STATUS));
        return;
      case Constants.CUSTOM_INSTRUCTIONS_WORKSPACE:
        String workspaceInstructions = preferenceStore.getString(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE);
        settings.getGithubSettings().setWorkspaceCopilotInstructions(workspaceInstructions);
        if (preferenceStore.getBoolean(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED)) {
          GitHubSettings githubSettings = new GitHubSettings();
          githubSettings.setWorkspaceCopilotInstructions(workspaceInstructions);
          singleSetting = new CopilotLanguageServerSettings(null, null, null, githubSettings);
          break;
        }
        return;
      case Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED:
        singleSetting = updateWorkspaceInstructionEnabled(
            preferenceStore.getBoolean(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED));
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
   * Sync MCP registration from both extension points and preference store.
   */
  public void syncMcpRegistrationConfiguration() {
    // From manual configuration
    settings.setMcpServers(preferenceStore.getString(Constants.MCP));

    // From McpRegistration extension point
    McpExtensionPointManager mgr = CopilotUi.getPlugin().getChatServiceManager().getMcpExtensionPointManager();
    settings.addMcpServers(mgr.getApprovedExtMcpServers());

    syncSingleConfiguration(new CopilotLanguageServerSettings(null, null, null, settings.getGithubSettings()));
  }

  /**
   * Initializes the MCP tools status from the preference store.
   */
  public void initializeMcpToolsStatus() {
    String savedMcpToolsStatus = preferenceStore.getString(Constants.MCP_TOOLS_STATUS);
    updateMcpToolsStatus(savedMcpToolsStatus);
  }

  /**
   * Updates the MCP tools status.
   *
   * @param mcpToolsStatus the MCP tools status in JSON format. e.g.
   *        {"server1":{"tool1":true,"tool2":false},"server2":{"tool1":true}}
   */
  private void updateMcpToolsStatus(String mcpToolsStatus) {
    if (StringUtils.isBlank(mcpToolsStatus)) {
      return;
    }

    try {
      Gson gson = new Gson();
      Map<String, Map<String, Boolean>> toolStatusMap = gson.fromJson(mcpToolsStatus,
          new TypeToken<Map<String, Map<String, Boolean>>>() {
          }.getType());

      UpdateMcpToolsStatusParams params = new UpdateMcpToolsStatusParams();
      List<McpServerToolsStatusCollection> serverList = new ArrayList<>();
      params.setServers(serverList);

      for (Map.Entry<String, Map<String, Boolean>> serverEntry : toolStatusMap.entrySet()) {
        String serverName = serverEntry.getKey();
        Map<String, Boolean> tools = serverEntry.getValue();

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

      this.copilotLanguageServerConnection.updateMcpToolsStatus(params);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to parse MCP tools status JSON", e);
    }
  }

  /**
   * Updates the proxy settings.
   */
  public void updateProxySettings() {
    proxyData = getProxy();
    if (proxyData == null) {
      settings.getHttp().setProxy(null);
      return;
    }
    settings.getHttp().setProxy(createProxyString(proxyData));
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
   * Creates a proxy string from the given proxy data.
   *
   * @param proxyData the proxy data
   * @return the proxy string
   */
  public static String createProxyString(IProxyData proxyData) {
    if (proxyData == null) {
      return null;
    }

    String proxyString = proxyData.getType() + "://";
    String host = proxyData.getHost();
    int port = proxyData.getPort();
    String user = proxyData.getUserId();
    String password = proxyData.getPassword();

    if (proxyData.isRequiresAuthentication()) {
      proxyString += user + ":" + password + "@";
    }
    proxyString += host + ":" + port;
    return proxyString;
  }

  /**
   * Updates the workspace instruction enabled/disabled state and manages the instruction content accordingly. This
   * method is called when the user toggles the workspace instructions on or off in preferences. When enabled, it loads
   * the stored workspace instructions; when disabled, it clears them.
   *
   * @param isEnabled true to enable workspace instructions and load the stored content, false to disable them and clear
   *        the content.
   * @return the CopilotLanguageServerSettings to sync with the language server if workspace instructions are being
   *        changed.
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
  }
}
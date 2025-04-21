package com.microsoft.copilot.eclipse.ui.preferences;

import java.net.URI;

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
    getSettings().setMcpServers(preferenceStore.getString(Constants.MCP));
    getSettings().getHttp().setProxyKerberosServicePrincipal(preferenceStore.getString(Constants.PROXY_KERBEROS_SP));
    getSettings().getGithubEnterprise().setUri(preferenceStore.getString(Constants.GITHUB_ENTERPRISE));
  }

  /**
   * A listener for the proxy service.
   */
  @Override
  public void proxyInfoChanged(IProxyChangeEvent event) {
    CopilotCore.LOGGER.info("Proxy info changed");
    updateProxySettings();
    syncConfiguration();
  }

  /**
   * A listener for the preferences.
   */
  @Override
  public void propertyChange(PropertyChangeEvent event) {
    switch (event.getProperty()) {
      case Constants.ENABLE_STRICT_SSL:
        var newVal = Boolean.parseBoolean(event.getNewValue().toString());
        this.settings.getHttp().setProxyStrictSsl(newVal);
        CopilotCore.LOGGER.info("Strict SSL is now " + event.getNewValue());
        break;
      case Constants.PROXY_KERBEROS_SP:
        this.settings.getHttp().setProxyKerberosServicePrincipal((String) event.getNewValue());
        CopilotCore.LOGGER.info("Kerberos SP is now " + event.getNewValue());
        break;
      case Constants.GITHUB_ENTERPRISE:
        this.settings.getGithubEnterprise().setUri((String) event.getNewValue());
        CopilotCore.LOGGER.info("GitHub Enterprise URI is now '" + event.getNewValue() + "'");
        break;
      case Constants.AUTO_SHOW_COMPLETION:
        Boolean autoShowCompletion = Boolean.parseBoolean(event.getNewValue().toString());
        this.settings.setEnableAutoCompletions(autoShowCompletion);
        CopilotCore.LOGGER.info("Auto show completion is now " + event.getNewValue());
        break;
      case Constants.MCP:
        this.settings.setMcpServers((String) event.getNewValue());
        CopilotCore.LOGGER.info("MCP is now \n" + event.getNewValue());
        break;
      default:
        return;
    }
    syncConfiguration();
  }

  /**
   * Synchronizes the configuration with the language server.
   */
  public void syncConfiguration() {
    DidChangeConfigurationParams params = new DidChangeConfigurationParams();
    params.setSettings(settings);
    CopilotCore copilotCore = CopilotCore.getPlugin();
    if (copilotCore != null && copilotCore.getGithubPanicErrorReport() != null) {
      copilotCore.getGithubPanicErrorReport().setProxyStrictSsl(settings.getHttp().isProxyStrictSsl());
      copilotCore.getGithubPanicErrorReport().setProxyData(proxyData);
    }
    this.copilotLanguageServerConnection.updateConfig(params);
  }

  /**
   * Updates the proxy settings.
   */
  public void updateProxySettings() {
    proxyData = getProxy();
    if (proxyData == null) {
      settings.getHttp().setProxy(null);
      CopilotCore.LOGGER.info("No proxy data found");
      return;
    }
    settings.getHttp().setProxy(createProxyString(proxyData));
    if (proxyData.getUserId() != null) {
      CopilotCore.LOGGER.info(String.format("Proxy will be updated to %s://[username]:[password]@%s:%s",
          proxyData.getType(), proxyData.getHost(), proxyData.getPort()));
    } else {
      CopilotCore.LOGGER.info(String.format("Proxy will be updated to %s", settings.getHttp().getProxy()));
    }

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
      CopilotCore.LOGGER.info("Proxies are disabled");
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
      CopilotCore.LOGGER.info("Proxy data is null");
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

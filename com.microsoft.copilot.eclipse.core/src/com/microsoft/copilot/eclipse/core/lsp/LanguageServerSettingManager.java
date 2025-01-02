package com.microsoft.copilot.eclipse.core.lsp;

import java.net.URI;

import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.lsp4j.DidChangeConfigurationParams;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.logger.LogLevel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;

/**
 * A class to manage the proxy service for the Copilot Language Server.
 */
public class LanguageServerSettingManager implements IProxyChangeListener {
  IProxyService proxyService = null;
  CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
  CopilotLanguageServerConnection copilotLanguageServerConnection = null;

  /**
   * Initializes the LanguageServerSettingManager.
   */
  public LanguageServerSettingManager(CopilotLanguageServerConnection conn, IProxyService proxyService) {
    this.copilotLanguageServerConnection = conn;
    this.proxyService = proxyService;

    // add listners
    proxyService.addProxyChangeListener(this);
  }

  /**
   * A listener for the proxy service.
   */
  @Override
  public void proxyInfoChanged(IProxyChangeEvent event) {
    CopilotCore.LOGGER.log(LogLevel.INFO, "Proxy info changed");
    updateProxySettings();
    syncConfiguration();
  }

  /**
   * Synchronizes the configuration with the language server.
   */
  public void syncConfiguration() {
    DidChangeConfigurationParams params = new DidChangeConfigurationParams();
    params.setSettings(settings);
    this.copilotLanguageServerConnection.updateConfig(params);
  }

  /**
   * Updates the proxy settings.
   */
  public void updateProxySettings() {
    IProxyData proxyData = getProxy();
    if (proxyData == null) {
      settings.getHttp().setProxy(null);
      CopilotCore.LOGGER.log(LogLevel.INFO, "No proxy data found");
      return;
    }
    settings.getHttp().setProxy(createProxyString(proxyData));
    CopilotCore.LOGGER.log(LogLevel.INFO, String.format("Proxy will be updated to %s", settings.getHttp().getProxy()));

  }

  /**
   * Gets the proxy data.
   *
   * @return the proxy data
   */
  private IProxyData getProxy() {
    if (proxyService == null) {
      CopilotCore.LOGGER.log(LogLevel.ERROR, "Proxy service is null");
      return null;
    }
    if (!proxyService.isProxiesEnabled()) {
      CopilotCore.LOGGER.log(LogLevel.INFO, "Proxies are disabled");
      return null;
    }
    IProxyData[] proxyData = proxyService.select(URI.create(Constants.GITHUB_COPILOT_URL));
    if (proxyData != null && proxyData.length > 0) {
      return proxyData[0];
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
      CopilotCore.LOGGER.log(LogLevel.ERROR, "Proxy data is null");
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
   * Disposes the resources of this LanguageServerSettingManager.
   */
  public void dispose() {
    proxyService.removeProxyChangeListener(this);
  }
}

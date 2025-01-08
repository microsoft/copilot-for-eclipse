package com.microsoft.copilot.eclipse.ui.preferences;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;
import com.microsoft.copilot.eclipse.ui.prerferences.LanguageServerSettingManager;

/**
 * Tests for the LanguageServerSettingManager.
 */
public class LanguageServerSettingManagerTests {
  @Mock
  private IPreferenceStore mockPreferenceStore;

  @Test
  void testNoProxy() {
    // when no proxy is applicable
    // arrange
    IProxyService mockProxyService = mock(IProxyService.class);
    CopilotLanguageServerConnection mockLsConnection = mock(CopilotLanguageServerConnection.class);
    when(mockProxyService.select(any())).thenReturn(null);
    var params = new DidChangeConfigurationParams();
    params.setSettings(new CopilotLanguageServerSettings());

    // act
    LanguageServerSettingManager manager = new LanguageServerSettingManager(mockLsConnection, mockProxyService,
        mockPreferenceStore);
    manager.updateProxySettings();
    manager.syncConfiguration();

    // assert
    verify(mockLsConnection, times(1)).updateConfig(params);
  }

  @Test
  void testBasicProxy() {
    // basic proxy test
    // arrange
    IProxyService mockProxyService = mock(IProxyService.class);
    IProxyData mockProxyData = mock(IProxyData.class);
    when(mockProxyData.getHost()).thenReturn("localhost");
    when(mockProxyData.getPort()).thenReturn(8080);
    when(mockProxyData.getType()).thenReturn("HTTPS");
    when(mockProxyData.isRequiresAuthentication()).thenReturn(false);
    when(mockProxyService.select(any())).thenReturn(new IProxyData[] { mockProxyData });
    when(mockProxyService.isProxiesEnabled()).thenReturn(true);
    var params = new DidChangeConfigurationParams();
    var settings = new CopilotLanguageServerSettings();
    settings.getHttp().setProxy("HTTPS://localhost:8080");
    params.setSettings(settings);
    CopilotLanguageServerConnection mockLsConnection = mock(CopilotLanguageServerConnection.class);

    // act
    LanguageServerSettingManager manager = new LanguageServerSettingManager(mockLsConnection, mockProxyService,
        mockPreferenceStore);
    manager.updateProxySettings();
    manager.syncConfiguration();

    // assert
    verify(mockLsConnection, times(1)).updateConfig(params);
  }

  @Test
  void testBasicAuthProxy() {
    // basic auth proxy test
    // arrange
    IProxyService mockProxyService = mock(IProxyService.class);
    IProxyData mockProxyData = mock(IProxyData.class);
    when(mockProxyData.getHost()).thenReturn("localhost");
    when(mockProxyData.getPort()).thenReturn(8080);
    when(mockProxyData.getType()).thenReturn("HTTPS");
    when(mockProxyData.isRequiresAuthentication()).thenReturn(true);
    when(mockProxyData.getUserId()).thenReturn("user");
    when(mockProxyData.getPassword()).thenReturn("password");
    when(mockProxyService.select(any())).thenReturn(new IProxyData[] { mockProxyData });
    when(mockProxyService.isProxiesEnabled()).thenReturn(true);
    var params = new DidChangeConfigurationParams();
    var settings = new CopilotLanguageServerSettings();
    settings.getHttp().setProxy("HTTPS://user:password@localhost:8080");
    params.setSettings(settings);
    CopilotLanguageServerConnection mockLsConnection = mock(CopilotLanguageServerConnection.class);

    // act
    LanguageServerSettingManager manager = new LanguageServerSettingManager(mockLsConnection, mockProxyService,
        mockPreferenceStore);
    manager.updateProxySettings();
    manager.syncConfiguration();

    // assert
    verify(mockLsConnection, times(1)).updateConfig(params);
  }

}

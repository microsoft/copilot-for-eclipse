package com.microsoft.copilot.eclipse.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings.CopilotSettings;
import com.microsoft.copilot.eclipse.ui.CopilotUi;

@ExtendWith(MockitoExtension.class)
class LanguageServerSettingManagerTests {
  @Mock
  private IPreferenceStore mockPreferenceStore;

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @Mock
  private IProxyService mockProxyService;

  @Test
  void testNoProxy() {
    // when no proxy is applicable
    // arrange
    when(mockPreferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION)).thenReturn(true);
    when(mockPreferenceStore.getString(Constants.MCP)).thenReturn(null);
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
    IProxyData mockProxyData = mock(IProxyData.class);
    when(mockProxyData.getHost()).thenReturn("localhost");
    when(mockProxyData.getPort()).thenReturn(8080);
    when(mockProxyData.getType()).thenReturn("HTTPS");
    when(mockProxyData.isRequiresAuthentication()).thenReturn(false);
    when(mockProxyService.select(any())).thenReturn(new IProxyData[] { mockProxyData });
    when(mockProxyService.isProxiesEnabled()).thenReturn(true);
    when(mockPreferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION)).thenReturn(true);
    when(mockPreferenceStore.getString(Constants.MCP)).thenReturn(null);
    var params = new DidChangeConfigurationParams();
    var settings = new CopilotLanguageServerSettings();
    settings.getHttp().setProxy("HTTPS://localhost:8080");
    params.setSettings(settings);

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
    IProxyData mockProxyData = mock(IProxyData.class);
    when(mockProxyData.getHost()).thenReturn("localhost");
    when(mockProxyData.getPort()).thenReturn(8080);
    when(mockProxyData.getType()).thenReturn("HTTPS");
    when(mockProxyData.isRequiresAuthentication()).thenReturn(true);
    when(mockProxyData.getUserId()).thenReturn("user");
    when(mockProxyData.getPassword()).thenReturn("password");
    when(mockProxyService.select(any())).thenReturn(new IProxyData[] { mockProxyData });
    when(mockProxyService.isProxiesEnabled()).thenReturn(true);
    when(mockPreferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION)).thenReturn(true);
    when(mockPreferenceStore.getString(Constants.MCP)).thenReturn(null);
    var params = new DidChangeConfigurationParams();
    var settings = new CopilotLanguageServerSettings();
    settings.getHttp().setProxy("HTTPS://user:password@localhost:8080");
    params.setSettings(settings);

    // act
    LanguageServerSettingManager manager = new LanguageServerSettingManager(mockLsConnection, mockProxyService,
        mockPreferenceStore);
    manager.updateProxySettings();
    manager.syncConfiguration();

    // assert
    verify(mockLsConnection, times(1)).updateConfig(params);
  }
  
  private void setupWorkspaceInstructionsMocks(boolean enabled, String instructions) {
    when(mockPreferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION)).thenReturn(true);
    when(mockPreferenceStore.getBoolean(Constants.ENABLE_STRICT_SSL)).thenReturn(false);
    when(mockPreferenceStore.getString(Constants.MCP)).thenReturn(null);
    when(mockPreferenceStore.getString(Constants.PROXY_KERBEROS_SP)).thenReturn(null);
    when(mockPreferenceStore.getString(Constants.GITHUB_ENTERPRISE)).thenReturn(null);
    when(mockPreferenceStore.getBoolean(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE_ENABLED)).thenReturn(enabled);
    if (enabled && instructions != null) {
      when(mockPreferenceStore.getString(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE)).thenReturn(instructions);
    }
  }

  @Test
  void testUpdateConfigShouldBeCalledWhenWorkspaceInstructionsEnabledWithContent() {
    // arrange
    IProxyService mockProxyService = mock(IProxyService.class);
    CopilotLanguageServerConnection mockLsConnection = mock(CopilotLanguageServerConnection.class);
    setupWorkspaceInstructionsMocks(true, "Test instructions");

    DidChangeConfigurationParams params = new DidChangeConfigurationParams();
    CopilotSettings copilotSettings = new CopilotSettings();
    copilotSettings.setWorkspaceCopilotInstructions("Test instructions");
    CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
    settings.getGithubSettings().setCopilotSettings(copilotSettings);
    params.setSettings(settings);

    // act
    LanguageServerSettingManager manager = new LanguageServerSettingManager(mockLsConnection, mockProxyService,
        mockPreferenceStore);
    manager.updateProxySettings();
    manager.syncConfiguration();

    // assert
    verify(mockPreferenceStore, times(1)).getString(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE);
    verify(mockLsConnection, times(1)).updateConfig(params);
    
    CopilotSettings capturedSettings = ((CopilotLanguageServerSettings) params.getSettings()).getGithubSettings().getCopilotSettings();
    assertEquals("Test instructions", capturedSettings.getWorkspaceCopilotInstructions());
    assertNull(capturedSettings.getMcpServers(), "Custom instructions update should not set MCP servers");
  }
  
  @Test
  void testUpdateConfigShouldBeCalledWithoutInstructionWhenWorkspaceInstructionsDisabled() {
    // arrange
    IProxyService mockProxyService = mock(IProxyService.class);
    CopilotLanguageServerConnection mockLsConnection = mock(CopilotLanguageServerConnection.class);
    setupWorkspaceInstructionsMocks(false, null);

    // Expected params should have empty workspace instructions since it's disabled
    DidChangeConfigurationParams expectedParams = new DidChangeConfigurationParams();
    CopilotLanguageServerSettings expectedSettings = new CopilotLanguageServerSettings();
    expectedParams.setSettings(expectedSettings);

    // act
    LanguageServerSettingManager manager = new LanguageServerSettingManager(mockLsConnection, mockProxyService,
        mockPreferenceStore);
    manager.updateProxySettings();
    manager.syncConfiguration();

    // assert - verify that updateConfig is called with settings that have empty workspace instructions
    verify(mockPreferenceStore, times(0)).getString(Constants.CUSTOM_INSTRUCTIONS_WORKSPACE);
    verify(mockLsConnection, times(1)).updateConfig(expectedParams);
    
    CopilotSettings capturedSettings = ((CopilotLanguageServerSettings) expectedParams.getSettings()).getGithubSettings().getCopilotSettings();
    assertNull(capturedSettings.getWorkspaceCopilotInstructions());
    assertNull(capturedSettings.getMcpServers(), "Custom instructions update should not set MCP servers");
  }
  
  @Test
  void testUpdateAutoCompletionSetting() {
    IPreferenceStore preferenceStore = CopilotUi.getPlugin().getPreferenceStore();

    new LanguageServerSettingManager(mockLsConnection, mockProxyService, preferenceStore);
    ArgumentCaptor<DidChangeConfigurationParams> paramsCaptor = ArgumentCaptor
        .forClass(DidChangeConfigurationParams.class);

    preferenceStore.setValue(Constants.AUTO_SHOW_COMPLETION, false);

    verify(mockLsConnection, timeout(100).times(1)).updateConfig(paramsCaptor.capture());

    CopilotLanguageServerSettings capturedSettings = (CopilotLanguageServerSettings) paramsCaptor.getValue()
        .getSettings();
    assertFalse(capturedSettings.isEnableAutoCompletions());
  }

  @Test
  void testUpdateStrictSslSetting() {
    IPreferenceStore preferenceStore = CopilotUi.getPlugin().getPreferenceStore();

    new LanguageServerSettingManager(mockLsConnection, mockProxyService, preferenceStore);
    ArgumentCaptor<DidChangeConfigurationParams> paramsCaptor = ArgumentCaptor
        .forClass(DidChangeConfigurationParams.class);

    preferenceStore.setValue(Constants.ENABLE_STRICT_SSL, false);

    verify(mockLsConnection, timeout(100).times(1)).updateConfig(paramsCaptor.capture());

    CopilotLanguageServerSettings capturedSettings = (CopilotLanguageServerSettings) paramsCaptor.getValue()
        .getSettings();
    assertFalse(capturedSettings.getHttp().isProxyStrictSsl());
  }

  @Test
	void testInitializeMcpToolsStatusWhenEmpty() {
    // arrange
	when(mockPreferenceStore.getBoolean(Constants.AUTO_SHOW_COMPLETION)).thenReturn(true);
	when(mockPreferenceStore.getString(Constants.PROXY_KERBEROS_SP)).thenReturn(null);
	when(mockPreferenceStore.getString(Constants.GITHUB_ENTERPRISE)).thenReturn(null);
	when(mockPreferenceStore.getString(Constants.MCP)).thenReturn(null);
    when(mockPreferenceStore.getString(Constants.MCP_TOOLS_STATUS)).thenReturn("");

    // act
    LanguageServerSettingManager manager = new LanguageServerSettingManager(mockLsConnection, mockProxyService,
        mockPreferenceStore);
	assertDoesNotThrow(manager::initializeMcpToolsStatus);

    // assert
	verify(mockPreferenceStore, times(1)).getString(Constants.MCP_TOOLS_STATUS);
    verify(mockLsConnection, times(0)).updateMcpToolsStatus(any());
  }
}
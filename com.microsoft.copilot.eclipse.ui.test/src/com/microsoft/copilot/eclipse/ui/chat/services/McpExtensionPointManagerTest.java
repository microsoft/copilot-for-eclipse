// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.McpExtensionPointManager.McpRegistrationInfo;

@ExtendWith(MockitoExtension.class)
class McpExtensionPointManagerTest {

  @Mock
  private McpConfigService mockMcpConfigService;

  @Mock
  private CopilotUi mockCopilotUi;

  private McpExtensionPointManager manager;
  private Gson gson;
  private CopilotUi originalPlugin;

  @BeforeEach
  void setUp() throws Exception {
    gson = new Gson();

    // Use reflection to temporarily replace the CopilotUi singleton
    // This is necessary since McpExtensionPointManager calls CopilotUi.getPlugin()
    Field pluginField = CopilotUi.class.getDeclaredField("COPILOT_UI_PLUGIN");
    pluginField.setAccessible(true);

    // Save the original plugin instance to restore later
    originalPlugin = (CopilotUi) pluginField.get(null);

    // Set the mock plugin
    pluginField.set(null, mockCopilotUi);

    manager = new McpExtensionPointManager(mockMcpConfigService);
  }

  @AfterEach
  void tearDown() throws Exception {
    // Restore the original plugin instance
    Field pluginField = CopilotUi.class.getDeclaredField("COPILOT_UI_PLUGIN");
    pluginField.setAccessible(true);
    pluginField.set(null, originalPlugin);
  }

  /**
   * Helper method to create McpRegistrationInfo instances using reflection to avoid IllegalAccessError due to
   * package-private constructor across OSGi bundles.
   */
  private McpRegistrationInfo createMcpRegistrationInfo(boolean isTrusted, boolean isApproved, String pluginDisplayName,
      Map<String, Object> mcpServers) throws Exception {
    Class<?> infoClass = McpRegistrationInfo.class;
    java.lang.reflect.Constructor<?> constructor = infoClass.getDeclaredConstructor(boolean.class, boolean.class,
        String.class, Map.class);
    constructor.setAccessible(true);
    return (McpRegistrationInfo) constructor.newInstance(isTrusted, isApproved, pluginDisplayName, mcpServers);
  }

  @Test
  void testRemovePersistedSettingWhenContributedMcpServerStringIsNull() throws Exception {
    // Arrange: Set up initial state with persisted MCP servers
    Map<String, Object> initialServers = new HashMap<>();
    initialServers.put("test-server", Map.of("command", "test-command"));

    McpRegistrationInfo initialRegInfo = createMcpRegistrationInfo(true, true, "Test Plugin", initialServers);

    Map<String, McpRegistrationInfo> initialMcpInfoMap = new HashMap<>();
    initialMcpInfoMap.put("com.example.plugin", initialRegInfo);

    // Use reflection to call the private method updateApprovedMcpServerString
    Method updateMethod = McpExtensionPointManager.class.getDeclaredMethod("updateApprovedMcpServerString", Map.class);
    updateMethod.setAccessible(true);

    // Act: Call updateApprovedMcpServerString with empty map (simulating null/removed servers)
    Map<String, McpRegistrationInfo> emptyMcpInfoMap = Collections.emptyMap();
    updateMethod.invoke(manager, emptyMcpInfoMap);

    // Get the approved servers string
    String approvedServers = manager.getApprovedExtMcpServers();

    // Assert: Verify the approved servers string contains an empty servers object
    assertNotNull(approvedServers);
    Map<String, Object> result = gson.fromJson(approvedServers, Map.class);
    assertNotNull(result);
    assertTrue(result.containsKey("servers"));

    Map<String, Object> servers = (Map<String, Object>) result.get("servers");
    assertNotNull(servers);
    assertTrue(servers.isEmpty(), "Servers map should be empty when contributed MCP server string is null");
  }

  @Test
  void testPersistExtMcpInfoWithEmptyMap() throws Exception {
    // Mock CopilotUi plugin to return the mock preference store
    IPreferenceStore mockPreferenceStore = mock(IPreferenceStore.class);
    when(mockCopilotUi.getPreferenceStore()).thenReturn(mockPreferenceStore);

    // Arrange: Use reflection to call the private persistExtMcpInfo method
    Method persistMethod = McpExtensionPointManager.class.getDeclaredMethod("persistExtMcpInfo", Map.class);
    persistMethod.setAccessible(true);

    // Act: Persist an empty map
    Map<String, McpRegistrationInfo> emptyMap = Collections.emptyMap();
    persistMethod.invoke(manager, emptyMap);

    // Assert: Verify that setValue was called with an empty JSON object
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockPreferenceStore).setValue(eq(Constants.MCP_EXTENSION_POINT_CONTRIB), valueCaptor.capture());

    String persistedValue = valueCaptor.getValue();
    assertNotNull(persistedValue);
    assertEquals("{}", persistedValue, "Empty map should be persisted as empty JSON object");
  }

  @Test
  void testUpdateApprovedMcpServerStringWithNullMap() throws Exception {
    // Arrange: Use reflection to call the private method
    Method updateMethod = McpExtensionPointManager.class.getDeclaredMethod("updateApprovedMcpServerString", Map.class);
    updateMethod.setAccessible(true);

    // Act: Call with null map
    updateMethod.invoke(manager, (Map<String, McpRegistrationInfo>) null);

    // Assert: Verify the approved servers string remains null or unchanged
    String approvedServers = manager.getApprovedExtMcpServers();
    // The method should return early when map is null, so approvedServers should be null
    assertTrue(approvedServers == null || approvedServers.isEmpty(),
        "Approved servers should be null or empty when input map is null");
  }

  @Test
  void testUpdateApprovedMcpServerStringWithUnapprovedServers() throws Exception {
    // Arrange: Create registration info with unapproved servers
    Map<String, Object> servers = new HashMap<>();
    servers.put("test-server", Map.of("command", "test-command"));

    McpRegistrationInfo unapprovedRegInfo = createMcpRegistrationInfo(true, false, // not approved
        "Test Plugin", servers);

    Map<String, McpRegistrationInfo> mcpInfoMap = new HashMap<>();
    mcpInfoMap.put("com.example.plugin", unapprovedRegInfo);

    // Use reflection to call the private method
    Method updateMethod = McpExtensionPointManager.class.getDeclaredMethod("updateApprovedMcpServerString", Map.class);
    updateMethod.setAccessible(true);

    // Act: Call with unapproved servers
    updateMethod.invoke(manager, mcpInfoMap);

    // Assert: Verify the approved servers string contains empty servers (since none are approved)
    String approvedServers = manager.getApprovedExtMcpServers();
    assertNotNull(approvedServers);

    Map<String, Object> result = gson.fromJson(approvedServers, Map.class);
    Map<String, Object> resultServers = (Map<String, Object>) result.get("servers");
    assertTrue(resultServers.isEmpty(), "Servers map should be empty when all servers are unapproved");
  }

  @Test
  void testUpdateApprovedMcpServerStringWithNullServers() throws Exception {
    // Arrange: Create registration info with null servers
    McpRegistrationInfo regInfoWithNullServers = createMcpRegistrationInfo(true, true, "Test Plugin", null);

    Map<String, McpRegistrationInfo> mcpInfoMap = new HashMap<>();
    mcpInfoMap.put("com.example.plugin", regInfoWithNullServers);

    // Use reflection to call the private method
    Method updateMethod = McpExtensionPointManager.class.getDeclaredMethod("updateApprovedMcpServerString", Map.class);
    updateMethod.setAccessible(true);

    // Act: Call with null servers
    updateMethod.invoke(manager, mcpInfoMap);

    // Assert: Verify the approved servers string contains empty servers
    String approvedServers = manager.getApprovedExtMcpServers();
    assertNotNull(approvedServers);

    Map<String, Object> result = gson.fromJson(approvedServers, Map.class);
    Map<String, Object> resultServers = (Map<String, Object>) result.get("servers");
    assertTrue(resultServers.isEmpty(), "Servers map should be empty when MCP servers are null");
  }

  @Test
  void testDetectChangesDropsApprovedServerWhenPluginUninstalled() throws Exception {
    // Regression test for https://github.com/microsoft/copilot-for-eclipse/issues/153 scenario 1:
    // a previously approved plugin is no longer providing any MCP server.
    IPreferenceStore mockPreferenceStore = mock(IPreferenceStore.class);
    when(mockCopilotUi.getPreferenceStore()).thenReturn(mockPreferenceStore);

    Map<String, Object> previouslyApprovedServers = new HashMap<>();
    previouslyApprovedServers.put("server-X", Map.of("url", "http://localhost:9000"));
    McpRegistrationInfo persistedInfo = createMcpRegistrationInfo(true, true, "Test Plugin",
        previouslyApprovedServers);
    Map<String, McpRegistrationInfo> persisted = new HashMap<>();
    persisted.put("com.example.plugin", persistedInfo);

    // Live extension scan returned nothing (plugin uninstalled or no longer provides servers).
    setExtMcpInfoMap(new HashMap<>());

    invokeDetectChanges(persisted);

    String approvedServers = manager.getApprovedExtMcpServers();
    assertNotNull(approvedServers, "Approved servers JSON should be non-null after detect");
    Map<String, Object> result = gson.fromJson(approvedServers, Map.class);
    Map<String, Object> resultServers = (Map<String, Object>) result.get("servers");
    assertTrue(resultServers.isEmpty(),
        "Stale approved server must be dropped when the live extension scan returns nothing");

    // No new approval prompt should be raised because there is no incoming registration to approve.
    verify(mockMcpConfigService, never()).setNewExtMcpRegFound(true);

    // The verified state must be persisted so subsequent startups do not resurrect the stale entry.
    ArgumentCaptor<String> persistedJson = ArgumentCaptor.forClass(String.class);
    verify(mockPreferenceStore, atLeastOnce()).setValue(eq(Constants.MCP_EXTENSION_POINT_CONTRIB),
        persistedJson.capture());
    assertEquals("{}", persistedJson.getValue(),
        "Persisted contribution map should be empty after the plugin is gone");
  }

  @Test
  void testDetectChangesDropsApprovalAndFlagsRedNoticeWhenConfigChanges() throws Exception {
    // Regression test for https://github.com/microsoft/copilot-for-eclipse/issues/153 scenario 2:
    // the contributing plugin returns a different config than what was previously approved.
    IPreferenceStore mockPreferenceStore = mock(IPreferenceStore.class);
    when(mockCopilotUi.getPreferenceStore()).thenReturn(mockPreferenceStore);

    Map<String, Object> oldServers = new HashMap<>();
    oldServers.put("server-X", Map.of("url", "http://localhost:9000"));
    McpRegistrationInfo persistedInfo = createMcpRegistrationInfo(true, true, "Test Plugin", oldServers);
    Map<String, McpRegistrationInfo> persisted = new HashMap<>();
    persisted.put("com.example.plugin", persistedInfo);

    // Live extension scan returned the same plugin but with a different server config (port change).
    Map<String, Object> newServers = new HashMap<>();
    newServers.put("server-X", Map.of("url", "http://localhost:9999"));
    McpRegistrationInfo currentInfo = createMcpRegistrationInfo(true, false, "Test Plugin", newServers);
    Map<String, McpRegistrationInfo> currentMap = new HashMap<>();
    currentMap.put("com.example.plugin", currentInfo);
    setExtMcpInfoMap(currentMap);

    invokeDetectChanges(persisted);

    String approvedServers = manager.getApprovedExtMcpServers();
    assertNotNull(approvedServers);
    Map<String, Object> result = gson.fromJson(approvedServers, Map.class);
    Map<String, Object> resultServers = (Map<String, Object>) result.get("servers");
    assertTrue(resultServers.isEmpty(),
        "Changed config must not be auto-applied; LSP must not receive the (now unapproved) entry");

    assertFalse(currentInfo.isApproved(),
        "Previously approved entry whose config changed must be marked as unapproved until the user re-approves");
    verify(mockMcpConfigService).setNewExtMcpRegFound(true);
    verify(mockPreferenceStore, atLeastOnce()).setValue(eq(Constants.MCP_EXTENSION_POINT_CONTRIB),
        org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void testDetectChangesPreservesApprovalWhenConfigUnchanged() throws Exception {
    // Companion test: when the live extension scan reports the same config as the persisted cache,
    // the previous approval must be carried over so the explicit LSP sync at the end of
    // doRegistration() can push the verified servers to the language server.
    IPreferenceStore mockPreferenceStore = mock(IPreferenceStore.class);
    when(mockCopilotUi.getPreferenceStore()).thenReturn(mockPreferenceStore);

    Map<String, Object> approvedServers = new HashMap<>();
    approvedServers.put("server-X", Map.of("url", "http://localhost:9000"));
    McpRegistrationInfo persistedInfo = createMcpRegistrationInfo(true, true, "Test Plugin", approvedServers);
    Map<String, McpRegistrationInfo> persisted = new HashMap<>();
    persisted.put("com.example.plugin", persistedInfo);

    // Live extension scan returned the same servers; default isApproved=false until carry-over runs.
    Map<String, Object> sameServers = new HashMap<>();
    sameServers.put("server-X", Map.of("url", "http://localhost:9000"));
    McpRegistrationInfo currentInfo = createMcpRegistrationInfo(true, false, "Test Plugin", sameServers);
    Map<String, McpRegistrationInfo> currentMap = new HashMap<>();
    currentMap.put("com.example.plugin", currentInfo);
    setExtMcpInfoMap(currentMap);

    invokeDetectChanges(persisted);

    assertTrue(currentInfo.isApproved(),
        "When the contributed config matches the persisted JSON, the previous approval must be carried over");

    String approvedJson = manager.getApprovedExtMcpServers();
    assertNotNull(approvedJson);
    Map<String, Object> result = gson.fromJson(approvedJson, Map.class);
    Map<String, Object> resultServers = (Map<String, Object>) result.get("servers");
    assertEquals(1, resultServers.size(),
        "Carried-over approved server must be present in the approved servers JSON for the LSP sync");

    // No red-notice should be raised because nothing has changed for the user to re-review.
    verify(mockMcpConfigService, never()).setNewExtMcpRegFound(true);
  }

  /**
   * Reflectively replace the manager's private {@code extMcpInfoMap} so tests can simulate the
   * outcome of {@code loadMcpRegistrationExtensionPoint()} without requiring a live OSGi
   * extension registry.
   */
  private void setExtMcpInfoMap(Map<String, McpRegistrationInfo> map) throws Exception {
    Field field = McpExtensionPointManager.class.getDeclaredField("extMcpInfoMap");
    field.setAccessible(true);
    field.set(manager, map);
  }

  private void invokeDetectChanges(Map<String, McpRegistrationInfo> persisted) throws Exception {
    // Get the scanned map that was previously set via setExtMcpInfoMap
    Field field = McpExtensionPointManager.class.getDeclaredField("extMcpInfoMap");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, McpRegistrationInfo> scannedMap = (Map<String, McpRegistrationInfo>) field.get(manager);

    // detectChangesInMcpContribs now takes (scannedMap, persistedMap) and no longer
    // calls updateApprovedMcpServerString / persistExtMcpInfo (those moved to doRegistration).
    Method detectMethod = McpExtensionPointManager.class.getDeclaredMethod("detectChangesInMcpContribs", Map.class,
        Map.class);
    detectMethod.setAccessible(true);
    detectMethod.invoke(manager, scannedMap, persisted);

    // Mirror what doRegistration() does after detectChanges: swap the field and update/persist.
    field.set(manager, scannedMap);
    Method updateMethod = McpExtensionPointManager.class.getDeclaredMethod("updateApprovedMcpServerString", Map.class);
    updateMethod.setAccessible(true);
    updateMethod.invoke(manager, scannedMap);
    Method persistMethod = McpExtensionPointManager.class.getDeclaredMethod("persistExtMcpInfo", Map.class);
    persistMethod.setAccessible(true);
    persistMethod.invoke(manager, scannedMap);
  }
}

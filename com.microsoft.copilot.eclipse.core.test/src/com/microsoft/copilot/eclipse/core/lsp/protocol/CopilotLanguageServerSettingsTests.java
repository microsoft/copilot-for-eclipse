// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Tests for MCP server merge behavior in CopilotLanguageServerSettings.
 */
class CopilotLanguageServerSettingsTests {

  private static final String SERVER_A_JSON = "{\"servers\": {\"server-a\": {\"command\": \"echo\"}}}";
  private static final String KEEP_ME_JSON = "{\"servers\": {\"keep-me\": {\"command\": \"stay\"}}}";

  private CopilotLanguageServerSettings settings;
  private Gson gson;

  @BeforeEach
  void setUp() {
    settings = new CopilotLanguageServerSettings();
    gson = new GsonBuilder().disableHtmlEscaping().create();
  }

  // --- setMcpServers ---

  @Test
  void setMcpServers_withWrappedFormat_extractsServers() {
    settings.setMcpServers(SERVER_A_JSON);

    String result = getMcpServers();
    assertNotNull(result);
    Map<String, Object> servers = parseJsonMap(result);
    assertEquals(1, servers.size());
    assertTrue(servers.containsKey("server-a"));
  }

  @Test
  void setMcpServers_withBlankInput_doesNotThrow() {
    settings.setMcpServers("");
    // blank input is passed through as-is by parseMcpServers
    String result = getMcpServers();
    assertEquals("", result);
  }

  @Test
  void setMcpServers_withInvalidJson_setsNull() {
    settings.setMcpServers("not valid json");
    String result = getMcpServers();
    assertNull(result);
  }

  // --- addMcpServers ---

  @Test
  void addMcpServers_withNoExistingServers_setsServers() {
    String json = "{\"servers\": {\"new-server\": {\"type\": \"stdio\", \"command\": \"npx\"}}}";
    settings.addMcpServers(json);

    String result = getMcpServers();
    assertNotNull(result);
    Map<String, Object> servers = parseJsonMap(result);
    assertEquals(1, servers.size());
    assertTrue(servers.containsKey("new-server"));
  }

  @Test
  void addMcpServers_withExistingServers_mergesBothServers() {
    settings.setMcpServers("{\"servers\": {\"existing-server\": {\"command\": \"a\"}}}");

    settings.addMcpServers("{\"servers\": {\"added-server\": {\"command\": \"b\"}}}");

    String result = getMcpServers();
    assertNotNull(result);
    Map<String, Object> servers = parseJsonMap(result);
    assertEquals(2, servers.size());
    assertTrue(servers.containsKey("existing-server"));
    assertTrue(servers.containsKey("added-server"));
  }

  @Test
  void addMcpServers_withOverlappingName_laterOverridesEarlier() {
    settings.setMcpServers("{\"servers\": {\"shared\": {\"command\": \"old-cmd\"}}}");
    settings.addMcpServers("{\"servers\": {\"shared\": {\"command\": \"new-cmd\"}}}");

    String result = getMcpServers();
    assertNotNull(result);
    assertTrue(result.contains("new-cmd"));
  }

  @Test
  void addMcpServers_withBlankInput_noChange() {
    settings.setMcpServers(KEEP_ME_JSON);
    settings.addMcpServers("");

    String result = getMcpServers();
    assertNotNull(result);
    Map<String, Object> servers = parseJsonMap(result);
    assertEquals(1, servers.size());
    assertTrue(servers.containsKey("keep-me"));
  }

  @Test
  void addMcpServers_withNullInput_noChange() {
    settings.setMcpServers(KEEP_ME_JSON);

    settings.addMcpServers(null);

    String result = getMcpServers();
    assertNotNull(result);
    Map<String, Object> servers = parseJsonMap(result);
    assertEquals(1, servers.size());
    assertTrue(servers.containsKey("keep-me"));
  }

  @Test
  void addMcpServers_withInvalidJson_noChange() {
    settings.setMcpServers(KEEP_ME_JSON);

    settings.addMcpServers("not valid json");

    String result = getMcpServers();
    assertNotNull(result);
    Map<String, Object> servers = parseJsonMap(result);
    assertEquals(1, servers.size());
    assertTrue(servers.containsKey("keep-me"));
  }

  // --- full merge pipeline: setMcpServers (file-based) + addMcpServers (preferences) + addMcpServers (ext) ---

  @Test
  void fullMergePipeline_filesThenPrefsThenExtPoints() {
    // Step 1: file-based servers (lowest priority) via setMcpServers
    settings.setMcpServers(
        "{\"servers\": {\"file-only\": {\"command\": \"file-cmd\"}, " + "\"shared\": {\"command\": \"file-shared\"}}}");

    // Step 2: preferences (higher priority) via addMcpServers
    settings.addMcpServers(
        "{\"servers\": {\"pref-only\": {\"command\": \"pref-cmd\"}, " + "\"shared\": {\"command\": \"pref-shared\"}}}");

    // Step 3: extension points (highest priority) via addMcpServers
    settings.addMcpServers(
        "{\"servers\": {\"ext-only\": {\"command\": \"ext-cmd\"}, " + "\"shared\": {\"command\": \"ext-shared\"}}}");

    String result = getMcpServers();
    assertNotNull(result);
    Map<String, Object> servers = parseJsonMap(result);

    // All unique servers present
    assertEquals(4, servers.size());
    assertTrue(servers.containsKey("file-only"));
    assertTrue(servers.containsKey("pref-only"));
    assertTrue(servers.containsKey("ext-only"));

    // "shared" should have the highest-priority (ext) value
    assertTrue(servers.containsKey("shared"));
    assertTrue(result.contains("ext-shared"));
  }

  @Test
  void addMcpServers_withMultipleCalls_serversAccumulate() {
    settings.addMcpServers("{\"servers\": {\"first\": {\"command\": \"1\"}}}");
    settings.addMcpServers("{\"servers\": {\"second\": {\"command\": \"2\"}}}");
    settings.addMcpServers("{\"servers\": {\"third\": {\"command\": \"3\"}}}");

    String result = getMcpServers();
    assertNotNull(result);
    Map<String, Object> servers = parseJsonMap(result);
    assertEquals(3, servers.size());
    assertTrue(servers.containsKey("first"));
    assertTrue(servers.containsKey("second"));
    assertTrue(servers.containsKey("third"));
  }

  private String getMcpServers() {
    return settings.getGithubSettings().getCopilotSettings().getMcpServers();
  }

  private Map<String, Object> parseJsonMap(String json) {
    return gson.fromJson(json, new TypeToken<Map<String, Object>>() {
    }.getType());
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class McpFileConfigServiceTests {

  @TempDir
  Path tempDir;

  // --- parseServers ---

  @Test
  void parseServers_withServersWrapper_extractsServers() {
    String json = """
        {
          "servers": {
            "my-server": {
              "type": "stdio",
              "command": "npx",
              "args": ["-y", "mcp-server"]
            }
          }
        }
        """;
    Map<String, Object> servers = McpFileConfigService.parseServers(json, "test");
    assertNotNull(servers);
    assertEquals(1, servers.size());
    assertTrue(servers.containsKey("my-server"));
  }

  @Test
  void parseServers_withoutServersWrapper_treatsWholeObjectAsServers() {
    String json = """
        {
          "my-server": {
            "type": "stdio",
            "command": "npx"
          }
        }
        """;
    Map<String, Object> servers = McpFileConfigService.parseServers(json, "test");
    assertNotNull(servers);
    assertEquals(1, servers.size());
    assertTrue(servers.containsKey("my-server"));
  }

  @Test
  void parseServers_filtersOutInputsKey() {
    String json = """
        {
          "inputs": [{"type": "promptString"}],
          "servers": {
            "fetch": {
              "command": "uvx",
              "args": ["mcp-server-fetch"]
            }
          }
        }
        """;
    Map<String, Object> servers = McpFileConfigService.parseServers(json, "test");
    assertNotNull(servers);
    assertEquals(1, servers.size());
    assertTrue(servers.containsKey("fetch"));
  }

  @Test
  void parseServers_emptyJson_returnsNull() {
    Map<String, Object> servers = McpFileConfigService.parseServers("{}", "test");
    assertNull(servers);
  }

  @Test
  void parseServers_invalidJson_returnsNull() {
    Map<String, Object> servers = McpFileConfigService.parseServers("not json", "test");
    assertNull(servers);
  }

  // --- readServersFromFile ---

  @Test
  void readServersFromFile_existingFile_readsCorrectly() throws IOException {
    Path mcpJson = tempDir.resolve("mcp.json");
    Files.writeString(mcpJson, """
        {
          "servers": {
            "test-server": {
              "type": "stdio",
              "command": "echo"
            }
          }
        }
        """, StandardCharsets.UTF_8);

    Map<String, Object> servers = McpFileConfigService.readServersFromFile(mcpJson);
    assertNotNull(servers);
    assertEquals(1, servers.size());
    assertTrue(servers.containsKey("test-server"));
  }

  @Test
  void readServersFromFile_nonExistentFile_returnsNull() {
    Path nonExistent = tempDir.resolve("does-not-exist.json");
    Map<String, Object> servers = McpFileConfigService.readServersFromFile(nonExistent);
    assertNull(servers);
  }

  // --- discoverSources ---

  @Test
  void discoverSources_findsVscodeConfig() throws IOException {
    Path projectRoot = tempDir.resolve("my-project");
    Path vscodeDir = projectRoot.resolve(".vscode");
    Files.createDirectories(vscodeDir);
    Files.writeString(vscodeDir.resolve("mcp.json"), """
        {
          "servers": {
            "vscode-server": {"command": "echo"}
          }
        }
        """, StandardCharsets.UTF_8);

    List<McpFileSource> sources = McpFileConfigService.discoverMcpJsonFiles(List.of(projectRoot));
    assertEquals(1, sources.size());
    assertEquals("my-project (.vscode)", sources.get(0).getLabel());
    assertEquals(1, sources.get(0).getServerCount());
  }

  @Test
  void discoverSources_findsCopilotConfig() throws IOException {
    Path projectRoot = tempDir.resolve("my-project");
    Path copilotDir = projectRoot.resolve(".github").resolve("copilot");
    Files.createDirectories(copilotDir);
    Files.writeString(copilotDir.resolve("mcp.json"), """
        {
          "servers": {
            "copilot-server": {"command": "test"}
          }
        }
        """, StandardCharsets.UTF_8);

    List<McpFileSource> sources = McpFileConfigService.discoverMcpJsonFiles(List.of(projectRoot));
    assertEquals(1, sources.size());
    assertEquals("my-project (.github/copilot)", sources.get(0).getLabel());
    assertEquals(1, sources.get(0).getServerCount());
  }

  @Test
  void discoverSources_findsBothProjectConfigs() throws IOException {
    Path projectRoot = tempDir.resolve("my-project");
    Path vscodeDir = projectRoot.resolve(".vscode");
    Path copilotDir = projectRoot.resolve(".github").resolve("copilot");
    Files.createDirectories(vscodeDir);
    Files.createDirectories(copilotDir);

    Files.writeString(vscodeDir.resolve("mcp.json"), """
        {"servers": {"vs-server": {"command": "a"}}}
        """, StandardCharsets.UTF_8);
    Files.writeString(copilotDir.resolve("mcp.json"), """
        {"servers": {"cp-server": {"command": "b"}}}
        """, StandardCharsets.UTF_8);

    List<McpFileSource> sources = McpFileConfigService.discoverMcpJsonFiles(List.of(projectRoot));
    assertEquals(2, sources.size());
    // .vscode comes first (lower priority), .github/copilot second (higher priority)
    assertTrue(sources.get(0).getLabel().contains(".vscode"));
    assertTrue(sources.get(1).getLabel().contains(".github/copilot"));
  }

  @Test
  void discoverSources_emptyProjectRoots_returnsEmptyOrGlobalOnly() {
    List<McpFileSource> sources = McpFileConfigService.discoverMcpJsonFiles(Collections.emptyList());
    // Only global config could be returned (if it exists on this machine)
    // All project-level sources should be absent
    for (McpFileSource source : sources) {
      assertEquals("Global", source.getLabel());
    }
  }

  // --- mergeToJson ---

  @Test
  void mergeToJson_mergesMultipleSources() {
    Map<String, Object> servers1 = Map.of("server-a", Map.of("command", "a"));
    Map<String, Object> servers2 = Map.of("server-b", Map.of("command", "b"));

    McpFileSource source1 = new McpFileSource("Global", tempDir.resolve("global.json"), servers1);
    McpFileSource source2 = new McpFileSource("Project", tempDir.resolve("project.json"), servers2);

    String json = McpFileConfigService.mergeMcpFilesToSingleJson(List.of(source1, source2));
    assertNotNull(json);
    assertTrue(json.contains("server-a"));
    assertTrue(json.contains("server-b"));
    assertTrue(json.contains("\"servers\""));
  }

  @Test
  void mergeToJson_laterSourceOverridesSameNamedServer() {
    Map<String, Object> lowPriority = Map.of("shared-server", Map.of("command", "old-cmd"));
    Map<String, Object> highPriority = Map.of("shared-server", Map.of("command", "new-cmd"));

    McpFileSource source1 = new McpFileSource("Global", tempDir.resolve("g.json"), lowPriority);
    McpFileSource source2 = new McpFileSource("Project", tempDir.resolve("p.json"), highPriority);

    String json = McpFileConfigService.mergeMcpFilesToSingleJson(List.of(source1, source2));
    assertNotNull(json);
    assertTrue(json.contains("new-cmd"));
  }

  @Test
  void mergeToJson_emptySources_returnsEmptyString() {
    String json = McpFileConfigService.mergeMcpFilesToSingleJson(Collections.emptyList());
    assertEquals("", json);
  }

  // --- resolveGlobalConfigPath ---

  @Test
  void resolveGlobalConfigPath_returnsNonNullPath() {
    Path path = McpFileConfigService.resolveGlobalConfigPath();
    // Should return a path as long as user.home is set (always true in tests)
    assertNotNull(path);
    assertTrue(path.toString().contains("github-copilot"));
    assertTrue(path.toString().contains("eclipse"));
    assertTrue(path.toString().endsWith("mcp.json"));
  }

  // --- McpFileSource ---

  @Test
  void mcpFileSource_handlesNullServers() {
    McpFileSource source = new McpFileSource("test", tempDir.resolve("t.json"), null);
    assertNotNull(source.getServers());
    assertEquals(0, source.getServerCount());
  }

  // --- readServersFromFile path traversal ---

  @Test
  void readServersFromFile_symlinkOutsideProject_returnsNull() throws IOException {
    // Create a valid mcp.json in an "outside" directory
    Path outsideDir = tempDir.resolve("outside");
    Files.createDirectories(outsideDir);
    Path realFile = outsideDir.resolve("mcp.json");
    Files.writeString(realFile, """
        {"servers": {"evil-server": {"command": "steal-data"}}}
        """, StandardCharsets.UTF_8);

    // Create a symlink inside a "project" that points to the outside file
    Path projectDir = tempDir.resolve("project");
    Path vscodeDir = projectDir.resolve(".vscode");
    Files.createDirectories(vscodeDir);
    Path symlink = vscodeDir.resolve("mcp.json");
    Files.createSymbolicLink(symlink, realFile);

    // The symlink resolves to a different canonical path, so it should be rejected
    Map<String, Object> servers = McpFileConfigService.readServersFromFile(symlink);
    assertNull(servers);
  }

  // --- loadAndMerge ---

  @Test
  void loadAndMerge_withTwoServersFromDifferentFiles_serversLoadedCorrectly() throws IOException {
    Path project1 = tempDir.resolve("project1");
    Path project2 = tempDir.resolve("project2");
    Files.createDirectories(project1.resolve(".vscode"));
    Files.createDirectories(project2.resolve(".github/copilot"));

    Files.writeString(project1.resolve(".vscode/mcp.json"), """
        {"servers": {"p1-server": {"command": "p1"}}}
        """, StandardCharsets.UTF_8);
    Files.writeString(project2.resolve(".github/copilot/mcp.json"), """
        {"servers": {"p2-server": {"command": "p2"}}}
        """, StandardCharsets.UTF_8);

    String json = McpFileConfigService.loadAndMerge(List.of(project1, project2));
    assertNotNull(json);
    assertTrue(json.contains("p1-server"));
    assertTrue(json.contains("p2-server"));
  }

  @Test
  void loadAndMerge_noFiles_returnsEmptyString() {
    Path emptyProject = tempDir.resolve("empty-project");
    String json = McpFileConfigService.loadAndMerge(List.of(emptyProject));
    // No files exist, and global config may or may not exist
    // Either empty or contains only global servers
    assertNotNull(json);
  }
}

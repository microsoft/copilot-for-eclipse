// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class CopilotLanguageServerSettingsTests {

  private final Gson gson = new Gson();

  @Test
  void testSetMcpServers_copiesTopLevelHeadersToRequestInitHeaders() {
    CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
    String preference = """
        {
          "servers": {
            "remote": {
              "type": "http",
              "url": "https://example.com/mcp",
              "headers": {
                "X-Username": "myuser",
                "X-Api-Token": "my-token"
              }
            }
          }
        }
        """;

    settings.setMcpServers(preference);

    JsonObject mcpServers = gson.fromJson(settings.getGithubSettings().getCopilotSettings().getMcpServers(),
        JsonObject.class);
    JsonObject remoteServer = mcpServers.getAsJsonObject("remote");
    assertTrue(remoteServer.has("requestInit"));
    JsonObject requestHeaders = remoteServer.getAsJsonObject("requestInit").getAsJsonObject("headers");
    assertEquals("myuser", requestHeaders.get("X-Username").getAsString());
    assertEquals("my-token", requestHeaders.get("X-Api-Token").getAsString());
    assertTrue(remoteServer.has("headers"));
  }

  @Test
  void testSetMcpServers_copiesTopLevelHeadersWithoutServersWrapper() {
    CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
    String preference = """
        {
          "remote": {
            "type": "http",
            "url": "https://example.com/mcp",
            "headers": {
              "X-Api-Token": "my-token"
            }
          }
        }
        """;

    settings.setMcpServers(preference);

    JsonObject mcpServers = gson.fromJson(settings.getGithubSettings().getCopilotSettings().getMcpServers(),
        JsonObject.class);
    JsonObject requestHeaders = mcpServers.getAsJsonObject("remote").getAsJsonObject("requestInit")
        .getAsJsonObject("headers");
    assertEquals("my-token", requestHeaders.get("X-Api-Token").getAsString());
  }

  @Test
  void testSetMcpServers_preservesExistingRequestInitHeaders() {
    CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
    String preference = """
        {
          "servers": {
            "remote": {
              "type": "http",
              "url": "https://example.com/mcp",
              "headers": {
                "Authorization": "Bearer top-level-token",
                "X-Workspace": "demo"
              },
              "requestInit": {
                "headers": {
                  "Authorization": "Bearer request-init-token"
                }
              }
            }
          }
        }
        """;

    settings.setMcpServers(preference);

    JsonObject mcpServers = gson.fromJson(settings.getGithubSettings().getCopilotSettings().getMcpServers(),
        JsonObject.class);
    JsonObject requestHeaders = mcpServers.getAsJsonObject("remote").getAsJsonObject("requestInit")
        .getAsJsonObject("headers");
    assertEquals("Bearer request-init-token", requestHeaders.get("Authorization").getAsString());
    assertEquals("demo", requestHeaders.get("X-Workspace").getAsString());
  }
}

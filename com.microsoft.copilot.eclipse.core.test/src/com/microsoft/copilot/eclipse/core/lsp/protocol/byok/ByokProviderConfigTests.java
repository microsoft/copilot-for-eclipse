// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class ByokProviderConfigTests {

  private static final Gson GSON = new Gson();

  @Test
  void testProviderConfig_serializesClsFieldNames() {
    ByokProviderConfig config = new ByokProviderConfig("Ollama", "http://localhost:11434");

    JsonObject json = JsonParser.parseString(GSON.toJson(config)).getAsJsonObject();

    assertEquals("Ollama", json.get("providerName").getAsString());
    assertEquals("http://localhost:11434", json.get("url").getAsString());
  }

  @Test
  void testListProviderConfigResponse_deserializesClsResponse() {
    ByokListProviderConfigResponse response = GSON.fromJson(
        "{\"providers\":[{\"providerName\":\"Ollama\",\"url\":\"http://localhost:11434\"}]}",
        ByokListProviderConfigResponse.class);

    assertEquals(1, response.providers().size());
    assertEquals(new ByokProviderConfig("Ollama", "http://localhost:11434"), response.providers().get(0));
  }
}
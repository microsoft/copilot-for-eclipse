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
  void testListProviderConfigParams_nullProviderSerializesEmptyObject() {
    ByokListProviderConfigParams params = new ByokListProviderConfigParams(null);

    JsonObject json = JsonParser.parseString(GSON.toJson(params)).getAsJsonObject();

    assertEquals(0, json.size());
  }

  @Test
  void testDeleteProviderConfigParams_serializesOnlyProviderName() {
    ByokDeleteProviderConfigParams params = new ByokDeleteProviderConfigParams("Ollama");

    JsonObject json = JsonParser.parseString(GSON.toJson(params)).getAsJsonObject();

    assertEquals(1, json.size());
    assertEquals("Ollama", json.get("providerName").getAsString());
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

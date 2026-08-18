// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class UserPreferenceTests {

  private static final Gson GSON = new Gson();

  @Test
  void testContextWindowPreference_deserializesLegacyStringAndSerializesNumber() {
    UserPreference preference = GSON.fromJson(
        "{\"contextWindowByModel\":{\"gpt-5\":\"1000000\"}}", UserPreference.class);

    assertEquals(1_000_000, preference.getContextWindow("gpt-5"));

    JsonObject serialized = JsonParser.parseString(GSON.toJson(preference)).getAsJsonObject();
    assertTrue(serialized.getAsJsonObject("contextWindowByModel").get("gpt-5").getAsJsonPrimitive().isNumber());
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Tests for ChatReferenceTypeAdapter
 */
class ChatReferenceTypeAdapterTests {

  private Gson gson;

  @BeforeEach
  void setUp() {
    gson = new GsonBuilder().registerTypeAdapterFactory(new ChatReferenceTypeAdapter.Factory()).create();
  }

  @Test
  void testParseReferencesFromString() {
    String json = """
        {
          "references": [
            {
              "type": "directory",
              "uri": "file:/C:/demo/dirs"
            },
            {
              "type": "file",
              "uri": "file:/C:/demo/main.java"
            }
          ]
        }
        """;

    // Parse the JSON to extract references array
    var jsonElement = gson.fromJson(json, com.google.gson.JsonElement.class);
    var referencesArray = jsonElement.getAsJsonObject().getAsJsonArray("references");

    // Parse each reference
    ChatReference[] references = gson.fromJson(referencesArray, ChatReference[].class);

    assertNotNull(references);
    assertEquals(2, references.length);

    // Verify directory reference
    assertTrue(references[0] instanceof DirectoryChatReference);
    DirectoryChatReference dirRef = (DirectoryChatReference) references[0];
    assertEquals("directory", dirRef.getType());
    assertEquals("file:/C:/demo/dirs", dirRef.getUri());

    // Verify file reference
    assertTrue(references[1] instanceof FileChatReference);
    FileChatReference fileRef = (FileChatReference) references[1];
    assertEquals("file", fileRef.getType());
    assertEquals("file:/C:/demo/main.java", fileRef.getUri());
  }
}

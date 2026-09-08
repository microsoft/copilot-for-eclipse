// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsCollection;

/**
 * Tests for input schema deserialization.
 */
class InputSchemaTests {

  @Test
  void testMcpToolInputSchemaAllowsNullableTypeUnion() {
    String json = """
        {
          "name": "nullable-repro",
          "status": "running",
          "tools": [
            {
              "name": "demo_tool",
              "description": "Demo tool with one optional nullable parameter.",
              "_status": "enabled",
              "inputSchema": {
                "type": "object",
                "properties": {
                  "required_arg": {
                    "type": "string",
                    "description": "A required argument."
                  },
                  "optional_arg": {
                    "type": ["string", "null"],
                    "description": "Optional, nullable."
                  },
                  "optional_count": {
                    "type": ["null", "number"],
                    "description": "Optional count."
                  }
                },
                "required": ["required_arg"]
              }
            }
          ]
        }
        """;

    McpServerToolsCollection server = new Gson().fromJson(json, McpServerToolsCollection.class);

    assertNotNull(server);
    assertEquals("nullable-repro", server.getName());
    assertEquals(1, server.getTools().size());
    InputSchemaPropertyValue optionalArg = server.getTools().get(0).getInputSchema().getProperties()
        .get("optional_arg");
    assertNotNull(optionalArg);
    assertEquals("string", optionalArg.getType());
    InputSchemaPropertyValue optionalCount = server.getTools().get(0).getInputSchema().getProperties()
        .get("optional_count");
    assertNotNull(optionalCount);
    assertEquals("number", optionalCount.getType());
  }

  @Test
  void testInputSchemaAllowsTypeUnion() {
    InputSchema schema = new Gson().fromJson("{\"type\":[\"object\",\"null\"]}", InputSchema.class);

    assertNotNull(schema);
    assertEquals("object", schema.getType());
  }

  @Test
  void testInputSchemaRejectsMultipleConcreteTypes() {
    assertThrows(JsonSyntaxException.class,
        () -> new Gson().fromJson("{\"type\":[\"string\",\"number\"]}", InputSchema.class));
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Tests for TransportTypeAdapter polymorphic deserialization.
 */
class TransportTypeAdapterTests {

  @Test
  void testDeserializeSseTransportWithinPackage() {
    String json = """
        {
          "identifier": "demo-server",
          "version": "1.0.0",
          "transport": {
            "type": "sse",
            "url": "http://example.com/events"
          }
        }
        """;

    Gson gson = new GsonBuilder().create();
    Package pkg = gson.fromJson(json, Package.class);

    assertNotNull(pkg, "Package should deserialize");
    Transport transport = pkg.transport();
    assertNotNull(transport, "Transport should deserialize");
    assertTrue(transport instanceof SseTransport, "Transport should be SseTransport");
    assertEquals("sse", transport.getType(), "Type discriminator should be sse");
    assertEquals("http://example.com/events", ((SseTransport) transport).getUrl(), "URL should match input JSON");
  }
}

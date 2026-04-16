// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Gson TypeAdapter to support polymorphic (de)serialization of Transport instances. Chooses the appropriate concrete
 * Transport subclass based on the 'type' discriminator.
 */
public class TransportTypeAdapter extends TypeAdapter<Transport> {
  private final Gson gson = new GsonBuilder().create();

  @Override
  public void write(JsonWriter out, Transport value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    gson.toJson(value, value.getClass(), out);
  }

  @Override
  public Transport read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }

    JsonElement element = JsonParser.parseReader(in);
    if (!element.isJsonObject()) {
      return gson.fromJson(element, Transport.class);
    }
    JsonObject obj = element.getAsJsonObject();
    String type = obj.has("type") && !obj.get("type").isJsonNull() ? obj.get("type").getAsString() : null;

    if (TransportType.stdio.name().equals(type)) {
      return gson.fromJson(obj, StdioTransport.class);
    } else if (TransportType.sse.name().equals(type)) {
      return gson.fromJson(obj, SseTransport.class);
    } else if (TransportType.streamable_http.name().equals(type)) {
      return gson.fromJson(obj, StreamableHttpTransport.class);
    }

    return gson.fromJson(obj, Transport.class);
  }
}

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
 * Gson TypeAdapter to support polymorphic (de)serialization of Argument instances. Chooses {@link PositionalArgument}
 * or {@link NamedArgument} based on the 'type' discriminator.
 */
public class ArgumentTypeAdapter extends TypeAdapter<Argument> {
  private final Gson gson = new GsonBuilder().create();

  @Override
  public void write(JsonWriter out, Argument value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    gson.toJson(value, value.getClass(), out);
  }

  @Override
  public Argument read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }

    JsonElement element = JsonParser.parseReader(in);
    if (!element.isJsonObject()) {
      return gson.fromJson(element, Argument.class);
    }
    JsonObject obj = element.getAsJsonObject();
    String type = obj.has("type") && !obj.get("type").isJsonNull() ? obj.get("type").getAsString() : null;

    if ("positional".equals(type)) {
      return gson.fromJson(obj, PositionalArgument.class);
    } else if ("named".equals(type)) {
      return gson.fromJson(obj, NamedArgument.class);
    }

    return gson.fromJson(obj, Argument.class);
  }
}

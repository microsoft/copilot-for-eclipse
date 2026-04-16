// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * TypeAdapter for serializing and deserializing ChatReference objects.
 */
public class ChatReferenceTypeAdapter extends TypeAdapter<ChatReference> {
  private final Gson gson;

  /**
   * Constructs a new ChatReferenceTypeAdapter with the given Gson instance.
   *
   * @param gson the Gson instance to use for serialization and deserialization
   */
  public ChatReferenceTypeAdapter(Gson gson) {
    this.gson = gson;
  }

  /**
   * Factory to create a TypeAdapter for ChatReference.
   */
  public static final class Factory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {

      if (typeToken.getRawType() != ChatReference.class) {
        return null;
      }
      return (TypeAdapter<T>) new ChatReferenceTypeAdapter(gson);
    }
  }

  @Override
  public ChatReference read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    var elem = JsonParser.parseReader(in);
    if (!elem.isJsonObject()) {
      return null;
    }
    var obj = elem.getAsJsonObject();

    String type = null;
    JsonElement typeEl = obj.get("type");
    if (typeEl != null && typeEl.isJsonPrimitive()) {
      type = typeEl.getAsString();
    }

    if ("file".equalsIgnoreCase(type)) {
      return gson.fromJson(obj, FileChatReference.class);
    }
    if ("directory".equalsIgnoreCase(type)) {
      return gson.fromJson(obj, DirectoryChatReference.class);
    }
    return null;
  }

  @Override
  public void write(JsonWriter out, ChatReference value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    gson.toJson(value, value.getClass(), out);
  }
}

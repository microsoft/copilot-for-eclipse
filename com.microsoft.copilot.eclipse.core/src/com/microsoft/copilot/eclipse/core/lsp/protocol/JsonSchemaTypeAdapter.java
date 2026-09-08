// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.io.IOException;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Gson adapter for JSON Schema type fields.
 */
public class JsonSchemaTypeAdapter extends TypeAdapter<String> {

  @Override
  public String read(JsonReader in) throws IOException {
    JsonToken token = in.peek();
    if (token == JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    if (token == JsonToken.STRING) {
      return in.nextString();
    }
    if (token == JsonToken.BOOLEAN) {
      return Boolean.toString(in.nextBoolean());
    }
    if (token == JsonToken.BEGIN_ARRAY) {
      return readTypeArray(in);
    }

    return in.nextString();
  }

  @Override
  public void write(JsonWriter out, String value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value);
    }
  }

  private String readTypeArray(JsonReader in) throws IOException {
    String concreteType = null;
    boolean nullTypePresent = false;

    in.beginArray();
    while (in.hasNext()) {
      JsonToken token = in.peek();
      if (token == JsonToken.STRING) {
        String type = in.nextString();
        if ("null".equals(type)) {
          nullTypePresent = true;
        } else if (concreteType == null) {
          concreteType = type;
        } else if (!concreteType.equals(type)) {
          throw new JsonSyntaxException("Multiple non-null JSON Schema types are not supported");
        }
      } else {
        in.skipValue();
      }
    }
    in.endArray();

    return concreteType != null ? concreteType : nullTypePresent ? "null" : null;
  }
}

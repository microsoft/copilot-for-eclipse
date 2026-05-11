// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * TypeAdapter for {@link Thinking} that tolerates the server's mixed wire shape for
 * {@code text}: it may be a string delta (e.g. {@code "text":"The"}) or an array
 * (e.g. {@code "text":[]}) when the report carries only an encrypted payload, or a
 * multi-fragment array of deltas. The array form is concatenated into a single string
 * (or {@code null} when empty) so the rest of the UI treats the report as scalar.
 */
public class ThinkingTypeAdapter extends TypeAdapter<Thinking> {
  private final TypeAdapter<Thinking> delegate;
  private final TypeAdapter<JsonElement> elementAdapter;

  /**
   * Construct a new adapter that delegates to the given default adapter after the
   * {@code text} field has been normalized.
   *
   * @param delegate the Gson-generated default adapter for {@link Thinking}
   * @param elementAdapter the adapter used to read the JSON tree
   */
  public ThinkingTypeAdapter(TypeAdapter<Thinking> delegate, TypeAdapter<JsonElement> elementAdapter) {
    this.delegate = delegate;
    this.elementAdapter = elementAdapter;
  }

  /** TypeAdapterFactory for {@link Thinking}. */
  public static final class Factory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
      if (typeToken.getRawType() != Thinking.class) {
        return null;
      }
      TypeAdapter<Thinking> defaultAdapter = gson.getDelegateAdapter(this, TypeToken.get(Thinking.class));
      TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
      return (TypeAdapter<T>) new ThinkingTypeAdapter(defaultAdapter, elementAdapter);
    }
  }

  @Override
  public Thinking read(JsonReader in) throws IOException {
    JsonElement element = elementAdapter.read(in);
    if (element != null && element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();
      JsonElement text = obj.get("text");
      if (text != null && text.isJsonArray()) {
        obj.add("text", flattenTextArray(text.getAsJsonArray()));
      }
    }
    return delegate.fromJsonTree(element);
  }

  private static JsonElement flattenTextArray(JsonArray arr) {
    StringBuilder sb = new StringBuilder();
    for (JsonElement e : arr) {
      if (!e.isJsonNull() && e.isJsonPrimitive()) {
        JsonPrimitive prim = e.getAsJsonPrimitive();
        if (prim.isString()) {
          sb.append(prim.getAsString());
        }
      }
    }
    return sb.length() == 0 ? JsonNull.INSTANCE : new JsonPrimitive(sb.toString());
  }

  @Override
  public void write(JsonWriter out, Thinking value) throws IOException {
    delegate.write(out, value);
  }
}

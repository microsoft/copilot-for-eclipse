// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Chat progress params adapter.
 */
public class ProgressParamsAdapter extends TypeAdapter<ProgressParams> {

  private final Gson gson;

  /**
   * Constructor.
   */
  public ProgressParamsAdapter(Gson gson) {
    this.gson = gson;
  }

  /**
   * Factory for ProgressParamsAdapter.
   */
  public static class Factory implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
      Class<?> rawType = typeToken.getRawType();
      if (!ProgressParams.class.isAssignableFrom(rawType)) {
        return null;
      }
      return (TypeAdapter<T>) new ProgressParamsAdapter(gson);
    }

  }

  @Override
  public ProgressParams read(JsonReader in) {
    // TODO: leverage ProgressNotificationAdapter to read the progress params
    ProgressParams ret = new ProgressParams();
    JsonElement element = gson.fromJson(in, JsonElement.class);
    if (element == null || !element.isJsonObject()) {
      return ret;
    }

    JsonObject object = element.getAsJsonObject();
    parseToken(object.get("token"), ret);
    parseValue(object.get("value"), ret);
    return ret;
  }

  @Override
  public void write(JsonWriter out, ProgressParams value) {
    // TODO: leverage ProgressNotificationAdapter to write the progress params
    try {
      JsonObject object = new JsonObject();
      addToken(object, value.getToken());
      addValue(object, value.getValue());
      gson.toJson(object, out);
    } catch (Exception e) {
      CopilotCore.LOGGER.error(e);
    }
  }

  private void parseToken(JsonElement tokenElement, ProgressParams params) {
    if (tokenElement == null || tokenElement.isJsonNull()) {
      return;
    }

    if (!tokenElement.isJsonPrimitive()) {
      return;
    }

    JsonPrimitive primitive = tokenElement.getAsJsonPrimitive();
    if (primitive.isString()) {
      params.setToken(Either.forLeft(primitive.getAsString()));
    } else if (primitive.isNumber()) {
      params.setToken(Either.forRight(primitive.getAsInt()));
    }
  }

  private void parseValue(JsonElement valueElement, ProgressParams params) {
    if (valueElement == null || valueElement.isJsonNull()) {
      return;
    }

    if (isChatProgress(valueElement)) {
      ChatProgressValue chatProgress = gson.fromJson(valueElement, ChatProgressValue.class);
      params.setValue(Either.forLeft(chatProgress));
      return;
    }

    Object genericValue = gson.fromJson(valueElement, Object.class);
    params.setValue(Either.forRight(genericValue));
  }

  private void addToken(JsonObject object, Either<String, Integer> token) {
    if (token == null) {
      return;
    }
    if (token.isLeft()) {
      object.addProperty("token", token.getLeft());
    } else {
      object.addProperty("token", token.getRight());
    }
  }

  private void addValue(JsonObject object, Either<?, ?> value) {
    if (value == null) {
      return;
    }

    JsonElement valueJson;
    if (value.isLeft()) {
      valueJson = gson.toJsonTree(value.getLeft());
    } else {
      valueJson = gson.toJsonTree(value.getRight());
    }
    object.add("value", valueJson);
  }

  private boolean isChatProgress(JsonElement valueElement) {
    if (valueElement == null || !valueElement.isJsonObject()) {
      return false;
    }
    JsonObject obj = valueElement.getAsJsonObject();
    JsonElement kind = obj.get("kind");
    if (kind == null || !kind.isJsonPrimitive()) {
      return false;
    }
    JsonPrimitive primitive = kind.getAsJsonPrimitive();
    if (!primitive.isString()) {
      return false;
    }
    String kindValue = primitive.getAsString();
    return "begin".equals(kindValue) || "report".equals(kindValue) || "end".equals(kindValue);
  }
}

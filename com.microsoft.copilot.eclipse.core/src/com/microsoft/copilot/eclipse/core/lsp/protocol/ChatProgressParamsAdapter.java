package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.io.IOException;

import com.google.gson.Gson;
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
public class ChatProgressParamsAdapter extends TypeAdapter<ProgressParams> {

  private final Gson gson;

  /**
   * Constructs a new ChatProgressParamsAdapter with the given Gson instance.
   *
   * @param gson the Gson instance to use for serialization and deserialization
   */
  public ChatProgressParamsAdapter(Gson gson) {
    this.gson = gson;
  }

  /**
   * Creates a new ChatProgressParams.
   */
  public static class Factory implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
      Class<?> rawType = typeToken.getRawType();
      if (!ProgressParams.class.isAssignableFrom(rawType)) {
        return null;
      }
      return (TypeAdapter<T>) new ChatProgressParamsAdapter(gson);
    }

  }

  @Override
  public ProgressParams read(JsonReader in) {
    // TODO: leverage ProgressNotificationAdapter to read the progress params
    ProgressParams ret = new ProgressParams();
    // parse token and value from the input stream and set them in the ret object
    try {
      in.beginObject();
      while (in.hasNext()) {
        var name = in.nextName();
        switch (name) {
          case "token":
            ret.setToken(in.nextString());
            break;
          case "value":
            ChatProgressValue val = gson.fromJson(in, ChatProgressValue.class);
            ret.setValue(Either.forLeft(val));
            break;
          default:
            in.skipValue();
            break;
        }
      }
      in.endObject();
    } catch (IOException e) {
      CopilotCore.LOGGER.error(e);
    }
    return ret;
  }

  @Override
  public void write(JsonWriter out, ProgressParams value) {
    // TODO: leverage ProgressNotificationAdapter to write the progress params
    var chatProgress = (ChatProgressValue) value.getValue().getLeft();
    try {
      out.beginObject();
      out.name("token").value(value.getToken().getLeft());
      out.name("value").value(gson.toJson(chatProgress));
      out.endObject();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

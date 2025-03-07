package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * A turn in a conversation.
 */
public class Turn {
  @NonNull
  String request;
  String response;
  String agentSlug;

  /**
   * Creates a new Turn.
   */
  public Turn(@NonNull String request, String response, String agentSlug) {
    this.request = request;
    this.response = response;
    this.agentSlug = agentSlug;
  }

  public String getRequest() {
    return request;
  }

  public String getResponse() {
    return response;
  }

  public String getAgentSlug() {
    return agentSlug;
  }

  public void setRequest(String request) {
    this.request = request;
  }

  public void setResponse(String response) {
    this.response = response;
  }

  public void setAgentSlug(String agentSlug) {
    this.agentSlug = agentSlug;
  }

  @Override
  public int hashCode() {
    return Objects.hash(request, response, agentSlug);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Turn turn = (Turn) o;
    return Objects.equals(request, turn.request) && Objects.equals(response, turn.response)
        && Objects.equals(agentSlug, turn.agentSlug);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("request", request);
    builder.add("response", response);
    builder.add("agentSlug", agentSlug);
    return builder.toString();
  }
}

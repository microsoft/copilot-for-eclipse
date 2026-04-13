package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import com.google.gson.annotations.JsonAdapter;

/**
 * Abstract base class representing a transport mechanism.
 */
@JsonAdapter(TransportTypeAdapter.class)
public abstract class Transport {
  private final String type;

  /**
   * Constructor for Transport.
   *
   * @param type The type of transport.
   */
  protected Transport(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
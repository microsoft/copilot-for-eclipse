// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Enum representing the chat mode.
 */
// TODO: now there is some issue with the enum type adapter, so we need to call .toString() or .valueOf() for conversion
public enum ChatMode {

  /**
   * Normal chat mode.
   */
  Ask {
    @Override
    public String displayName() {
      return "Ask";
    }
  },

  /**
   * Agent mode.
   */
  Agent {
    @Override
    public String displayName() {
      return "Agent";
    }
  };

  /**
   * Returns a human-readable display name for the chat mode.
   *
   * @return The display name for this chat mode.
   */
  public abstract String displayName();
}

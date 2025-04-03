package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Enum representing the chat mode.
 */
// TODO: now there is some issue with the enum type adapter, so we need to call .toString() or .valueOf() for conversion
public enum ChatMode {

  /**
   * Normal chat mode.
   */
  Ask,

  /**
   * Agent mode.
   */
  Agent;
}
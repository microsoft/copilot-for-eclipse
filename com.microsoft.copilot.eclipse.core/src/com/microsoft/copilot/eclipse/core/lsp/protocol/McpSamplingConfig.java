// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;

/**
 * User preferences for MCP sampling requests from a server.
 *
 * @param alwaysAllow whether the user chose to always allow sampling from this server
 * @param alwaysDeny whether the user chose to always deny sampling from this server (not yet
 *     surfaced by this dialog; always {@code false})
 * @param allowedModels the models the user has restricted sampling to (not yet surfaced by
 *     this dialog; always empty)
 * @param allowedDuringChat whether sampling is allowed while an interactive chat session is
 *     active; this IDE does not yet distinguish in-chat from out-of-chat sampling requests, so
 *     it always mirrors {@link #alwaysAllow}
 * @param allowedOutsideChat whether sampling is allowed outside of an interactive chat session
 *     (e.g. background/agent jobs); this IDE does not yet distinguish these contexts, so it
 *     always mirrors {@link #alwaysAllow}
 */
public record McpSamplingConfig(boolean alwaysAllow, boolean alwaysDeny, List<String> allowedModels,
    boolean allowedDuringChat, boolean allowedOutsideChat) {

  /**
   * Creates a config where {@code allowedDuringChat} and {@code allowedOutsideChat} both mirror
   * {@code alwaysAllow}, since this IDE does not yet distinguish between in-chat and
   * out-of-chat sampling requests.
   *
   * @param alwaysAllow whether the user chose to always allow sampling from this server
   * @param alwaysDeny whether the user chose to always deny sampling from this server
   * @param allowedModels the models the user has restricted sampling to
   */
  public McpSamplingConfig(boolean alwaysAllow, boolean alwaysDeny, List<String> allowedModels) {
    this(alwaysAllow, alwaysDeny, allowedModels, alwaysAllow, alwaysAllow);
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link McpSamplingConfig}.
 */
class McpSamplingConfigTests {

  @Test
  void threeArgConstructor_mirrorsAlwaysAllowIntoChatContextFlags_whenApproved() {
    McpSamplingConfig config = new McpSamplingConfig(true, false, List.of());

    assertTrue(config.allowedDuringChat());
    assertTrue(config.allowedOutsideChat());
  }

  @Test
  void threeArgConstructor_mirrorsAlwaysAllowIntoChatContextFlags_whenNotApproved() {
    McpSamplingConfig config = new McpSamplingConfig(false, false, List.of());

    assertFalse(config.allowedDuringChat());
    assertFalse(config.allowedOutsideChat());
  }

  @Test
  void fiveArgConstructor_allowsChatContextFlagsToBeSetIndependently() {
    McpSamplingConfig config = new McpSamplingConfig(false, false, List.of(), true, false);

    assertFalse(config.alwaysAllow());
    assertTrue(config.allowedDuringChat());
    assertFalse(config.allowedOutsideChat());
  }
}

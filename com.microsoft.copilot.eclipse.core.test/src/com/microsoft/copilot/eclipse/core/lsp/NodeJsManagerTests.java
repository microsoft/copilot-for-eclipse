package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class NodeJsManagerTests {

  @Test
  void testGetNodeInstallation() {
    assertNotNull(NodeJsManager.getNodeJsLocation());
  }

}

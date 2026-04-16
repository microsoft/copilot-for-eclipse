// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModelCapabilities;

/**
 * Tests for ModelUtils utility class.
 */
class ModelUtilsTests {

  @Test
  void testConvertByokModelToCopilotModel_basicConversion() {
    ByokModel byokModel = new ByokModel();
    byokModel.setModelId("test-model");
    byokModel.setProviderName("Azure");

    CopilotModel result = ModelUtils.convertByokModelToCopilotModel(byokModel);

    assertNotNull(result);
    assertEquals("test-model", result.getId());
    assertEquals("test-model", result.getModelName());
    assertEquals("Azure", result.getProviderName());
    assertEquals(1, result.getScopes().size());
    assertTrue(result.getScopes().contains(CopilotScope.CHAT_PANEL));
  }

  @Test
  void testConvertByokModelToCopilotModel_withToolCallingCapability() {
    ByokModel byokModel = new ByokModel();
    byokModel.setModelId("gpt-4");
    byokModel.setProviderName("OpenAI");

    ByokModelCapabilities capabilities = new ByokModelCapabilities();
    capabilities.setName("GPT-4 Model");
    capabilities.setVision(true);
    capabilities.setToolCalling(true);
    byokModel.setModelCapabilities(capabilities);

    CopilotModel result = ModelUtils.convertByokModelToCopilotModel(byokModel);

    assertEquals("GPT-4 Model", result.getModelName());
    assertNotNull(result.getCapabilities());
    assertTrue(result.getCapabilities().supports().vision());
    assertEquals(2, result.getScopes().size());
    assertTrue(result.getScopes().contains(CopilotScope.CHAT_PANEL));
    assertTrue(result.getScopes().contains(CopilotScope.AGENT_PANEL));
  }
}

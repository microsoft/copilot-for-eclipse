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

  @Test
  void testConvertByokModelToCopilotModel_preservesTokenLimits() {
    ByokModel byokModel = new ByokModel();
    byokModel.setModelId("gpt-4.1");

    ByokModelCapabilities capabilities = new ByokModelCapabilities();
    capabilities.setMaxInputTokens(128000);
    capabilities.setMaxOutputTokens(16000);
    byokModel.setModelCapabilities(capabilities);

    CopilotModel result = ModelUtils.convertByokModelToCopilotModel(byokModel);

    assertNotNull(result.getCapabilities());
    assertNotNull(result.getCapabilities().limits());
    assertEquals(-1, result.getCapabilities().limits().maxContextWindowTokens());
    assertEquals(128000, result.getCapabilities().limits().maxInputTokens());
    assertEquals(16000, result.getCapabilities().limits().maxOutputTokens());
  }
}

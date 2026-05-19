// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesLimits;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesSupports;
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
    assertNull(result.getCapabilities().limits().maxContextWindowTokens());
    assertEquals(128000, result.getCapabilities().limits().maxInputTokens());
    assertEquals(16000, result.getCapabilities().limits().maxOutputTokens());
  }

  @Test
  void testResolveDefaultReasoningEffort_prefersMediumForClaudeModels() {
    CopilotModel model = new CopilotModel();
    model.setModelFamily("claude-3.7-sonnet");
    model.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(false, List.of("low", "medium", "high"), true),
        new CopilotModelCapabilitiesLimits(null, null, null, null)));

    assertEquals("medium", ModelUtils.resolveDefaultReasoningEffort(model));
  }

  @Test
  void testResolveDefaultReasoningEffort_returnsNullWhenSupportsReasoningEffortLevelFalse() {
    CopilotModel model = new CopilotModel();
    model.setModelFamily("gpt-4o");
    // efforts list is populated, but the server has not vetted the model as supporting effort selection
    model.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(false, List.of("low", "medium", "high"), false),
        new CopilotModelCapabilitiesLimits(null, null, null, null)));

    assertNull(ModelUtils.resolveDefaultReasoningEffort(model));
  }

  @Test
  void testSupportsReasoningEffortLevel_trueWhenCapabilityFlagSet() {
    CopilotModel model = new CopilotModel();
    model.setModelName("gpt-5");
    model.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(false, List.of("low", "medium", "high"), true),
        new CopilotModelCapabilitiesLimits(null, null, null, null)));

    assertTrue(ModelUtils.supportsReasoningEffortLevel(model));
  }

  @Test
  void testSupportsReasoningEffortLevel_falseWhenCapabilityFlagUnset() {
    CopilotModel model = new CopilotModel();
    model.setModelName("gpt-4o");
    model.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(false, List.of("low", "medium", "high"), false),
        new CopilotModelCapabilitiesLimits(null, null, null, null)));

    assertFalse(ModelUtils.supportsReasoningEffortLevel(model));
  }

  @Test
  void testSupportsReasoningEffortLevel_falseForAutoModel() {
    CopilotModel model = new CopilotModel();
    model.setModelName("Auto");
    // Even if the server were to advertise the capability, the Auto model routes to other models and does not
    // own its own effort selection.
    model.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(false, List.of("low", "medium", "high"), true),
        new CopilotModelCapabilitiesLimits(null, null, null, null)));

    assertFalse(ModelUtils.supportsReasoningEffortLevel(model));
  }

  @Test
  void testSupportsReasoningEffortLevel_falseWhenCapabilitiesMissing() {
    CopilotModel model = new CopilotModel();
    model.setModelName("gpt-5");

    assertFalse(ModelUtils.supportsReasoningEffortLevel(model));
    assertFalse(ModelUtils.supportsReasoningEffortLevel(null));
  }

  @Test
  void testIsAutoModel() {
    CopilotModel auto = new CopilotModel();
    auto.setModelName("Auto");
    assertTrue(ModelUtils.isAutoModel(auto));

    CopilotModel autoLower = new CopilotModel();
    autoLower.setModelName("auto");
    assertFalse(ModelUtils.isAutoModel(autoLower));

    CopilotModel other = new CopilotModel();
    other.setModelName("gpt-5");
    assertFalse(ModelUtils.isAutoModel(other));

    assertFalse(ModelUtils.isAutoModel(null));
  }
}

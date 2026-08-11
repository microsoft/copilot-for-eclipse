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
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelBilling;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelBillingTokenPrices;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesLimits;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesSupports;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCustomModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelTokenPriceTier;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils.ContextWindowOption;
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
    model.setId("auto");
    model.setModelName("Automatic");
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
  void testGetModelSuffix_customModelUsesProvider() {
    // Organization-contributed custom models arrive without a providerName but carry their provider in the
    // custom-model metadata; the suffix should surface that provider like a BYOK model.
    CopilotModel model = new CopilotModel();
    model.setModelName("Sonnet (Org)");
    model.setCustomModel(new CopilotModelCustomModel("Contoso Azure Key", "Contoso", "organization", "Azure"));

    assertEquals("Azure", ModelUtils.getModelSuffix(model, null, null));
  }

  @Test
  void testGetModelSuffix_providerNameTakesPrecedenceOverCustomModel() {
    CopilotModel model = new CopilotModel();
    model.setModelName("GPT-4o");
    model.setProviderName("OpenAI");
    model.setCustomModel(new CopilotModelCustomModel("Key", "Contoso", "organization", "Azure"));

    assertEquals("OpenAI", ModelUtils.getModelSuffix(model, null, null));
  }

  @Test
  void testGetModelSuffix_autoUsesStableModelId() {
    CopilotModel auto = new CopilotModel();
    auto.setId("auto");
    auto.setModelName("Automatic");
    assertEquals("Variable", ModelUtils.getModelSuffix(auto, null, null));

    CopilotModel matchingDisplayName = new CopilotModel();
    matchingDisplayName.setId("gpt-5");
    matchingDisplayName.setModelName("Auto");
    assertEquals("", ModelUtils.getModelSuffix(matchingDisplayName, null, null));
  }

  @Test
  void testIsAutoModel_usesStableModelId() {
    CopilotModel auto = new CopilotModel();
    auto.setId("auto");
    auto.setModelName("Automatic");
    assertTrue(ModelUtils.isAutoModel(auto));

    CopilotModel matchingDisplayName = new CopilotModel();
    matchingDisplayName.setId("gpt-5");
    matchingDisplayName.setModelName("Auto");
    assertFalse(ModelUtils.isAutoModel(matchingDisplayName));

    assertFalse(ModelUtils.isAutoModel(null));
  }

  private static CopilotModel modelWithTiers(Integer defaultMaxContext, Integer longContextMaxContext,
      Integer maxOutputTokens) {
    CopilotModel model = new CopilotModel();
    model.setModelName("gpt-5");
    model.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(false, null, false),
        new CopilotModelCapabilitiesLimits(null, maxOutputTokens, null, null)));
    CopilotModelTokenPriceTier defaultTier = defaultMaxContext == null ? null
        : new CopilotModelTokenPriceTier(null, 1.0, 2.0, defaultMaxContext);
    CopilotModelTokenPriceTier longContextTier = longContextMaxContext == null ? null
        : new CopilotModelTokenPriceTier(null, 3.0, 4.0, longContextMaxContext);
    model.setBilling(new CopilotModelBilling(true, 1.0, true,
        new CopilotModelBillingTokenPrices(1_000_000.0, defaultTier, longContextTier)));
    return model;
  }

  @Test
  void testGetContextWindowOptions_returnsOnePerTier() {
    CopilotModel model = modelWithTiers(128000, 1000000, 16000);

    List<ContextWindowOption> options = ModelUtils.getContextWindowOptions(model);

    assertEquals(2, options.size());
    assertTrue(options.get(0).isDefault());
    assertEquals(128000, options.get(0).maxContext());
    assertFalse(options.get(1).isDefault());
    assertEquals(1000000, options.get(1).maxContext());
  }

  @Test
  void testGetContextWindowOptions_deduplicatesEquivalentTiersWithDefaultPrecedence() {
    CopilotModel model = modelWithTiers(1000000, 1000000, 16000);

    List<ContextWindowOption> options = ModelUtils.getContextWindowOptions(model);

    assertEquals(1, options.size());
    assertTrue(options.get(0).isDefault());
    assertEquals(1000000, options.get(0).maxContext());
    assertFalse(ModelUtils.supportsContextWindowSelection(model));
  }

  @Test
  void testGetContextWindowOptions_emptyWhenNoTokenPrices() {
    CopilotModel model = new CopilotModel();
    model.setModelName("gpt-5");
    model.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(false, null, false),
        new CopilotModelCapabilitiesLimits(200000, 16000, null, null)));

    assertTrue(ModelUtils.getContextWindowOptions(model).isEmpty());
  }

  @Test
  void testGetContextWindowOptions_defaultTierFallsBackToMaxContextWindowTokens() {
    CopilotModel model = new CopilotModel();
    model.setModelName("gpt-5");
    model.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(false, null, false),
        new CopilotModelCapabilitiesLimits(200000, 16000, null, null)));
    // Default tier without its own maxContext -> falls back to maxContextWindowTokens (200000).
    model.setBilling(new CopilotModelBilling(true, 1.0, true, new CopilotModelBillingTokenPrices(1_000_000.0,
        new CopilotModelTokenPriceTier(null, 1.0, 2.0, null), null)));

    List<ContextWindowOption> options = ModelUtils.getContextWindowOptions(model);

    assertEquals(1, options.size());
    assertEquals(200000, options.get(0).maxContext());
  }

  @Test
  void testGetContextWindowDisplaySize_addsMaxOutputWhenTierHasMaxContext() {
    CopilotModel model = modelWithTiers(128000, 1000000, 16000);
    List<ContextWindowOption> options = ModelUtils.getContextWindowOptions(model);

    assertEquals(144000, ModelUtils.getContextWindowDisplaySize(model, options.get(0)));
    assertEquals(1016000, ModelUtils.getContextWindowDisplaySize(model, options.get(1)));
  }

  @Test
  void testSupportsContextWindowSelection_trueOnlyForMultipleTiers() {
    assertTrue(ModelUtils.supportsContextWindowSelection(modelWithTiers(128000, 1000000, 16000)));
    assertFalse(ModelUtils.supportsContextWindowSelection(modelWithTiers(128000, null, 16000)));
    assertFalse(ModelUtils.supportsContextWindowSelection(new CopilotModel()));
  }

  @Test
  void testFindContextWindowOption_matchesByMaxContext() {
    CopilotModel model = modelWithTiers(128000, 1000000, 16000);

    ContextWindowOption match = ModelUtils.findContextWindowOption(model, 1000000);
    assertNotNull(match);
    assertFalse(match.isDefault());
    assertEquals(1000000, match.maxContext());

    assertNull(ModelUtils.findContextWindowOption(model, 999));
    assertNull(ModelUtils.findContextWindowOption(model, null));
  }

  @Test
  void testResolveDefaultContextWindowOption_prefersDefaultTier() {
    CopilotModel model = modelWithTiers(128000, 1000000, 16000);

    ContextWindowOption option = ModelUtils.resolveDefaultContextWindowOption(model);

    assertNotNull(option);
    assertTrue(option.isDefault());
    assertEquals(128000, option.maxContext());
    assertNull(ModelUtils.resolveDefaultContextWindowOption(new CopilotModel()));
  }
}

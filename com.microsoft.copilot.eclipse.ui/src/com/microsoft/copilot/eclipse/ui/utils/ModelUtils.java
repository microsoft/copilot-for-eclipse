// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesLimits;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesSupports;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelTokenPriceTier;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModelCapabilities;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Utility class for model related operations.
 */
public class ModelUtils {

  private ModelUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Convert ByokModel to CopilotModel format for unified handling.
   */
  public static CopilotModel convertByokModelToCopilotModel(ByokModel byokModel) {
    CopilotModel copilotModel = new CopilotModel();

    copilotModel.setId(byokModel.getModelId());
    String modelName = byokModel.getModelCapabilities() != null ? byokModel.getModelCapabilities().getName()
        : byokModel.getModelId();
    copilotModel.setModelName(modelName);
    copilotModel.setModelFamily(byokModel.getModelId());
    copilotModel.setProviderName(byokModel.getProviderName());

    List<String> scopes = new ArrayList<>();
    scopes.add(CopilotScope.CHAT_PANEL);
    if (byokModel.getModelCapabilities() != null && byokModel.getModelCapabilities().isToolCalling()) {
      scopes.add(CopilotScope.AGENT_PANEL);
    }
    copilotModel.setScopes(scopes);

    ByokModelCapabilities byokCapabilities = byokModel.getModelCapabilities();
    if (byokCapabilities != null) {
      CopilotModelCapabilitiesSupports supports = new CopilotModelCapabilitiesSupports(
          byokCapabilities.isVision(), null, false);
      // BYOK only exposes input/output token limits; context window and non-streaming output are unknown.
      CopilotModelCapabilitiesLimits limits = new CopilotModelCapabilitiesLimits(null,
          byokCapabilities.getMaxOutputTokens(), byokCapabilities.getMaxInputTokens(), null);
      copilotModel.setCapabilities(new CopilotModelCapabilities(supports, limits));
    }
    copilotModel.setBilling(null);
    copilotModel.setPreview(false);
    copilotModel.setChatDefault(false);
    copilotModel.setChatFallback(false);

    return copilotModel;
  }

  /**
   * Formats the billing multiplier for display. Returns the multiplier value with trailing zeros removed and an "x"
   * suffix (e.g., "0x", "1x", "1.5x").
   *
   * @param multiplier the billing multiplier value
   * @return the formatted multiplier text
   */
  public static String formatBillingMultiplier(double multiplier) {
    BigDecimal multiplierValue = BigDecimal.valueOf(multiplier).stripTrailingZeros();
    return multiplierValue.toPlainString() + Messages.model_billing_multiplier_suffix;
  }

  /**
   * Separator used between parts of the model picker suffix (e.g. "1M - High - $$$").
   */
  private static final String SUFFIX_PART_SEPARATOR = " - ";

  /**
   * Returns the display suffix for a model in the model picker.
   *
   * <p>The suffix is composed of multiple parts joined by {@value #SUFFIX_PART_SEPARATOR} (e.g. context window,
   * reasoning effort, price tier). For BYOK models the provider name is used as-is, and the {@code Auto} model uses
   * a fixed {@code Variable} label.
   *
   * @param model the model
   * @param reasoningEffort the effective reasoning effort to display, or {@code null} to omit
   * @return the suffix string, or an empty string if no suffix applies
   */
  public static String getModelSuffix(CopilotModel model, String reasoningEffort) {
    if (model == null) {
      return "";
    }
    if (model.getProviderName() != null) {
      return model.getProviderName();
    }
    if (isAutoModel(model)) {
      return Messages.model_billing_multiplier_variable;
    }
    // TODO: Remove this legacy fallback after TBB is officially released.
    // When token-based billing is not enabled on the language server side, fall back to the
    // original multiplier suffix (e.g. "1x", "1.5x") instead of the context-window | price tier
    // form that depends on the TBB-only capability metadata.
    if (model.getBilling() != null && !model.getBilling().tokenBasedBillingEnabled()) {
      return formatBillingMultiplier(model.getBilling().multiplier());
    }
    return String.join(SUFFIX_PART_SEPARATOR, buildSuffixParts(model, reasoningEffort));
  }

  /**
   * Builds the ordered list of suffix parts for a model. Add new parts (e.g. thinking effort) here in the desired
   * display order. Blank values are filtered out by the caller.
   */
  private static List<String> buildSuffixParts(CopilotModel model, String reasoningEffort) {
    List<String> parts = new ArrayList<>();
    addIfNotBlank(parts, getContextWindowText(model));
    // Only surface a reasoning-effort suffix when the language server has explicitly advertised the model as
    // supporting selectable effort levels. The server only sets supportsReasoningEffortLevel when the model has
    // more than one effort level AND is hosted on a compatible endpoint.
    if (supportsReasoningEffortLevel(model)) {
      addIfNotBlank(parts, formatReasoningEffortLevel(reasoningEffort));
    }
    addIfNotBlank(parts, formatPriceCategory(model.getModelPickerPriceCategory()));
    return parts;
  }

  private static void addIfNotBlank(List<String> parts, String value) {
    if (StringUtils.isNotBlank(value)) {
      parts.add(value);
    }
  }

  /**
   * Formats the model picker price category as a dollar-sign tier: {@code Low} -> {@code $}, {@code Medium} ->
   * {@code $$}, {@code High} -> {@code $$$}. Returns {@code null} for blank or unrecognized values.
   *
   * @param priceCategory the model picker price category (e.g. {@code Low}, {@code Medium}, {@code High})
   * @return the dollar-sign tier string, or {@code null} if the category is blank or unrecognized
   */
  public static String formatPriceCategory(String priceCategory) {
    if (StringUtils.isBlank(priceCategory)) {
      return null;
    }
    switch (priceCategory.toLowerCase()) {
      case "low":
        return "$";
      case "medium":
        return "$$";
      case "high":
        return "$$$";
      default:
        return null;
    }
  }

  /**
   * Returns the formatted context window size for the model, or {@code null} if unavailable.
   */
  public static String getContextWindowText(CopilotModel model) {
    Integer contextWindow = resolveContextWindowSize(model);
    if (contextWindow == null || contextWindow <= 0) {
      return null;
    }
    return formatTokenCount(contextWindow);
  }

  /**
   * Resolves the user-facing context window size for the model, mirroring the language-server / IntelliJ behavior.
   *
   * <p>When the model advertises a {@code default} price tier with its own {@code maxContext} (the input budget), the
   * full window is {@code maxContext + maxOutputTokens}. Otherwise, token-based billing models fall back to
   * {@code maxInputTokens + maxOutputTokens}, and finally to the advertised {@code maxContextWindowTokens}.
   *
   * @param model the model
   * @return the context window size in tokens, or {@code null} when it cannot be determined
   */
  public static Integer resolveContextWindowSize(CopilotModel model) {
    if (model.getCapabilities() == null || model.getCapabilities().limits() == null) {
      return null;
    }
    CopilotModelCapabilitiesLimits limits = model.getCapabilities().limits();
    Integer maxOutputTokens = limits.maxOutputTokens();
    int output = maxOutputTokens == null ? 0 : maxOutputTokens;

    CopilotModelTokenPriceTier defaultTier = getDefaultTokenPriceTier(model);
    if (defaultTier != null && defaultTier.maxContext() != null) {
      return defaultTier.maxContext() + output;
    }

    // TODO: Remove this legacy fallback after TBB is officially released.
    if (isTokenBasedBillingEnabled(model)) {
      Integer maxInputTokens = limits.maxInputTokens();
      if (maxInputTokens != null && maxOutputTokens != null) {
        return maxInputTokens + maxOutputTokens;
      }
    }

    return limits.maxContextWindowTokens();
  }

  // TODO: Remove this legacy fallback after TBB is officially released.
  private static boolean isTokenBasedBillingEnabled(CopilotModel model) {
    return model.getBilling() != null && model.getBilling().tokenBasedBillingEnabled();
  }

  /**
   * Returns the model's {@code default} token price tier, or {@code null} when the model carries no token-based
   * pricing.
   */
  private static CopilotModelTokenPriceTier getDefaultTokenPriceTier(CopilotModel model) {
    if (model.getBilling() == null || model.getBilling().tokenPrices() == null) {
      return null;
    }
    return model.getBilling().tokenPrices().defaultTier();
  }

  /**
   * Formats a token count into a compact human-readable string (e.g. 128K, 1M, 1.5M).
   *
   * @param tokens the token count
   * @return the formatted string
   */
  public static String formatTokenCount(int tokens) {
    if (tokens >= 1_000_000 && tokens % 1_000_000 == 0) {
      return tokens / 1_000_000 + "M";
    } else if (tokens >= 1_000_000) {
      String formatted = String.format("%.1f", tokens / 1_000_000.0);
      formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
      return formatted + "M";
    } else if (tokens >= 1_000 && tokens % 1_000 == 0) {
      return tokens / 1_000 + "K";
    } else if (tokens >= 1_000) {
      String formatted = String.format("%.1f", tokens / 1_000.0);
      formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
      return formatted + "K";
    }
    return String.valueOf(tokens);
  }

  /**
    * Returns the default reasoning effort to use when the user has not made a selection. Prefers {@code medium} when
    * it is supported, falling back to the first entry in the supported list. Returns {@code null} when the model
    * does not surface selectable reasoning effort levels (see {@link #supportsReasoningEffortLevel(CopilotModel)}).
   *
   * @param model the model
   * @return the default effort identifier, or {@code null} when none can be determined
   */
  public static String resolveDefaultReasoningEffort(CopilotModel model) {
    if (!supportsReasoningEffortLevel(model)) {
      return null;
    }
    List<String> efforts = getSupportedReasoningEfforts(model);
    if (efforts.isEmpty()) {
      return null;
    }
    for (String effort : efforts) {
      if ("medium".equalsIgnoreCase(effort)) {
        return effort;
      }
    }
    return efforts.get(0);
  }

  /**
   * Formats a reasoning effort identifier as a localized display label. Known identifiers ({@code none}, {@code low},
   * {@code medium}, {@code high}, {@code xhigh}) resolve to their localized {@code Messages} constants; unknown
   * identifiers fall back to a title-cased rendering of the first character (e.g. {@code custom} -> {@code Custom}).
   * Returns {@code null} when {@code effort} is blank.
   *
   * @param effort the effort identifier
   * @return the display label, or {@code null} when blank
   */
  public static String formatReasoningEffortLevel(String effort) {
    if (StringUtils.isBlank(effort)) {
      return null;
    }
    String trimmed = effort.trim();
    switch (trimmed.toLowerCase(Locale.ROOT)) {
      case "none":
        return Messages.model_reasoningEffort_none;
      case "low":
        return Messages.model_reasoningEffort_low;
      case "medium":
        return Messages.model_reasoningEffort_medium;
      case "high":
        return Messages.model_reasoningEffort_high;
      case "xhigh":
        return Messages.model_reasoningEffort_xhigh;
      default:
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }
  }

  /**
   * Returns the localized secondary description for a reasoning effort identifier (e.g. {@code low} -> "Faster
   * responses, less thorough reasoning"). Returns {@code null} when {@code effort} is blank or unrecognized.
   *
   * @param effort the effort identifier
   * @return the localized description, or {@code null} when blank or unrecognized
   */
  public static String formatReasoningEffortDescription(String effort) {
    if (StringUtils.isBlank(effort)) {
      return null;
    }
    switch (effort.trim().toLowerCase(Locale.ROOT)) {
      case "none":
        return Messages.model_reasoningEffort_none_description;
      case "low":
        return Messages.model_reasoningEffort_low_description;
      case "medium":
        return Messages.model_reasoningEffort_medium_description;
      case "high":
        return Messages.model_reasoningEffort_high_description;
      case "xhigh":
        return Messages.model_reasoningEffort_xhigh_description;
      default:
        return null;
    }
  }

  /**
   * Returns the list of reasoning effort levels advertised by the model, or an empty list when none are advertised.
   *
   * @param model the model
   * @return the list of supported reasoning effort identifiers (e.g. {@code low}, {@code medium}, {@code high})
   */
  public static List<String> getSupportedReasoningEfforts(CopilotModel model) {
    if (model == null || model.getCapabilities() == null || model.getCapabilities().supports() == null) {
      return List.of();
    }
    List<String> efforts = model.getCapabilities().supports().reasoningEfforts();
    return efforts == null ? List.of() : efforts;
  }

  /**
   * Returns whether the model is the special "Auto" model, which dynamically routes requests to other models and
   * therefore does not expose its own reasoning-effort selection.
   *
   * @param model the model
   * @return {@code true} when the model is the Auto model
   */
  public static boolean isAutoModel(CopilotModel model) {
    return model != null && "Auto".equals(model.getModelName());
  }

  /**
   * Returns whether the model surfaces selectable reasoning effort levels to the user. The language server only
   * advertises {@code supportsReasoningEffortLevel = true} when the model has more than one effort level and is
   * hosted on a compatible endpoint, so this is the canonical gate for the reasoning-effort UI and for sending a
   * {@code modelInfo.reasoningEffort} payload with chat requests. The Auto model is excluded because it routes to
   * other models and does not own its own effort selection.
   *
   * @param model the model
   * @return {@code true} when the user can select a reasoning effort for this model
   */
  public static boolean supportsReasoningEffortLevel(CopilotModel model) {
    if (model == null || model.getCapabilities() == null || model.getCapabilities().supports() == null) {
      return false;
    }
    if (isAutoModel(model)) {
      return false;
    }
    return model.getCapabilities().supports().supportsReasoningEffortLevel();
  }
}

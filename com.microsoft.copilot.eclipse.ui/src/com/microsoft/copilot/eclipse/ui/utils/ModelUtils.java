// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
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
      CopilotModel.CopilotModelCapabilitiesSupports supports = new CopilotModel.CopilotModelCapabilitiesSupports(
          byokCapabilities.isVision());
      // BYOK only exposes input/output token limits; context window and non-streaming output are unknown.
      CopilotModel.CopilotModelCapabilitiesLimits limits = new CopilotModel.CopilotModelCapabilitiesLimits(null,
          byokCapabilities.getMaxOutputTokens(), byokCapabilities.getMaxInputTokens(), null);
      copilotModel.setCapabilities(new CopilotModel.CopilotModelCapabilities(supports, limits));
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
   * Separator used between parts of the model picker suffix (e.g. "1M | High | $$$").
   */
  private static final String SUFFIX_PART_SEPARATOR = " | ";

  /**
   * Returns the display suffix for a model in the model picker.
   *
   * <p>The suffix is composed of multiple parts joined by {@value #SUFFIX_PART_SEPARATOR}. New parts (e.g. context
   * window size, thinking effort) should be appended to the list returned by {@link #buildSuffixParts(CopilotModel)}
   * in the desired display order; blank values are skipped automatically.
   *
   * <p>For BYOK models the provider name is used as-is, and the {@code Auto} model uses a fixed {@code Variable}
   * label.
   *
   * @param model the model
   * @return the suffix string, or an empty string if no suffix applies
   */
  public static String getModelSuffix(CopilotModel model) {
    if (model == null) {
      return "";
    }
    if (model.getProviderName() != null) {
      return model.getProviderName();
    }
    if ("Auto".equals(model.getModelName())) {
      return Messages.model_billing_multiplier_variable;
    }
    return String.join(SUFFIX_PART_SEPARATOR, buildSuffixParts(model));
  }

  /**
   * Builds the ordered list of suffix parts for a model. Add new parts (e.g. thinking effort) here in the desired
   * display order. Blank values are filtered out by the caller.
   */
  private static List<String> buildSuffixParts(CopilotModel model) {
    List<String> parts = new ArrayList<>();
    addIfNotBlank(parts, getContextWindowText(model));
    // TODO: thinking effort (e.g. "High") goes here.
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
  private static String getContextWindowText(CopilotModel model) {
    if (model.getCapabilities() == null || model.getCapabilities().limits() == null) {
      return null;
    }
    Integer maxContextWindowTokens = model.getCapabilities().limits().maxContextWindowTokens();
    if (maxContextWindowTokens == null || maxContextWindowTokens <= 0) {
      return null;
    }
    return formatTokenCount(maxContextWindowTokens);
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
}

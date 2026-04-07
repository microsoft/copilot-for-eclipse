package com.microsoft.copilot.eclipse.ui.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
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

    if (byokModel.getModelCapabilities() != null) {
      CopilotModel.CopilotModelCapabilitiesSupports supports = new CopilotModel.CopilotModelCapabilitiesSupports(
          byokModel.getModelCapabilities().isVision());
      int maxInputTokens = byokModel.getModelCapabilities().getMaxInputTokens() != null
          ? byokModel.getModelCapabilities().getMaxInputTokens() : -1;
      int maxOutputTokens = byokModel.getModelCapabilities().getMaxOutputTokens() != null
          ? byokModel.getModelCapabilities().getMaxOutputTokens() : -1;
      CopilotModel.CopilotModelCapabilitiesLimits limits = new CopilotModel.CopilotModelCapabilitiesLimits(
          maxInputTokens, maxOutputTokens, -1);
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
   * Returns the display suffix for a model in the model picker.
   *
   * @param model the model
   * @return the suffix string, or an empty string if no suffix applies
   */
  public static String getModelSuffix(CopilotModel model) {
    if (model.getProviderName() != null) {
      return model.getProviderName();
    }
    if (model.getBilling() != null) {
      return formatBillingMultiplier(model.getBilling().multiplier());
    }
    if ("Auto".equals(model.getModelName())) {
      return Messages.model_billing_multiplier_variable;
    }
    return "";
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
   * Formats the numeric part of a per-million-token price.
   *
   * @param price the price in dollars
   * @return the formatted price value string (e.g. "$1.25", "$10.00", "$0.0013")
   */
  public static String formatPriceValue(double price) {
    if (price == 0.0) {
      return "$0";
    } else if (price < 0.01) {
      return String.format("$%.4f", price).replaceAll("0+$", "");
    } else {
      return String.format("$%.2f", price);
    }
  }
}

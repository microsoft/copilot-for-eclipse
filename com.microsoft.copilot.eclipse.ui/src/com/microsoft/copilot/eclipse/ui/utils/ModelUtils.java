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
      copilotModel.setCapabilities(new CopilotModel.CopilotModelCapabilities(supports));
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
   * Composes tooltip text for a model item in the model picker.
   *
   * @param model the model to compose tooltip for
   * @param suffix the suffix shown next to the model name in the picker
   * @return the tooltip text
   */
  public static String getModelTooltipText(CopilotModel model, String suffix) {
    if (model == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(model.getModelName());
    if (StringUtils.isNotBlank(suffix)) {
      sb.append("\n").append(String.format(Messages.model_tooltip_quota, suffix));
    }
    return sb.toString();
  }
}

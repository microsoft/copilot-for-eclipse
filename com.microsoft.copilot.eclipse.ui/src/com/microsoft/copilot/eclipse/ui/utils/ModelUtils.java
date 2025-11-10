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
      copilotModel.setCapabilities(new CopilotModel.CopilotModelCapabilities(supports));
    }
    copilotModel.setBilling(null);
    copilotModel.setPreview(false);
    copilotModel.setChatDefault(false);
    copilotModel.setChatFallback(false);

    return copilotModel;
  }

  /**
   * Formats the billing multiplier for display. If the multiplier is 0, returns "Included". Otherwise, returns the
   * multiplier value with trailing zeros removed and an "x" suffix.
   *
   * @param multiplier the billing multiplier value
   * @return the formatted multiplier text
   */
  public static String formatBillingMultiplier(double multiplier) {
    BigDecimal multiplierValue = BigDecimal.valueOf(multiplier).stripTrailingZeros();
    if (multiplierValue.toPlainString().equals("0")) {
      return Messages.model_billing_included;
    } else {
      return multiplierValue.toPlainString() + Messages.model_billing_multiplier_suffix;
    }
  }

}

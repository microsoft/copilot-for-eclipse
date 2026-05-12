// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.handlers;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.GC;

import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.Quota;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.MenuUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Helper class for quota text calculations.
 */
public class QuotaTextCalculator {
  private final GC gc;
  private final CheckQuotaResult quotaResult;
  private final int maxWidth;
  private final int spaceWidth;
  private static final int PADDING_WIDTH = 20;

  /**
   * Constructor for QuotaTextCalculator.
   *
   * @param gc the graphics context used for text measurements.
   * @param quotaResult the result containing quota information.
   */
  public QuotaTextCalculator(GC gc, CheckQuotaResult quotaResult) {
    this.gc = gc;
    this.quotaResult = quotaResult;
    this.spaceWidth = gc.textExtent(UiUtils.HAIR_SPACE).x;
    this.maxWidth = calculateMaxWidth();
  }

  private int calculateMaxWidth() {
    int max = 0;
    if (!PlatformUtils.isWindows()) {
      max = Math.max(max,
          gc.stringExtent(Messages.menu_quota_codeCompletions + getPercentUsed(quotaResult.completions())).x);
      max = Math.max(max,
          gc.stringExtent(Messages.menu_quota_chatMessages + getPercentUsed(quotaResult.chat())).x);
      if (quotaResult.copilotPlan() != CopilotPlan.free && quotaResult.premiumInteractions() != null) {
        if (quotaResult.tokenBasedBillingEnabled()) {
          max = Math.max(max, gc.stringExtent(
              getPremiumRequestsLabel() + getPremiumRequestsSuffix()).x);
        } else {
          // TODO: Remove this legacy fallback after TBB is officially released.
          max = Math.max(max, gc.stringExtent(
              Messages.menu_quota_premiumRequests + getPercentUsed(quotaResult.premiumInteractions())).x);
        }
      }
      max += PADDING_WIDTH;
    }
    return max;
  }

  /**
   * Returns the label used for the monthly limit row. CFI (Copilot for Individuals) plans show
   * "Included credits"; all other paid plans show "Monthly limit".
   */
  private String getPremiumRequestsLabel() {
    if (MenuUtils.isCfiPlan(quotaResult.copilotPlan())) {
      return Messages.menu_quota_includedCredits;
    }
    return Messages.menu_quota_monthlyLimit;
  }

  /**
   * Returns the tooltip used for the premium requests row. CFI (Copilot for Individuals) plans get
   * the "included credits" tooltip; all other paid plans get the "monthly limit" tooltip.
   */
  public String getPremiumRequestsTooltip() {
    if (MenuUtils.isCfiPlan(quotaResult.copilotPlan())) {
      return Messages.menu_quota_includedCreditsTooltip;
    }
    return Messages.menu_quota_monthlyLimitTooltip;
  }

  /**
   * Returns the aligned text for code completions quota.
   */
  public String getCompletionText() {
    return getAlignedQuotaText(Messages.menu_quota_codeCompletions, getPercentUsed(quotaResult.completions()));
  }

  /**
   * Returns the aligned text for chat messages quota.
   */
  public String getChatText() {
    return getAlignedQuotaText(Messages.menu_quota_chatMessages, getPercentUsed(quotaResult.chat()));
  }

  /**
   * Returns the aligned text for the monthly limit row, sourced from the premium interactions quota.
   * CFI (Copilot for Individuals) plans label this row "Included credits" and display the absolute
   * "{used}/{entitlement} AI credits used" suffix instead of a percentage.
   */
  public String getPremiumRequestsText() {
    return getAlignedQuotaText(getPremiumRequestsLabel(), getPremiumRequestsSuffix());
  }

  // TODO: Remove this legacy fallback after TBB is officially released.
  /**
   * Returns the aligned text for the legacy "Premium Requests" row used when token-based billing is
   * not enabled on the language server. Preserves the original main-branch label and "{percent}%"
   * suffix.
   */
  public String getPremiumText() {
    return getAlignedQuotaText(Messages.menu_quota_premiumRequests,
        getPercentUsed(quotaResult.premiumInteractions()));
  }

  /**
   * Returns the suffix used for the premium requests row. CFI (Copilot for Individuals) plans get
   * the absolute "{used}/{entitlement} AI credits used" form; other paid plans get the standard
   * "{percent}% used" form.
   */
  private String getPremiumRequestsSuffix() {
    Quota premiumQuota = quotaResult.premiumInteractions();
    if (MenuUtils.isCfiPlan(quotaResult.copilotPlan()) && premiumQuota != null && !premiumQuota.unlimited()) {
      long entitlement = Math.round(premiumQuota.entitlement());
      long used = Math.max(0, entitlement - Math.round(premiumQuota.quotaRemaining()));
      return NLS.bind(Messages.menu_quota_aiCreditsUsedFormat, used, entitlement);
    }
    return getPercentUsed(premiumQuota);
  }

  /**
   * Helper method to generate aligned quota text with a caller-supplied suffix.
   *
   * @param messagePrefix the message prefix (e.g., "Included credits")
   * @param quotaText the suffix to display on the right (e.g., "12/300 AI credits used")
   * @return the aligned quota text
   */
  private String getAlignedQuotaText(String messagePrefix, String quotaText) {
    if (PlatformUtils.isWindows()) {
      // windows supports align the text via \t
      return messagePrefix.trim() + "\t" + quotaText;
    }
    int currentWidth = gc.stringExtent(messagePrefix + quotaText).x;
    int spacesToAdd = (int) Math.round((maxWidth - currentWidth) / (double) spaceWidth) + 1;
    return UiUtils.getAlignedText(gc, messagePrefix, UiUtils.HAIR_SPACE, quotaText, spacesToAdd, maxWidth);
  }

  private String getPercentUsed(Quota quota) {
    if (quota == null) {
      return "";
    }
    if (quota.unlimited()) {
      return Messages.menu_quota_included;
    }
    double percent = Math.max(0, 100 - quota.percentRemaining());
    String formattedPercent = percent < 0.1 ? "0"
        : String.format("%.1f", Math.round(percent * 10) / 10.0);
    if (!quotaResult.tokenBasedBillingEnabled()) {
      // TODO: Remove this legacy fallback after TBB is officially released.
      return formattedPercent + "%";
    }
    return NLS.bind(Messages.menu_quota_percentUsedFormat, formattedPercent);
  }
}

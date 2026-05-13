// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import com.google.gson.annotations.SerializedName;

/**
 * Parameters for the {@code copilot/quotaWarning} notification. Sent by the language server when the user's AI quota
 * exceeds a warning threshold.
 *
 * @param title the popup title supplied by the language server
 * @param message the popup body message
 * @param severity the language-server severity hint (e.g. {@code "info"} or {@code "warning"}); used by the client to
 *     decide which icon to render on the banner. May be {@code null}.
 * @param copilotPlan the user's Copilot plan
 * @param premiumInteractions the premium-interactions snapshot for the warning, or {@code null} when the language
 *     server does not include it
 */
public record QuotaWarningNotification(
    String title,
    String message,
    String severity,
    CopilotPlan copilotPlan,
    @SerializedName("premium_interactions") PremiumInteractions premiumInteractions) {

  /**
   * Premium-interactions snapshot embedded in a {@link QuotaWarningNotification}. The shape is dictated by the
   * language server and differs from {@link Quota}.
   *
   * @param quota total monthly premium-interactions allowance
   * @param used premium interactions consumed so far this period
   * @param percentRemaining percentage of the allowance remaining
   * @param overageUsed additional paid interactions consumed beyond the allowance
   * @param overageEnabled whether the user has enabled paid overage
   * @param resetDate ISO-8601 instant when the monthly allowance resets, or {@code null}
   * @param unlimited whether this quota has no monthly limit
   */
  public record PremiumInteractions(
      double quota,
      double used,
      double percentRemaining,
      double overageUsed,
      boolean overageEnabled,
      String resetDate,
      boolean unlimited) {
  }
}

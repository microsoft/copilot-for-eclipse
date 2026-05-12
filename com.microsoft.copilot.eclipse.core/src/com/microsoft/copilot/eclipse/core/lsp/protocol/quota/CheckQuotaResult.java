// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

/**
 * Result of the {@code checkQuota} request.
 *
 * @param chat chat quota snapshot
 * @param completions completions quota snapshot
 * @param premiumInteractions premium interactions quota snapshot
 * @param resetDate ISO-8601 local date when the monthly allowance resets, or {@code null}
 * @param resetDateUtc ISO-8601 instant when the monthly allowance resets in UTC, or {@code null}
 * @param copilotPlan the user's Copilot plan
 * @param tokenBasedBillingEnabled whether the user's billing is token-based
 */
public record CheckQuotaResult(
    Quota chat,
    Quota completions,
    Quota premiumInteractions,
    String resetDate,
    String resetDateUtc,
    CopilotPlan copilotPlan,
    boolean tokenBasedBillingEnabled) {

  private static final CheckQuotaResult EMPTY =
      new CheckQuotaResult(null, null, null, null, null, null, false);

  /**
   * Returns an empty {@link CheckQuotaResult} used as a placeholder before the language server
   * supplies real quota data.
   */
  public static CheckQuotaResult empty() {
    return EMPTY;
  }
}

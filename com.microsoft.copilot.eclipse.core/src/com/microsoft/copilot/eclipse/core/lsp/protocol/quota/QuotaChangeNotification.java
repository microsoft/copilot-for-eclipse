package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import com.google.gson.annotations.SerializedName;

/**
 * Parameters for the "copilot/quotaChange" notification. Sent by the language server when the user's quota status
 * changes.
 */
public record QuotaChangeNotification(
    @SerializedName("premium_interactions") QuotaSnapshotParams premiumInteractions,
    @SerializedName("extended_usage_interval") QuotaSnapshotParams extendedUsageInterval,
    @SerializedName("immediate_usage_interval") QuotaSnapshotParams immediateUsageInterval,
    CopilotPlan copilotPlan) {

  /**
   * Creates a notification from a {@link CheckQuotaResult} returned by the checkQuota request.
   *
   * @param result the check quota result
   * @return a notification, or null if the input is null
   */
  public static QuotaChangeNotification fromCheckQuotaResult(CheckQuotaResult result) {
    if (result == null) {
      return null;
    }
    return new QuotaChangeNotification(
        QuotaSnapshotParams.fromQuota(result.getPremiumInteractionsQuota()),
        QuotaSnapshotParams.fromTbbQuota(result.getExtendedUsageInterval()),
        QuotaSnapshotParams.fromTbbQuota(result.getImmediateUsageInterval()),
        result.getCopilotPlan());
  }
}

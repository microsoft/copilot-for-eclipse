package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

/**
 * Snapshot of a single quota category as sent by the language server in the "copilot/quotaChange" notification.
 */
public record QuotaSnapshotParams(
    int quota,
    double percentRemaining,
    int overageUsed,
    boolean overageEnabled,
    boolean unlimited,
    String resetDate) {

  /**
   * Creates a snapshot from a {@link Quota} object returned by the checkQuota request.
   *
   * @param q the quota, may be null
   * @return a snapshot, or null if the input is null
   */
  public static QuotaSnapshotParams fromQuota(Quota q) {
    if (q == null) {
      return null;
    }
    int entitlement = q.isUnlimited() ? 0 : q.getEntitlement();
    double pctRemaining = q.getPercentRemaining();
    return new QuotaSnapshotParams(entitlement, pctRemaining, q.getOverageCount(), q.isOveragePermitted(),
        q.isUnlimited(), "");
  }

  /**
   * Creates a snapshot from a {@link TbbQuota} object returned by the checkQuota request.
   *
   * @param q the TBB quota, may be null
   * @return a snapshot, or null if the input is null
   */
  public static QuotaSnapshotParams fromTbbQuota(TbbQuota q) {
    if (q == null) {
      return null;
    }
    int entitlement = q.isUnlimited() ? 0 : q.getEntitlement();
    double pctRemaining = q.getPercentRemaining();
    String resetDate = q.getResetAt() != null ? q.getResetAt() : "";
    return new QuotaSnapshotParams(entitlement, pctRemaining, q.getOverageCount(), q.isOveragePermitted(),
        q.isUnlimited(), resetDate);
  }
}

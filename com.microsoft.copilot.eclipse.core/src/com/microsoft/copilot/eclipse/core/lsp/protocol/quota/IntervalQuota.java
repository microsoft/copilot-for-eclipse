package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

/**
 * Interval-based quota information, used for immediateUsageInterval and extendedUsageInterval.
 */
public record IntervalQuota(
    double percentRemaining,
    boolean unlimited,
    boolean overagePermitted,
    Integer entitlement,
    Integer quotaRemaining,
    String timeStamp,
    String resetAt) {
}

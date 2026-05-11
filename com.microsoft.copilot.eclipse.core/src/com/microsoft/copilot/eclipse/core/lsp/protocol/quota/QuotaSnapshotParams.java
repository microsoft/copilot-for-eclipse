// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

/**
 * Snapshot of a single quota bucket (chat, completions, or premium interactions) shipped with
 * {@code copilot/quotaChange} and {@code copilot/quotaWarning} notifications.
 *
 * @param quota total entitlement
 * @param used computed amount used (entitlement * (1 - percentRemaining / 100))
 * @param percentRemaining percentage of the quota remaining (0-100)
 * @param overageUsed overage amount consumed
 * @param overageEnabled whether overages are permitted
 * @param resetDate ISO 8601 timestamp when the quota resets, or empty when unknown
 * @param unlimited true when the quota is unlimited
 */
public record QuotaSnapshotParams(double quota, double used, double percentRemaining, double overageUsed,
    boolean overageEnabled, String resetDate, boolean unlimited) {
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import com.google.gson.annotations.SerializedName;

/**
 * Parameters for the {@code copilot/quotaChange} notification, sent by the language server
 * whenever the user's quota usage changes.
 *
 * @param chat current chat quota snapshot, when available
 * @param completions current completions quota snapshot, when available
 * @param premiumInteractions current premium interactions quota snapshot, when available
 * @param copilotPlan the user's Copilot plan (e.g. free, individual, individual_pro, individual_max,
 *     business, enterprise)
 */
public record QuotaChangeParams(QuotaSnapshotParams chat, QuotaSnapshotParams completions,
    @SerializedName("premium_interactions") QuotaSnapshotParams premiumInteractions, String copilotPlan) {
}

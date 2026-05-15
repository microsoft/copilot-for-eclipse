// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import com.google.gson.annotations.SerializedName;

/**
 * Parameters for the {@code copilot/quotaWarning} notification, sent by the language server when
 * the user crosses a quota usage threshold or starts consuming overages.
 *
 * @param title warning title (e.g. "Copilot Quota Usage Alert")
 * @param message human-readable warning message
 * @param severity severity level, either {@code "warning"} or {@code "info"}
 * @param chat current chat quota snapshot, when available
 * @param completions current completions quota snapshot, when available
 * @param premiumInteractions current premium interactions quota snapshot, when available
 * @param copilotPlan the user's Copilot plan
 */
public record QuotaWarningParams(String title, String message, String severity, QuotaSnapshotParams chat,
    QuotaSnapshotParams completions,
    @SerializedName("premium_interactions") QuotaSnapshotParams premiumInteractions, CopilotPlan copilotPlan) {
}

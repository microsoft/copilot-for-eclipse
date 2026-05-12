// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

/**
 * Parameters for the "copilot/quotaWarning" notification. Sent by the language server when the user's AI quota exceeds
 * the warning threshold.
 */
public record QuotaWarningNotification(String message, double percentUsed) {
}

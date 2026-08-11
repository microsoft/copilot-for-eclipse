// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

/**
 * Provider-level BYOK configuration.
 *
 * @param providerName provider display name
 * @param url          provider endpoint URL
 */
public record ByokProviderConfig(String providerName, String url) {
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

/**
 * Parameters for deleting a built-in BYOK provider configuration.
 *
 * @param providerName provider name
 */
public record ByokDeleteProviderConfigParams(String providerName) {
}

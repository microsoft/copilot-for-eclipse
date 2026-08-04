// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Parameters for listing built-in BYOK provider configurations.
 *
 * @param providerName provider name, or {@code null} to list all configured providers
 */
public record ByokListProviderConfigParams(@Nullable String providerName) {
}

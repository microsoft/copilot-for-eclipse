// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import java.util.List;

/**
 * Response model for listing provider-level BYOK configurations.
 *
 * @param providers provider configurations
 */
public record ByokListProviderConfigResponse(List<ByokProviderConfig> providers) {
}

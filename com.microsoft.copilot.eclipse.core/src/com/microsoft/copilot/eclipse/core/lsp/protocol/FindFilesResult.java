// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;

/**
 * Result of the {@code workspace/findFiles} request, containing URIs of files matching the glob pattern.
 *
 * @param uris the list of file URIs matching the glob pattern
 */
public record FindFilesResult(List<String> uris) {
}

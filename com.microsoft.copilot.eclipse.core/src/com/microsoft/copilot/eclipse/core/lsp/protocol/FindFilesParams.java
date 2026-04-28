// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Parameters for the {@code workspace/findFiles} request. Used by the language server to ask the client to search for
 * files matching a glob pattern under a given base URI (e.g. a semanticfs workspace folder).
 *
 * @param baseUri the base URI to search under (e.g. a semanticfs workspace folder)
 * @param pattern the glob pattern to match file paths against
 * @param maxResults the maximum number of results to return (optional)
 */
public record FindFilesParams(String baseUri, String pattern, int maxResults) {
}

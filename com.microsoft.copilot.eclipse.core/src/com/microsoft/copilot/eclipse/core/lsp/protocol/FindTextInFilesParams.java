// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Parameters for the {@code workspace/findTextInFiles} request. Used by the language server to ask the client to search
 * for text (or a regex) in files under a given base URI.
 *
 * @param baseUri the base URI to search under (e.g. a semanticfs workspace folder)
 * @param query the text or regex pattern to search for in files
 * @param isRegexp whether the query is a regular expression
 * @param includePattern an optional glob pattern to filter which files to search
 * @param maxResults the maximum number of results to return (optional)
 */
public record FindTextInFilesParams(String baseUri, String query, Boolean isRegexp, String includePattern,
    Integer maxResults) {
}

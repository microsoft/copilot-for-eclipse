package com.microsoft.copilot.eclipse.core.lsp.protocol.tools;

/**
 * Result record for getErrors method, containing the content and exception status.
 *
 * @param content The error messages and results.
 * @param hasException Whether any exceptions occurred during processing.
 */
public record GetErrorsResult(String content, boolean hasException) {
}

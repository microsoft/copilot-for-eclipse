// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Metadata for a Copilot customization file (skill, prompt, instruction, or agent) as returned by
 * the language server's customization list requests.
 *
 * @param id the stable identifier, when provided by the language server
 * @param name the user-facing name
 * @param uri the on-disk file URI, or {@code null} for entries without a backing file
 * @param storage the storage scope reported by the language server (for example {@code local} or {@code user})
 */
public record CustomizationFileInfo(String id, String name, String uri, String storage) {
}

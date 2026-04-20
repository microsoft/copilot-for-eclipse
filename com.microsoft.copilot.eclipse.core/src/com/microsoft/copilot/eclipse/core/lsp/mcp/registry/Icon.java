// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents the icon of the MCP.
 *
 * @param src      The source URL of the icon.
 * @param mimeType The MIME type of the icon.
 * @param sizes    The available sizes of the icon.
 * @param theme    The theme of the icon.
 */
public record Icon(String src, String mimeType, List<String> sizes, String theme) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("src", src);
    builder.append("mimeType", mimeType);
    builder.append("sizes", sizes);
    builder.append("theme", theme);
    return builder.toString();
  }
}

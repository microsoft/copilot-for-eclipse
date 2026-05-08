// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * MCP tool annotations describing tool behavior hints (e.g., readOnly, destructive).
 * Provided by CLS, originally sourced from the MCP server's tool definition.
 */
public class ToolAnnotations {
  private boolean readOnlyHint;
  private boolean destructiveHint;
  private boolean idempotentHint;
  private boolean openWorldHint;

  public boolean isReadOnlyHint() {
    return readOnlyHint;
  }

  public void setReadOnlyHint(boolean readOnlyHint) {
    this.readOnlyHint = readOnlyHint;
  }

  public boolean isDestructiveHint() {
    return destructiveHint;
  }

  public void setDestructiveHint(boolean destructiveHint) {
    this.destructiveHint = destructiveHint;
  }

  public boolean isIdempotentHint() {
    return idempotentHint;
  }

  public void setIdempotentHint(boolean idempotentHint) {
    this.idempotentHint = idempotentHint;
  }

  public boolean isOpenWorldHint() {
    return openWorldHint;
  }

  public void setOpenWorldHint(boolean openWorldHint) {
    this.openWorldHint = openWorldHint;
  }

  @Override
  public int hashCode() {
    return Objects.hash(readOnlyHint, destructiveHint, idempotentHint, openWorldHint);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ToolAnnotations other = (ToolAnnotations) obj;
    return readOnlyHint == other.readOnlyHint
        && destructiveHint == other.destructiveHint
        && idempotentHint == other.idempotentHint
        && openWorldHint == other.openWorldHint;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("readOnlyHint", readOnlyHint);
    builder.append("destructiveHint", destructiveHint);
    builder.append("idempotentHint", idempotentHint);
    builder.append("openWorldHint", openWorldHint);
    return builder.toString();
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Chat persistence.
 * https://github.com/microsoft/copilot-client/blob/main/agent/src/methods/conversation/conversationPersistence.ts#L31
 */
public class ChatPersistence {
  private String path;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
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
    ChatPersistence other = (ChatPersistence) obj;
    return Objects.equals(path, other.path);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("path", path);
    return builder.toString();
  }
}

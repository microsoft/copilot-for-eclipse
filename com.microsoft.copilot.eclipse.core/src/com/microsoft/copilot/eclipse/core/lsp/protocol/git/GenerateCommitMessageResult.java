// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.git;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result of the 'git/generateCommitMessage' request.
 */
public class GenerateCommitMessageResult {

  private String commitMessage;

  /**
   * Creates a new GenerateCommitMessageResult.
   */
  public GenerateCommitMessageResult(String commitMessage) {
    this.commitMessage = commitMessage;
  }

  public String getCommitMessage() {
    return commitMessage;
  }

  public void setCommitMessage(String commitMessage) {
    this.commitMessage = commitMessage;
  }

  @Override
  public int hashCode() {
    return Objects.hash(commitMessage);
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
    GenerateCommitMessageResult other = (GenerateCommitMessageResult) obj;
    return Objects.equals(commitMessage, other.commitMessage);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("commitMessage", commitMessage);
    return builder.toString();
  }

}

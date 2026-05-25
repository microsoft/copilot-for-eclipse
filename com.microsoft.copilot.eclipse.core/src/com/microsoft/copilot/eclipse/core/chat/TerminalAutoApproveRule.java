// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A single terminal auto-approve rule mapping a command name or regex pattern to an
 * allow/deny decision.
 */
public class TerminalAutoApproveRule {
  private String command;
  private boolean autoApprove;

  /**
   * Creates a new rule.
   *
   * @param command the command name or regex pattern
   * @param autoApprove true to auto-approve, false to always require confirmation
   */
  public TerminalAutoApproveRule(String command, boolean autoApprove) {
    this.command = command;
    this.autoApprove = autoApprove;
  }

  /** Default constructor for Gson deserialization. */
  public TerminalAutoApproveRule() {
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public boolean isAutoApprove() {
    return autoApprove;
  }

  public void setAutoApprove(boolean autoApprove) {
    this.autoApprove = autoApprove;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoApprove, command);
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
    TerminalAutoApproveRule other = (TerminalAutoApproveRule) obj;
    return autoApprove == other.autoApprove
        && Objects.equals(command, other.command);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("command", command)
        .append("autoApprove", autoApprove)
        .toString();
  }
}

// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Complete content for rendering a confirmation UI. Returned by handlers when a tool call
 * needs user confirmation. Contains the display text and the list of action buttons.
 */
public class ConfirmationContent {

  private final String title;
  private final String message;
  private final List<ConfirmationAction> actions;

  /**
   * Creates a new confirmation content.
   *
   * @param title bold title text displayed at the top
   * @param message description text (may be null)
   * @param actions list of button actions for the confirmation UI
   */
  public ConfirmationContent(String title, String message,
      List<ConfirmationAction> actions) {
    this.title = title;
    this.message = message;
    this.actions = actions;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public List<ConfirmationAction> getActions() {
    return actions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(actions, message, title);
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
    ConfirmationContent other = (ConfirmationContent) obj;
    return Objects.equals(actions, other.actions)
        && Objects.equals(message, other.message)
        && Objects.equals(title, other.title);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("title", title)
        .append("message", message)
        .append("actions", actions)
        .toString();
  }
}

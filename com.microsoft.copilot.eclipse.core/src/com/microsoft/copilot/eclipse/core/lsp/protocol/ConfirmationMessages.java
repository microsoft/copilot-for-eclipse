package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Confirmation message for the tool invocation.
 */
public class ConfirmationMessages {
  private String title;
  private String message;

  /**
   * Default constructor.
   */
  public ConfirmationMessages() {
  }

  /**
   * Construct a new ConfirmationMessages.
   *
   * @param title The title of the confirmation.
   * @param message The message of the confirmation.
   */
  public ConfirmationMessages(String title, String message) {
    this.title = title;
    this.message = message;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, title);
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
    ConfirmationMessages other = (ConfirmationMessages) obj;
    return Objects.equals(message, other.message) && Objects.equals(title, other.title);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("title", title);
    builder.append("message", message);
    return builder.toString();
  }

}

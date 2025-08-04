package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Interface for a chat reference, which can be a file or directory.
 */
public interface ChatReference {

  /**
   * Get the type of the content.
   *
   * @return the content as a String
   */
  String getType();

  /**
   * Enum representing the type of reference.
   */
  public enum ReferenceType {
    FILE("file"), Directory("directory");

    private final String value;

    ReferenceType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}

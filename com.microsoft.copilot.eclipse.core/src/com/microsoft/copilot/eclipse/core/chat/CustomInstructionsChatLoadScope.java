// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import com.microsoft.copilot.eclipse.core.Constants;

/**
 * Scope loading modes for custom instructions in GitHub Copilot chat.
 * <ul>
 * <li><b>ALL_PROJECTS</b>: Load custom instructions from all projects in the Eclipse workspace
 * <li><b>REFERENCED_PROJECTS</b>: Load custom instructions only from parent projects of files/folders referenced in the
 * Copilot chat
 * </ul>
 */
public enum CustomInstructionsChatLoadScope {
  ALL_PROJECTS(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE_ALL),
  REFERENCED_PROJECTS(Constants.CUSTOM_INSTRUCTIONS_CHAT_LOAD_SCOPE_REFERENCED);

  public static final CustomInstructionsChatLoadScope DEFAULT_VALUE = CustomInstructionsChatLoadScope.ALL_PROJECTS;

  private final String value;

  CustomInstructionsChatLoadScope(String value) {
    this.value = value;
  }

  /**
   * Returns the string value representing this enum entry for preference serialization.
   *
   * @return the string value for this preference setting
   */
  public String getValue() {
    return value;
  }

  /**
   * Retrieves the enum constant corresponding to the given string value if available, otherwise an
   * {@link IllegalArgumentException} is thrown.
   *
   * @param value the string value (preference) representing an enum entry
   * @return the enum entry representing the given value
   * @throws IllegalArgumentException if the value does not correspond to any enum entry
   */
  public static CustomInstructionsChatLoadScope fromValue(String value) {
    for (CustomInstructionsChatLoadScope scope : values()) {
      if (scope.getValue().equals(value)) {
        return scope;
      }
    }
    throw new IllegalArgumentException("Unknown CustomInstructionsLoadScope value: " + value);
  }

}

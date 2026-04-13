// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.logger;

/**
 * The event type enum.
 */
public enum LogLevel {
  INFO("INFO"), WARNING("WARNING"), ERROR("ERROR");

  private String value;

  LogLevel(String string) {
    this.value = string;
  }

  public String getValue() {
    return this.value;
  }
}

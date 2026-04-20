/*******************************************************************************
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 *******************************************************************************/
package com.microsoft.copilot.eclipse.swtbot.test.probe;

/**
 * A single step in a probe script. Gson-friendly POJO.
 *
 * <p>Fields are optional depending on {@link #action}. See {@code SKILL.md} for the
 * canonical action vocabulary.</p>
 */
public class ProbeStep {
  /** Action name (e.g. "click", "typeIn", "screenshot"). Required. */
  public String action;

  /** Optional step id; used as filename stem for screenshots and ui dumps. */
  public String id;

  /** Timeout in seconds for this step where applicable (default depends on action). */
  public Integer timeoutSec;

  /**
   * If {@code false}, step failure is recorded but does not fail the overall probe.
   * Default is {@code true}.
   */
  public Boolean failFast;

  /** Free-form text payload (for typeIn, etc.). */
  public String text;

  /** Key name for pressKey (e.g. "ENTER", "ESC", "TAB"). */
  public String key;

  /** Eclipse id string (view id, command id). */
  public String idRef;

  /** Element locator. */
  public Locator locator;

  /** Whether the element should exist (for assertExists). Default true. */
  public Boolean shouldExist;

  /** No-arg method name to invoke reflectively (for waitForMethod). */
  public String method;

  /**
   * Optional expected value for {@code waitForMethod}. If {@code null} the action
   * polls until the return value is non-null (and non-empty for strings); otherwise
   * it polls until the return value's {@code toString()} equals this string.
   */
  public String expectedValue;

  public boolean isFailFast() {
    return failFast == null ? true : failFast;
  }
}

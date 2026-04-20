/*******************************************************************************
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 *******************************************************************************/
package com.microsoft.copilot.eclipse.swtbot.test.probe;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates step results and serializes the final report.
 */
public class StepResult {
  public int index;
  public String action;
  public String id;
  public String status;
  public String message;
  public long durationMs;
  public List<String> screenshots = new ArrayList<>();
}

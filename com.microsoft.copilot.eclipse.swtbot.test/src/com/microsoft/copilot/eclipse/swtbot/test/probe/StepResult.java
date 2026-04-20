// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

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

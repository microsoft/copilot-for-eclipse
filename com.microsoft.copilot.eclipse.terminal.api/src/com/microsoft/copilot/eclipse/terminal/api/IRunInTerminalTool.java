// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.terminal.api;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.resource.ImageDescriptor;


/**
 * Interface for terminal tool implementations that can execute commands in a terminal. This interface is used by the
 * SPI to allow different terminal tools to be plugged in.
 */
public interface IRunInTerminalTool {

  /**
   * Executes a command in the terminal with an initial working directory.
   *
   * @param command The command to execute.
   * @param isBackground Whether the command should run in the background.
   * @param workingDirectory The terminal's initial working directory.
   * @return A CompletableFuture that resolves to the output of the command.
   */
  public CompletableFuture<String> executeCommand(String command, boolean isBackground, String workingDirectory);

  /**
   * Prepares terminal properties for the command execution with an initial working directory.
   *
   * @param runInBackground Whether the command should run in the background.
   * @param executionId The unique identifier for the execution.
   * @param workingDirectory The terminal's initial working directory.
   * @return A map containing terminal properties.
   */
  public Map<String, Object> prepareTerminalProperties(boolean runInBackground, String executionId,
      String workingDirectory);

  /**
   * Retrieves the output of a background command execution.
   *
   * @param executionId The unique identifier for the background execution.
   * @return A StringBuilder containing the output of the command.
   */
  public StringBuilder getBackgroundCommandOutput(String executionId);

  /**
   * Cancels the foreground terminal command if one is currently running.
   */
  public void cancelCurrentCommand();

  /**
   * Sets the terminal icon descriptor for the tool.
   */
  public void setTerminalIconDescriptor(ImageDescriptor terminalIconDescriptor);
}

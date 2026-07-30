// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.terminal.api;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.eclipse.swt.graphics.Image;


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
   * Sets the supplier that provides the terminal icon for the tool.
   *
   * <p>The supplier must return a shared {@link Image} owned by a plug-in's image registry.
   * The image must never be disposed (this is done by the registry).
   *
   * <p>The run-in-terminal implementation needs to invoke the supplier on the UI thread.
   *
   * @param terminalIconSupplier supplier of a shared, non-disposable terminal icon
   */
  public void setTerminalIconSupplier(Supplier<Image> terminalIconSupplier);
}

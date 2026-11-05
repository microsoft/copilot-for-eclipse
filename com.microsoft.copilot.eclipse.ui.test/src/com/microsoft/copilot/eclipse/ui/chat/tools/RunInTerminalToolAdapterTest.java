// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult.ToolInvocationStatus;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

class RunInTerminalToolAdapterTest {

  private RunInTerminalToolAdapter runInTerminalToolAdapter;
  private Shell shell;

  @BeforeEach
  void setUp() {
    // Setup SWT components
    SwtUtils.invokeOnDisplayThread(() -> {
      shell = new Shell(Display.getDefault());
    });

    runInTerminalToolAdapter = new RunInTerminalToolAdapter();
  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Test
  void testGetToolInformation() {
    // Act
    LanguageModelToolInformation toolInfo = runInTerminalToolAdapter.getToolInformation();

    // Assert
    assertNotNull(toolInfo);
    assertEquals("run_in_terminal", toolInfo.getName());
    assertNotNull(toolInfo.getDescription());
    assertNotNull(toolInfo.getInputSchema());
    assertTrue(toolInfo.getInputSchema().getRequired().contains("command"));
  }

  @Test
  void testInvokeWithEmptyCommandReturnsErrorStatus() throws InterruptedException, ExecutionException {
    // Arrange
    Map<String, Object> input = new HashMap<>();
    input.put("command", "");
    input.put("isBackground", false);

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = runInTerminalToolAdapter.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert
    assertNotNull(results);
    assertEquals(1, results.length);
    assertEquals(ToolInvocationStatus.error, results[0].getStatus());
    assertEquals("The tool cannot be invoked due to the command is null or empty.",
      results[0].getContent().get(0).getValue());
  }

  @Test
  void testInvokeWithCommandReturnsWellFormedResult() throws InterruptedException, ExecutionException {
    // Arrange
    Map<String, Object> input = new HashMap<>();
    input.put("command", "echo test");
    input.put("isBackground", false);

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = runInTerminalToolAdapter.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert - the outcome depends on the host's terminal, so only the result's shape is verified
    assertNotNull(results);
    assertEquals(1, results.length);
    assertNotNull(results[0].getStatus());
    assertNotNull(results[0].getContent());
  }

  @Test
  void testNeedConfirmation() {
    // Act & Assert
    assertEquals(true, runInTerminalToolAdapter.needConfirmation());
  }

  @Test
  void testToolName() {
    // Act & Assert
    assertEquals("run_in_terminal", runInTerminalToolAdapter.getToolName());
  }

  @Test
  void testGetTerminalOutputToolWithUnknownIdReturnsErrorStatus()
      throws InterruptedException, ExecutionException {
    // Arrange
    RunInTerminalToolAdapter.GetTerminalOutputTool getTool = new RunInTerminalToolAdapter.GetTerminalOutputTool();
    Map<String, Object> input = new HashMap<>();
    input.put("id", "terminal-123");

    // Act
    CompletableFuture<LanguageModelToolResult[]> future = getTool.invoke(input, null);
    LanguageModelToolResult[] results = future.get();

    // Assert
    assertNotNull(results);
    assertEquals(1, results.length);
    assertEquals(ToolInvocationStatus.error, results[0].getStatus());
    assertEquals("Invalid terminal ID terminal-123", results[0].getContent().get(0).getValue());
  }

  @Test
  void testGetTerminalOutputToolGetToolInformation() {
    // Arrange
    RunInTerminalToolAdapter.GetTerminalOutputTool getTool = new RunInTerminalToolAdapter.GetTerminalOutputTool();

    // Act
    LanguageModelToolInformation toolInfo = getTool.getToolInformation();

    // Assert
    assertNotNull(toolInfo);
    assertEquals("get_terminal_output", toolInfo.getName());
    assertNotNull(toolInfo.getDescription());
    assertNotNull(toolInfo.getInputSchema());
    assertTrue(toolInfo.getInputSchema().getRequired().contains("id"));
  }
}

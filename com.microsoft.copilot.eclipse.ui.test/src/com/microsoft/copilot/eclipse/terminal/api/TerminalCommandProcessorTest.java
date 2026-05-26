// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.terminal.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.terminal.api.TerminalCommandProcessor.CompletionCheckState;

class TerminalCommandProcessorTest {
  @Test
  void testFormatForExecution_singleLine_appendsCarriageReturn() {
    assertEquals("echo hello\r", TerminalCommandProcessor.formatForExecution("echo hello"));
  }

  @Test
  void testFormatForExecution_multiline_wrapsWithBracketedPaste() {
    assertEquals("\u001b[200~echo first\recho second\u001b[201~\r",
        TerminalCommandProcessor.formatForExecution("echo first\necho second"));
  }

  @Test
  void testFormatForExecution_multilineWithCrlf_normalizesLineEndings() {
    assertEquals("\u001b[200~echo first\recho second\u001b[201~\r",
        TerminalCommandProcessor.formatForExecution("echo first\r\necho second"));
  }

  @Test
  void testFormatForExecution_multilineWithTrailingNewline_doesNotSubmitEmptyCommand() {
    assertEquals("\u001b[200~echo first\recho second\u001b[201~\r",
        TerminalCommandProcessor.formatForExecution("echo first\necho second\n"));
  }

  @Test
  void testFormatForExecution_multilineWithoutBracketedPaste_submitsPlainLines() {
    assertEquals("echo first\recho second\r",
        TerminalCommandProcessor.formatForExecution("echo first\necho second", false));
  }

  @Test
  void testFormatForExecution_singleLineWithTrailingNewline_doesNotUseBracketedPaste() {
    assertEquals("echo hello\r", TerminalCommandProcessor.formatForExecution("echo hello\n"));
  }

  @Test
  void testFormatForExecution_backslashContinuation_doesNotUseBracketedPaste() {
    assertEquals("echo hello \\" + "\r  world\r",
        TerminalCommandProcessor.formatForExecution("echo hello \\\n  world"));
  }

  @Test
  void testTryCompleteWithMarker_completed_keepsNextPrompt() {
    StringBuilder output = new StringBuilder("echo hi\r\n")
        .append("hi\r\n")
        .append(ShellIntegrationScripts.COMMAND_FINISH_MARKER_PREFIX).append("0\u0007")
        .append(ShellIntegrationScripts.PROMPT_START_MARKER)
        .append("PS C:\\projects\\copilot-eclipse> ")
        .append(ShellIntegrationScripts.PROMPT_END_MARKER);

    var result = TerminalCommandProcessor.tryCompleteWithMarker(output);

    assertEquals(CompletionCheckState.COMPLETED, result.state());
    assertEquals("echo hi\nhi\nPS C:\\projects\\copilot-eclipse>", result.output());
  }

  @Test
  void testTryCompleteWithMarker_completedWithBareMarker() {
    StringBuilder output = new StringBuilder("echo hi\r\nhi\r\n]7775;C;0]7775;A$ ]7775;B");

    var result = TerminalCommandProcessor.tryCompleteWithMarker(output);

    assertEquals(CompletionCheckState.COMPLETED, result.state());
    assertEquals("echo hi\nhi\n$", result.output());
  }

  @Test
  void testTryCompleteWithMarker_incomplete_removesPromptMarkers() {
    StringBuilder output = new StringBuilder()
        .append(ShellIntegrationScripts.PROMPT_START_MARKER)
        .append("PS C:\\projects\\copilot-eclipse> ")
        .append(ShellIntegrationScripts.PROMPT_END_MARKER);

    var result = TerminalCommandProcessor.tryCompleteWithMarker(output);

    assertEquals(CompletionCheckState.INCOMPLETE, result.state());
    assertEquals("PS C:\\projects\\copilot-eclipse> ", output.toString());
  }

  @Test
  void testTryCompleteWithMarker_completedPreservesOutputContainingCommandText() {
    StringBuilder output = new StringBuilder("echo hi\r\n")
        .append("before\r\necho hi\r\nafter\r\n")
        .append(ShellIntegrationScripts.COMMAND_FINISH_MARKER_PREFIX).append("0\u0007")
        .append(ShellIntegrationScripts.PROMPT_START_MARKER)
        .append("$ ")
        .append(ShellIntegrationScripts.PROMPT_END_MARKER);

    var result = TerminalCommandProcessor.tryCompleteWithMarker(output);

    assertEquals(CompletionCheckState.COMPLETED, result.state());
    assertEquals("echo hi\nbefore\necho hi\nafter\n$", result.output());
  }

  @Test
  void testTryCompleteWithPrompt_completed_extractsOutputBetweenPrompts() {
    StringBuilder output = new StringBuilder("PS C:\\repo> echo hi\nhi\nPS C:\\repo> ");

    var result = TerminalCommandProcessor.tryCompleteWithPrompt(output);

    assertEquals(CompletionCheckState.COMPLETED, result.state());
    assertEquals("echo hi\nhi", result.output());
  }

  @Test
  void testTruncateOutput_shortOutput_returnsOriginalOutput() {
    String output = "line 1\r\nline 2";

    assertEquals(output, TerminalCommandProcessor.truncateOutput(output));
  }

  @Test
  void testTruncateOutput_longOutput_keepsTailLines() {
    StringBuilder output = new StringBuilder();
    for (int lineIndex = 1; lineIndex <= 1005; lineIndex++) {
      output.append("line ").append(lineIndex).append('\n');
    }

    String result = TerminalCommandProcessor.truncateOutput(output.toString());

    assertTrue(result.startsWith("[Terminal output truncated: showing last 1000 lines.]\n"));
    assertFalse(result.contains("line 5\n"));
    assertTrue(result.contains("line 6\n"));
    assertTrue(result.endsWith("line 1005\n"));
  }

  @Test
  void testPrepareOutputForModel_removesCopilotShellMarkers() {
    String output = "start\n" + ShellIntegrationScripts.PROMPT_START_MARKER
        + "]7775;B\nbody\n]7775;C\nliteral 7775;A remains\n"
        + ShellIntegrationScripts.COMMAND_FINISH_MARKER_PREFIX + "1\u0007end";

    String result = TerminalCommandProcessor.prepareOutputForModel(output);

    assertEquals("start\n\nbody\n\nliteral 7775;A remains\nend", result);
  }
}
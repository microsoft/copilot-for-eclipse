// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.terminal.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Processes terminal command input and output buffers.
 */
public final class TerminalCommandProcessor {
  private static final int MAX_OUTPUT_LINE_COUNT = 1000;
  private static final String BRACKETED_PASTE_START = "\u001b[200~";
  private static final String BRACKETED_PASTE_END = "\u001b[201~";
  private static final String ANSI_CSI_SEQUENCE_PATTERN = "\u001B\\[(\\?)?[\\d;]*[a-zA-Z]";
  private static final String OSC_SEQUENCE_PATTERN = "\u001B\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\)";
  private static final Pattern PROMPT_START_MARKER_PATTERN = buildMarkerPattern("A", false);
  private static final Pattern PROMPT_END_MARKER_PATTERN = buildMarkerPattern("B", false);
  private static final Pattern COMMAND_FINISH_MARKER_PATTERN = buildMarkerPattern("C", true);

  private TerminalCommandProcessor() {
    // Utility class
  }

  /**
   * Formats a command for immediate terminal execution.
   *
   * @param command the command to send
   * @return command text formatted for terminal input
   */
  public static String formatForExecution(String command) {
    return formatForExecution(command, true);
  }

  /**
   * Formats a command for immediate terminal execution.
   *
   * @param command the command to send
   * @param useBracketedPaste whether multiline commands should be sent using bracketed paste
   * @return command text formatted for terminal input
   */
  public static String formatForExecution(String command, boolean useBracketedPaste) {
    String normalizedCommand = StringUtils.stripEnd(normalizeLineEndings(command), "\n");
    String terminalInput = useBracketedPaste && isMultilineCommand(normalizedCommand)
        ? BRACKETED_PASTE_START + normalizedCommand + BRACKETED_PASTE_END
        : normalizedCommand;
    terminalInput = terminalInput.replace('\n', '\r');
    if (!terminalInput.endsWith("\r")) {
      terminalInput += "\r";
    }
    return terminalInput;
  }

  /**
   * Truncates terminal output to the tail when it is too long for a tool result.
   *
   * @param output terminal output
   * @return terminal output, truncated to the last lines when needed
   */
  public static String truncateOutput(String output) {
    if (output == null || output.isEmpty()) {
      return output == null ? "" : output;
    }

    String normalizedOutput = normalizeLineEndings(output);
    int scanIndex = normalizedOutput.endsWith("\n") ? normalizedOutput.length() - 2 : normalizedOutput.length() - 1;
    int keptLineCount = 1;
    while (scanIndex >= 0) {
      if (normalizedOutput.charAt(scanIndex) == '\n') {
        if (keptLineCount == MAX_OUTPUT_LINE_COUNT) {
          StringBuilder truncatedOutput = new StringBuilder();
          truncatedOutput.append("[Terminal output truncated: showing last ")
              .append(MAX_OUTPUT_LINE_COUNT)
              .append(" lines.]\n");
          truncatedOutput.append(normalizedOutput.substring(scanIndex + 1));
          return truncatedOutput.toString();
        }
        keptLineCount++;
      }
      scanIndex--;
    }

    return output;
  }

  /**
   * Cleans terminal output for model-visible tool results.
   *
   * @param output terminal output
   * @return output with terminal control sequences removed and long output truncated
   */
  public static String prepareOutputForModel(String output) {
    return truncateOutput(cleanTerminalControlSequences(output));
  }

  /**
   * Attempts to complete a command using shell integration markers.
   *
   * @param output terminal output buffer
   * @return the completion check result
   */
  public static CompletionCheckResult tryCompleteWithMarker(StringBuilder output) {
    MarkerRange commandFinishMarkerRange = findMarker(output, COMMAND_FINISH_MARKER_PATTERN, 0);
    if (commandFinishMarkerRange == null) {
      // Startup or idle prompts can arrive before a command runs. Keep the visible prompt text, but remove marker
      // bytes so later command output cleanup does not have to handle stale prompt boundaries.
      removePromptMarkers(output);
      return CompletionCheckResult.incomplete();
    }

    // A complete marker command is the command finish marker followed by the next prompt end. Waiting for B keeps the
    // prompt line in the returned output, which gives the language model the terminal's current working directory.
    MarkerRange promptEndMarkerRange = findMarker(output, PROMPT_END_MARKER_PATTERN, commandFinishMarkerRange.endIndex);
    if (promptEndMarkerRange == null) {
      return CompletionCheckResult.incomplete();
    }

    // Keep terminal output plus the next prompt, but exclude command finish markers.
    String completedOutput = output.substring(0, commandFinishMarkerRange.startIndex)
        + output.substring(commandFinishMarkerRange.endIndex, promptEndMarkerRange.endIndex);
    return CompletionCheckResult.completed(cleanCommandOutput(completedOutput));
  }

  /**
   * Attempts to complete a command by detecting a shell prompt.
   *
   * @param output terminal output buffer
   * @return the completion check result
   */
  public static CompletionCheckResult tryCompleteWithPrompt(StringBuilder output) {
    String terminalOutput = output.toString().trim();
    int lastNewLineIndex = terminalOutput.lastIndexOf('\n');
    if (lastNewLineIndex <= 0) {
      return CompletionCheckResult.incomplete();
    }

    String lastLine = terminalOutput.substring(lastNewLineIndex).trim();
    if (lastLine.isBlank() || lastLine.length() == 1) {
      return CompletionCheckResult.incomplete();
    }

    char lastChar = lastLine.charAt(lastLine.length() - 1);
    boolean isPromptChar = lastChar == '>' || lastChar == '#' || lastChar == '$' || lastChar == '%';
    if (!isPromptChar) {
      return CompletionCheckResult.incomplete();
    }

    String contentWithoutLastPrompt = terminalOutput.substring(0, lastNewLineIndex);
    int promptStartIndex = contentWithoutLastPrompt.indexOf(lastLine);
    if (promptStartIndex == -1) {
      promptStartIndex = 0;
    } else {
      promptStartIndex += lastLine.length();
    }

    if (contentWithoutLastPrompt.isBlank()) {
      return CompletionCheckResult.incomplete();
    }
    return CompletionCheckResult.completed(contentWithoutLastPrompt.substring(promptStartIndex).trim());
  }

  private static String removeBracketedPasteMarkers(String output) {
    return output.replace(BRACKETED_PASTE_START, "").replace(BRACKETED_PASTE_END, "");
  }

  private static boolean isMultilineCommand(String command) {
    int newlineIndex = command.indexOf('\n');
    while (newlineIndex >= 0) {
      if (newlineIndex == 0 || command.charAt(newlineIndex - 1) != '\\') {
        return true;
      }
      newlineIndex = command.indexOf('\n', newlineIndex + 1);
    }
    return false;
  }

  private static MarkerRange findMarker(StringBuilder output, Pattern markerPattern, int startIndex) {
    Matcher matcher = markerPattern.matcher(output);
    if (!matcher.find(startIndex)) {
      return null;
    }
    return new MarkerRange(matcher.start(), matcher.end());
  }

  private static void removePromptMarkers(StringBuilder output) {
    removeAll(output, PROMPT_START_MARKER_PATTERN);
    removeAll(output, PROMPT_END_MARKER_PATTERN);
  }

  private static void removeAll(StringBuilder output, Pattern markerPattern) {
    Matcher matcher = markerPattern.matcher(output);
    while (matcher.find()) {
      output.delete(matcher.start(), matcher.end());
      matcher.reset(output);
    }
  }

  private static Pattern buildMarkerPattern(String markerKind, boolean includeExitCode) {
    String exitCodePattern = includeExitCode ? "(?:;[-]?\\d+)?" : "";
    return Pattern.compile("(?:\u001B)?\\]" + ShellIntegrationScripts.OSC_NAMESPACE + ";" + markerKind
        + exitCodePattern + "(?:\u0007|\u001B\\\\)?");
  }

  private static String cleanCommandOutput(String output) {
    String normalizedOutput = normalizeLineEndings(output);
    normalizedOutput = removeShellIntegrationMarkers(normalizedOutput);
    normalizedOutput = removeBracketedPasteMarkers(normalizedOutput);
    return normalizedOutput.trim();
  }

  private static String removeShellIntegrationMarkers(String output) {
    return output.replaceAll(ShellIntegrationScripts.OSC_MARKER_PATTERN, "");
  }

  private static String cleanTerminalControlSequences(String output) {
    if (output == null || output.isEmpty()) {
      return output == null ? "" : output;
    }
    return removeShellIntegrationMarkers(output)
        .replaceAll(ANSI_CSI_SEQUENCE_PATTERN, "")
        .replaceAll(OSC_SEQUENCE_PATTERN, "");
  }

  private static String normalizeLineEndings(String value) {
    return value.replace("\r\n", "\n").replace('\r', '\n');
  }

  private record MarkerRange(int startIndex, int endIndex) {
  }

  /**
   * Result of checking a terminal output buffer for command completion.
   */
  public record CompletionCheckResult(CompletionCheckState state, String output) {
    private static CompletionCheckResult incomplete() {
      return new CompletionCheckResult(CompletionCheckState.INCOMPLETE, "");
    }

    private static CompletionCheckResult completed(String output) {
      return new CompletionCheckResult(CompletionCheckState.COMPLETED, output);
    }
  }

  /**
   * Terminal output completion states.
   */
  public enum CompletionCheckState {
    INCOMPLETE,
    COMPLETED
  }
}

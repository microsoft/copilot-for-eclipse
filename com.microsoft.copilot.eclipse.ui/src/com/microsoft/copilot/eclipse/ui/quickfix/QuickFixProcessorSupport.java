// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.quickfix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

final class QuickFixProcessorSupport {

  private QuickFixProcessorSupport() {
  }

  static boolean isCopilotAvailable() {
    CopilotCore plugin = CopilotCore.getPlugin();
    if (plugin == null) {
      return false;
    }

    AuthStatusManager authStatusManager = plugin.getAuthStatusManager();
    return authStatusManager != null && authStatusManager.isSignedIn();
  }

  static boolean hasOverlappingProblemMarker(IFile file, IDocument document, int offset, int length)
      throws CoreException {
    if (file == null || document == null || offset < 0 || offset > document.getLength()) {
      return false;
    }

    int selectionLength = Math.max(0, length);
    for (IMarker marker : file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)) {
      String message = marker.getAttribute(IMarker.MESSAGE, "").trim();
      if (!message.isEmpty() && overlaps(marker, document, offset, selectionLength)) {
        return true;
      }
    }
    return false;
  }

  static ProblemContext findProblemContext(IFile file, IDocument document, int offset, int length)
      throws CoreException {
    if (file == null || document == null || offset < 0 || offset > document.getLength()) {
      return ProblemContext.empty();
    }

    int selectionLength = Math.max(0, length);
    List<ProblemMarker> problems = new ArrayList<>();
    for (IMarker marker : file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)) {
      String message = marker.getAttribute(IMarker.MESSAGE, "").trim();
      if (message.isEmpty() || !overlaps(marker, document, offset, selectionLength)) {
        continue;
      }

      int start = sortOffset(marker, document);
      problems.add(new ProblemMarker(start, selectionEnd(marker, document, start), message));
    }

    problems.sort(Comparator.comparingInt(ProblemMarker::start).thenComparing(ProblemMarker::message));
    LinkedHashSet<String> uniqueMessages = new LinkedHashSet<>();
    for (ProblemMarker problem : problems) {
      uniqueMessages.add(problem.message());
    }
    if (uniqueMessages.isEmpty()) {
      return ProblemContext.empty();
    }

    int contextStart = offset;
    int contextEnd = (int) Math.min(document.getLength(), (long) offset + selectionLength);
    if (selectionLength == 0) {
      contextStart = problems.stream().mapToInt(ProblemMarker::start).min().orElse(offset);
      contextEnd = problems.stream().mapToInt(ProblemMarker::end).max().orElse(contextStart);
    }
    return new ProblemContext(List.copyOf(uniqueMessages), contextStart, Math.max(0, contextEnd - contextStart));
  }

  private static boolean overlaps(IMarker marker, IDocument document, int offset, int selectionLength)
      throws CoreException {
    int markerStart = marker.getAttribute(IMarker.CHAR_START, -1);
    int markerEnd = marker.getAttribute(IMarker.CHAR_END, -1);
    if (markerStart >= 0 && markerEnd >= markerStart) {
      if (selectionLength == 0) {
        return markerStart == markerEnd ? offset == markerStart : markerStart <= offset && offset < markerEnd;
      }

      long selectionEnd = (long) offset + selectionLength;
      if (markerStart == markerEnd) {
        return offset <= markerStart && markerStart < selectionEnd;
      }
      return markerStart < selectionEnd && offset < markerEnd;
    }

    int markerLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
    if (markerLine < 1) {
      return false;
    }

    try {
      int selectionStartLine = document.getLineOfOffset(offset) + 1;
      long requestedEndOffset = (long) offset + selectionLength - 1;
      int selectionEndOffset = selectionLength == 0 ? offset
          : (int) Math.min(document.getLength(), requestedEndOffset);
      int selectionEndLine = document.getLineOfOffset(selectionEndOffset) + 1;
      return selectionStartLine <= markerLine && markerLine <= selectionEndLine;
    } catch (BadLocationException e) {
      return false;
    }
  }

  private static int sortOffset(IMarker marker, IDocument document) throws CoreException {
    int markerStart = marker.getAttribute(IMarker.CHAR_START, -1);
    if (markerStart >= 0) {
      return markerStart;
    }

    int markerLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
    if (markerLine < 1) {
      return Integer.MAX_VALUE;
    }

    try {
      return document.getLineOffset(markerLine - 1);
    } catch (BadLocationException e) {
      return Integer.MAX_VALUE;
    }
  }

  private static int selectionEnd(IMarker marker, IDocument document, int start) throws CoreException {
    int markerStart = marker.getAttribute(IMarker.CHAR_START, -1);
    int markerEnd = marker.getAttribute(IMarker.CHAR_END, -1);
    if (markerStart >= 0 && markerEnd >= markerStart) {
      return Math.min(document.getLength(), markerEnd);
    }

    int markerLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
    if (markerLine < 1) {
      return start;
    }

    try {
      return start + document.getLineInformation(markerLine - 1).getLength();
    } catch (BadLocationException e) {
      return start;
    }
  }

  static String buildPrompt(List<String> messages) {
    StringBuilder prompt = new StringBuilder(Messages.quickFix_prompt).append(System.lineSeparator());
    for (String message : messages) {
      prompt.append(System.lineSeparator()).append("- ").append(message);
    }
    return prompt.toString();
  }

  static void openChat(String prompt) {
    UiUtils.executeCommandWithParameters(UiConstants.OPEN_CHAT_VIEW_COMMAND_ID, createOpenChatParameters(prompt));
  }

  static Map<String, Object> createOpenChatParameters(String prompt) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(UiConstants.OPEN_CHAT_VIEW_INPUT_VALUE, prompt);
    parameters.put(UiConstants.OPEN_CHAT_VIEW_AUTO_SEND, Boolean.FALSE.toString());
    return parameters;
  }

  record ProblemContext(List<String> messages, int selectionOffset, int selectionLength) {
    private static ProblemContext empty() {
      return new ProblemContext(List.of(), 0, 0);
    }
  }

  private record ProblemMarker(int start, int end, String message) {
  }
}

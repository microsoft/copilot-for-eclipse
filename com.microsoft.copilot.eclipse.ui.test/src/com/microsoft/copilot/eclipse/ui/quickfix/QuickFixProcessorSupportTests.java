// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.quickfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Point;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

class QuickFixProcessorSupportTests {
  private static final String CONTENT = "first line\nsecond problem\nthird problem\n";

  private IProject project;
  private IFile file;
  private IDocument document;

  @BeforeEach
  void setUp() throws Exception {
    project = ResourcesPlugin.getWorkspace().getRoot().getProject("copilot-quick-fix-tests");
    if (project.exists()) {
      project.delete(true, true, null);
    }
    project.create(null);
    project.open(null);

    file = project.getFile("problems.txt");
    file.create(new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)), true, null);
    document = new Document(CONTENT);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (project != null && project.exists()) {
      project.delete(true, true, null);
    }
  }

  @Test
  void findsProblemAtCaret() throws Exception {
    createMarker("Fix the second line", 11, 25, 2);

    assertTrue(QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, 18, 0));
    assertFalse(QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, 10, 0));
    assertFalse(QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, 25, 0));
    assertEquals(List.of("Fix the second line"),
        QuickFixProcessorSupport.findProblemContext(file, document, 18, 0).messages());
    assertTrue(QuickFixProcessorSupport.findProblemContext(file, document, 10, 0).messages().isEmpty());
    assertTrue(QuickFixProcessorSupport.findProblemContext(file, document, 25, 0).messages().isEmpty());
  }

  @Test
  void findsAllProblemsOverlappingSelectionInSourceOrder() throws Exception {
    createMarker("Third problem", 26, 39, 3);
    createMarker("Second problem", 11, 25, 2);
    createMarker("Second problem", 15, 20, 2);

    assertTrue(QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, 11, 28));
    assertEquals(List.of("Second problem", "Third problem"),
        QuickFixProcessorSupport.findProblemContext(file, document, 11, 28).messages());
  }

  @Test
  void combinesProblemsAtSameRangeAndSelectsAffectedRange() throws Exception {
    createMarker("Second problem", 11, 25, 2);
    createMarker("Another problem", 11, 25, 2);
    createMarker("Second problem", 11, 25, 2);

    QuickFixProcessorSupport.ProblemContext problemContext =
        QuickFixProcessorSupport.findProblemContext(file, document, 18, 0);

    assertEquals(List.of("Another problem", "Second problem"), problemContext.messages());
    assertEquals(11, problemContext.selectionOffset());
    assertEquals(14, problemContext.selectionLength());
  }

  @Test
  void supportsZeroLengthAndLineOnlyMarkers() throws Exception {
    createMarker("Insertion problem", 11, 11, 2);
    IMarker lineMarker = file.createMarker(IMarker.PROBLEM);
    lineMarker.setAttribute(IMarker.MESSAGE, "Line problem");
    lineMarker.setAttribute(IMarker.LINE_NUMBER, 3);

    assertTrue(QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, 11, 0));
    assertTrue(QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, 30, 0));
    assertEquals(List.of("Insertion problem"),
        QuickFixProcessorSupport.findProblemContext(file, document, 11, 0).messages());
    assertEquals(List.of("Line problem"),
        QuickFixProcessorSupport.findProblemContext(file, document, 30, 0).messages());
  }

  @Test
  void ordersLineOnlyAndCharacterRangeMarkersBySourcePosition() throws Exception {
    createMarker("Third problem", 26, 39, 3);
    IMarker lineMarker = file.createMarker(IMarker.PROBLEM);
    lineMarker.setAttribute(IMarker.MESSAGE, "Second problem");
    lineMarker.setAttribute(IMarker.LINE_NUMBER, 2);

    assertEquals(List.of("Second problem", "Third problem"),
        QuickFixProcessorSupport.findProblemContext(file, document, 11, 28).messages());
  }

  @Test
  void ignoresMarkersWithoutMessageAndInvalidInvocationOffsets() throws Exception {
    IMarker marker = file.createMarker(IMarker.PROBLEM);
    marker.setAttribute(IMarker.CHAR_START, 0);
    marker.setAttribute(IMarker.CHAR_END, 5);

    assertFalse(QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, 2, 0));
    assertFalse(QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, -1, 0));
    assertFalse(QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, document.getLength() + 1, 0));
    assertTrue(QuickFixProcessorSupport.findProblemContext(file, document, 2, 0).messages().isEmpty());
    assertTrue(QuickFixProcessorSupport.findProblemContext(file, document, -1, 0).messages().isEmpty());
    assertTrue(QuickFixProcessorSupport.findProblemContext(file, document, document.getLength() + 1, 0)
        .messages().isEmpty());
  }

  @Test
  void handlesInvalidAndVeryLargeSelectionLengths() throws Exception {
    createMarker("Second problem", 11, 25, 2);

    assertTrue(QuickFixProcessorSupport.findProblemContext(file, document, 10, -1).messages().isEmpty());
    assertEquals(List.of("Second problem"),
        QuickFixProcessorSupport.findProblemContext(file, document, 10, Integer.MAX_VALUE).messages());
  }

  @Test
  void buildsPromptAndProposalPrefillsIt() {
    String expectedPrompt = Messages.quickFix_prompt + System.lineSeparator() + System.lineSeparator()
        + "- First problem"
        + System.lineSeparator() + "- Second problem";
    String prompt = QuickFixProcessorSupport.buildPrompt(List.of("First problem", "Second problem"));
    assertEquals(expectedPrompt, prompt);

    AtomicReference<String> openedPrompt = new AtomicReference<>();
    CopilotQuickFixProposal proposal = new CopilotQuickFixProposal(prompt, 11, 14, openedPrompt::set);
    proposal.apply(document);

    assertEquals(expectedPrompt, openedPrompt.get());
    assertEquals(Messages.quickFix_fixWithCopilot, proposal.getDisplayString());
    assertEquals(new Point(11, 14), proposal.getSelection(document));

    Map<String, Object> parameters = QuickFixProcessorSupport.createOpenChatParameters(prompt);
    assertEquals(expectedPrompt, parameters.get(UiConstants.OPEN_CHAT_VIEW_INPUT_VALUE));
    assertEquals(Boolean.FALSE.toString(), parameters.get(UiConstants.OPEN_CHAT_VIEW_AUTO_SEND));
  }

  private void createMarker(String message, int start, int end, int line) throws Exception {
    IMarker marker = file.createMarker(IMarker.PROBLEM);
    marker.setAttribute(IMarker.MESSAGE, message);
    marker.setAttribute(IMarker.CHAR_START, start);
    marker.setAttribute(IMarker.CHAR_END, end);
    marker.setAttribute(IMarker.LINE_NUMBER, line);
  }
}
